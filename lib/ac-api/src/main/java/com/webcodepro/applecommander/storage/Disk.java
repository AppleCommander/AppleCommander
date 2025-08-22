/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2022 by Robert Greene
 * robgreene at users.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.webcodepro.applecommander.storage;

import com.webcodepro.applecommander.storage.physical.*;
import com.webcodepro.applecommander.util.TextBundle;
import org.applecommander.image.UniversalDiskImage;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;
import org.applecommander.util.DataBuffer;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * Abstract representation of an Apple2 disk (floppy, 800k, hard disk).
 * <p>
 * Date created: Oct 3, 2002 10:59:47 PM
 * @author Rob Greene
 */
public class Disk {
	/**
	 * Specifies a filter to be used in determining filetypes which are supported.
	 * This works from a file extension, so it may or may not apply to the Macintosh.
	 */
	public static class FilenameFilter {
		private String names;
		private String[] extensions;
		public FilenameFilter(String names, String... extensions) {
			this.names = names;
			this.extensions = extensions;
		}
		public String getExtensions() {
			return String.join(";", extensions);
		}
		public String[] getExtensionList() {
			return extensions;
		}
		public String getNames() {
			return names;
		}
	}

	public static final int BLOCK_SIZE = 512;
	public static final int SECTOR_SIZE = 256;
	public static final int PRODOS_BLOCKS_ON_140KB_DISK = 280;
	public static final int DOS33_SECTORS_ON_140KB_DISK = 560;
	public static final int APPLE_140KB_DISK = 143360;
	public static final int APPLE_140KB_NIBBLE_DISK = 232960;
    public static final int APPLE_400KB_DISK = 409600;
	public static final int APPLE_800KB_DISK = 819200;
	public static final int APPLE_800KB_2IMG_DISK =
		APPLE_800KB_DISK + UniversalDiskImage.HEADER_SIZE;
	public static final int APPLE_5MB_HARDDISK = 5242880;
	public static final int APPLE_10MB_HARDDISK = 10485760;
	public static final int APPLE_20MB_HARDDISK = 20971520;
	public static final int APPLE_32MB_HARDDISK = 33553920;	// short one block!

	private static final TextBundle textBundle = StorageBundle.getInstance();
	private static final FilenameFilter[] filenameFilters;
	private static final List<String> allFileExtensions;
	static {
		// Build everything dynamically
		List<String> templates = List.of(
				"140kDosImages:do,dsk",
				"140kProdosImages:po",
				"140kNibbleImages:nib",
				"800kProdosImages:2mg,2img",
				"ApplePcImages:hdv",
				"WozImages:woz",
				"DiskCopyImages:dc");
		List<FilenameFilter> filters = new ArrayList<>();
		List<String> allImages = new ArrayList<>(List.of("*.shk", "*.sdk"));
		List<String> compressedImages = new ArrayList<>(allImages);
		for (String template : templates) {
			String[] parts = template.split(":");
			String bundleName = String.format("Disk.%s", parts[0]);
			List<String> extensions = new ArrayList<>();
			for (String extension : parts[1].split(",")) {
				String ext1 = String.format("*.%s", extension);
				String ext2 = String.format("*.%s.gz", extension);
				extensions.add(ext1);
				extensions.add(ext2);
				compressedImages.add(ext2);
			}
			allImages.addAll(extensions);
			String text = textBundle.get(bundleName);
			filters.add(new FilenameFilter(text, extensions.toArray(new String[0])));
		}
		filters.addFirst(new FilenameFilter(textBundle.get("Disk.AllImages"), allImages.toArray(new String[0])));
		filters.add(new FilenameFilter(textBundle.get("Disk.CompressedImages"), compressedImages.toArray(new String[0])));
		filters.add(new FilenameFilter(textBundle.get("Disk.AllFiles"), "*.*"));
		filenameFilters = filters.toArray(new FilenameFilter[0]);
		// allFileExtensions is of the format ".dsk", ".dsk.gz", so we just strip the first character off...
		allFileExtensions = allImages.stream().map(s -> s.substring(1)).toList();
	}

	private String filename;
	private boolean newImage = false;
	private Source diskImageManager;
	private ImageOrder imageOrder = null;
	private FormattedDisk[] formattedDisks;

	/**
	 * Get the supported file filters supported by the Disk interface.
	 * This is due to the fact that FilenameFilter is an inner class of Disk -
	 * without an instance of the class, the filters cannot be created.
	 */
	public static FilenameFilter[] getFilenameFilters() {
		return filenameFilters;
	}

	/**
	 * Get the supported file extensions supported by the Disk interface.
	 * This is used by the Swing UI to populate the open file dialog box.
	 */
	public static List<String> getAllExtensions() {
		return allFileExtensions;
	}

	/**
	 * Construct a Disk with the given byte array.
	 */
	protected Disk(String filename, ImageOrder imageOrder) {
		this.imageOrder = imageOrder;
		this.filename = filename;
		this.newImage = true;
	}

	/**
	 * Construct a Disk and load the specified file.
	 * Read in the entire contents of the file.
	 */
	public Disk(String filename) throws IOException {
		this(filename, 0, false);
	}

	/**
	 * Construct a Disk and load the specified file.
	 * Read in the entire contents of the file.
	 */
	public Disk(String filename, boolean knownProDOSOrder) throws IOException {
		this(filename, 0, knownProDOSOrder);
	}

	/**
	 * Construct a Disk and load the specified file.
	 * Read in the entire contents of the file.
	 */
	public Disk(String filename, int startBlocks) throws IOException {
		this(filename, startBlocks, false);
	}

	/**
	 * Construct a Disk and load the specified file.
	 * Read in the entire contents of the file.
	 */
	public Disk(String filename, int startBlocks, boolean knownProDOSOrder) throws IOException {
		this(filename, Sources.create(Path.of(filename)).orElseThrow(), startBlocks, knownProDOSOrder);
	}

	public Disk(String filename, Source source, int startBlocks, boolean knownProDOSOrder) throws IOException {
		this.filename = filename;
		this.diskImageManager = source;

		List<FormattedDisk> foundDisks = Disks.inspect(source);
		if (!foundDisks.isEmpty()) {
			formattedDisks = foundDisks.toArray(new FormattedDisk[0]);
			imageOrder = foundDisks.getFirst().getImageOrder();
		}
		else {
			DiskFactory.Context ctx = new DiskFactory.Context(source);
			imageOrder = ctx.orders.getFirst();
		}
	}

	/**
	 * Save a Disk image to its file.
	 */
	public void save() throws IOException {
		File file = new File(getFilename());
		if (!file.exists()) {
			file.createNewFile();
		}
		OutputStream output = new FileOutputStream(file);
		if (isCompressed()) {
			output = new GZIPOutputStream(output);
		}
		DataBuffer data =getDiskImageManager().readAllBytes();
		byte[] fileData = new byte[data.limit()];
		data.read(fileData);
		output.write(fileData);
		output.close();
		getDiskImageManager().clearChanges();
		newImage = false;
	}

	/**
	 * Save a Disk image as a new/different file.
	 */
	public void saveAs(String filename) throws IOException {
		this.filename = filename;
		save();
	}

	/**
	 * Determine type of disk, and return the appropriate
	 * FormattedDisk object.  Throws an Exception if none is recognized.
	 * @throws DiskUnrecognizedException 
	 */
	public FormattedDisk[] getFormattedDisks() throws DiskUnrecognizedException {
		if (formattedDisks != null && formattedDisks.length > 0) {
			return formattedDisks;
		}
		throw new DiskUnrecognizedException(filename);
	}

    /**
     * Allows super-classes to pass in the specific FormattedDisk to support new discovery mechanism.
     * (Discovery occurs at class construction, not every time a formatted disk is pulled.)
     */
    protected void setFormattedDisks(FormattedDisk... formattedDisks) {
        assert(formattedDisks != null);
        this.formattedDisks = formattedDisks;
    }

	/**
	 * Returns the diskImageManager.
	 * @return Source diskImageManager The disk Image Manager of this disk
	 */
	public Source getDiskImageManager() {
		if (imageOrder != null) {
			return imageOrder.getDiskImageManager();
		}
		return diskImageManager;
	}

	/**
	 * Returns the filename.
	 * @return String
	 */
	public String getFilename() {
		return filename;
	}
	
	/**
	 * Sets the filename.
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	/**
	 * Returns the name of the underlying image order.
	 * @return String
	 */
	public String getOrderName() {
		return (imageOrder == null) ? textBundle.get("FormattedDisk.Unknown") : imageOrder.getName(); 
	}
	
	/**
	 * Indicate if this disk is GZIP compressed.
	 */
	public boolean isCompressed() {
		return filename.toLowerCase().endsWith(".gz"); //$NON-NLS-1$
	}
	
	/**
	 * Indicate if this disk is a ShrinkIt-compressed disk image.
	 */
	public boolean isSDK()
	{
		return filename.toLowerCase().endsWith(".sdk"); //$NON-NLS-1$
	}

	/**
	 * Indicate if this disk is ProDOS ordered (beginning with block 0).
	 */
	public boolean isProdosOrder() {
		return filename.toLowerCase().endsWith(".po") //$NON-NLS-1$
			|| filename.toLowerCase().endsWith(".po.gz") //$NON-NLS-1$
			|| is2ImgOrder()
			|| filename.toLowerCase().endsWith(".hdv") //$NON-NLS-1$
			|| getPhysicalSize() >= APPLE_800KB_2IMG_DISK;
	}
	
	/**
	 * Indicate if this disk is DOS ordered (T0,S0 - T35,S15).
	 */
	public boolean isDosOrder() {
		return filename.toLowerCase().endsWith(".do") //$NON-NLS-1$
			|| filename.toLowerCase().endsWith(".do.gz") //$NON-NLS-1$
			|| filename.toLowerCase().endsWith(".dsk") //$NON-NLS-1$
			|| filename.toLowerCase().endsWith(".dsk.gz"); //$NON-NLS-1$
	}
	
	/**
	 * Indicate if this disk is a 2IMG disk.
	 * This is ProDOS ordered, but with a header on the disk.
	 */
	public boolean is2ImgOrder() {
		return filename.toLowerCase().endsWith(".2img") //$NON-NLS-1$
			|| filename.toLowerCase().endsWith(".2img.gz") //$NON-NLS-1$
			|| filename.toLowerCase().endsWith(".2mg") //$NON-NLS-1$
		|| filename.toLowerCase().endsWith(".2mg.gz"); //$NON-NLS-1$
	}

	/**
	 * Indicate if this disk is a nibbilized disk..
	 */
	public boolean isNibbleOrder() {
		return filename.toLowerCase().endsWith(".nib") //$NON-NLS-1$
			|| filename.toLowerCase().endsWith(".nib.gz"); //$NON-NLS-1$
	}
	
	/**
	 * Identify the size of this disk.
	 */
	public int getPhysicalSize() {
		if (getImageOrder() instanceof WozOrder) {
			// Total hack since WOZ is currently a special case.
			return getImageOrder().getPhysicalSize();
		}
		if (getDiskImageManager() != null) {
			return getDiskImageManager().getSize();
		}
		return getImageOrder().getPhysicalSize();
	}
	
	/**
	 * Resize a disk image up to a larger size.  The primary intention is to
	 * "fix" disk images that have been created too small.  The primary culprit
	 * is ApplePC HDV images which dynamically grow.  Since AppleCommander
	 * works with a byte array, the image must grow to its full size.
	 * @param newSize
	 */
	protected void resizeDiskImage(int newSize) {
		if (newSize < getPhysicalSize()) {
			throw new IllegalArgumentException(
				textBundle.get("Disk.ResizeDiskError")); //$NON-NLS-1$
		}
		DataBuffer backingBuffer = imageOrder.getDiskImageManager().get(DataBuffer.class).orElseThrow();
		backingBuffer.limit(newSize);
	}
	
	/**
	 * Read the block from the disk image.
	 */
	public byte[] readBlock(int block) {
		return imageOrder.readBlock(block);
	}
	
	/**
	 * Write the block to the disk image.
	 */
	public void writeBlock(int block, byte[] data) {
		imageOrder.writeBlock(block, data);
	}

	/**
	 * Retrieve the specified sector.
	 */
	public byte[] readSector(int track, int sector) throws IllegalArgumentException {
		return imageOrder.readSector(track, sector);
	}
	
	/**
	 * Write the specified sector.
	 */
	public void writeSector(int track, int sector, byte[] bytes) 
			throws IllegalArgumentException {
		imageOrder.writeSector(track, sector, bytes);
	}

	/**
	 * Answers true if this disk image is within the expected 140K
	 * disk size.  Can vary if a header has been applied or if this is
	 * a nibblized disk image.
	 */
	protected boolean is140KbDisk() {
		return getPhysicalSize() >= APPLE_140KB_DISK
			&& getPhysicalSize() <= APPLE_140KB_NIBBLE_DISK;
	}

	/**
	 * Indicates if the disk has changed. Triggered when data is
	 * written and cleared when data is saved.
	 */
	public boolean hasChanged() {
		return getDiskImageManager().hasChanged();
	}
	
	/**
	 * Indicates if the disk image is new.  This can be used
	 * for Save As processing.
	 */
	public boolean isNewImage() {
		return newImage;
	}
	
	/**
	 * Answer with the physical ordering of the disk.
	 */
	public ImageOrder getImageOrder() {
		return imageOrder;
	}
	
	/**
	 * Set the physical ordering of the disk.
	 */
	protected void setImageOrder(ImageOrder imageOrder) {
		this.imageOrder = imageOrder;
	}

	/**
	 * Find the standard sized disk that will fit the requested number of bytes.
	 * @return int size of the disk if it will satisfy the request, -1 otherwise 
	 */
	public static int sizeToFit(long bytes) {
		if (bytes < APPLE_140KB_DISK) {
			return APPLE_140KB_DISK;
		} else if (bytes < APPLE_800KB_DISK) {
			return APPLE_800KB_DISK;
		} else if (bytes < APPLE_5MB_HARDDISK) {
			return APPLE_5MB_HARDDISK;
		} else if (bytes < APPLE_10MB_HARDDISK) {
			return APPLE_10MB_HARDDISK;
		} else if (bytes < APPLE_20MB_HARDDISK) {
			return APPLE_20MB_HARDDISK;
		} else if (bytes < APPLE_32MB_HARDDISK) {
			return APPLE_32MB_HARDDISK;
		}
		return -1;
	}
}
