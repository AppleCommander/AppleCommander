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

import com.webcodepro.applecommander.storage.os.cpm.CpmFileEntry;
import com.webcodepro.applecommander.storage.os.cpm.CpmFormatDisk;
import com.webcodepro.applecommander.storage.os.gutenberg.GutenbergFormatDisk;
import com.webcodepro.applecommander.storage.physical.*;
import com.webcodepro.applecommander.util.AppleUtil;
import com.webcodepro.applecommander.util.TextBundle;
import org.applecommander.hint.Hint;
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

		// Temporary shim to allow early testing of DiskFactory recognition.
		List<FormattedDisk> foundDisks = Disks.inspect(source);
		if (!foundDisks.isEmpty()) {
			formattedDisks = foundDisks.toArray(new FormattedDisk[0]);
		}

		int diskSize = source.getSize();

		knownProDOSOrder |= source.is(Hint.PRODOS_BLOCK_ORDER);

		ImageOrder dosOrder = new DosOrder(diskImageManager);
		ImageOrder proDosOrder = new ProdosOrder(diskImageManager);

		if (isSDK()) {
			imageOrder = proDosOrder; // SDKs are always in ProDOS order
		} else {
			/*
			 * First step: test physical disk orders for viable file systems.
			 */
			int rc = -1;
			if (diskSize == APPLE_140KB_DISK) {
				// First, test the really-really likely orders/formats for
				// 5-1/4" disks.
				imageOrder = dosOrder;
				if ((isProdosFormat() || isDosFormat()) && !knownProDOSOrder) {
					rc = 0;
				} else {
					imageOrder = proDosOrder;
					if (knownProDOSOrder || isProdosFormat() || isDosFormat() || isRdos33Format()) {
						rc = 0;
					}
				}
				if (rc == -1) {
					/*
				 	* Check filenames for something deterministic.
				 	*/
					if (isProdosOrder() || is2ImgOrder()) {
						imageOrder = proDosOrder;
						rc = 0;
					} else if (isDosOrder()) {
						imageOrder = dosOrder;
						rc = 0;
					} else if (isNibbleOrder()) {
						imageOrder = new NibbleOrder(diskImageManager);
						rc = 0;
					}
				}
				if (rc == -1) {
					/*
					 * Ok, it's not one of those. Now, let's go back to DOS
					 * order, and see if we recognize other things. If not,
					 * we'll fall through to other processing later.
					 */
					imageOrder = dosOrder;
					rc = testImageOrder();
				}
			}
			if (rc == -1) {
				imageOrder = proDosOrder;
				rc = testImageOrder();
				if (rc == -1) {
					/*
				 	* Couldn't find anything recognizable. Final step: 
				 	* just punt and start testing filenames.
				 	*/
					if (isProdosOrder() || is2ImgOrder()) {
						imageOrder = proDosOrder;
					} else if (isDosOrder()) {
						imageOrder = dosOrder;
					} else if (isNibbleOrder()) {
						imageOrder = new NibbleOrder(diskImageManager);
					} else {
						imageOrder = proDosOrder;
					}
				}
			}
		}
	}
	
	/**
	 * Test the image order to see if we can recognize a file system. Returns: 0
	 * on recognition; -1 on failure.
	 */
	public int testImageOrder()
	{
		int rc = (isProdosFormat() ? 1 : 0) + (isDosFormat() ? 2 : 0) + (isCpmFormat() ? 4 : 0) + (isUniDosFormat() ? 8 : 0) + (isPascalFormat() ? 16 : 0) + (isOzDosFormat() ? 32 : 0);
		if (rc == 0)
			rc = -1;
		return rc;
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
		// Old logic
        if (isCpmFormat()) {
			return new FormattedDisk[]
				{ new CpmFormatDisk(filename, imageOrder) };
		} else if (isWPFormat()) {
			return new FormattedDisk[]
				{ new GutenbergFormatDisk(filename, imageOrder) };
		}
		throw new DiskUnrecognizedException(filename);
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
	 * Test the disk format to see if this is a ProDOS formatted
	 * disk.
	 */
	public boolean isProdosFormat() {
		byte[] prodosVolumeDirectory = readBlock(2);
		int volDirEntryLength = prodosVolumeDirectory[0x23];
		int volDirEntriesPerBlock = prodosVolumeDirectory[0x24];

		return prodosVolumeDirectory[0] == 0 &&
			prodosVolumeDirectory[1] == 0 &&
			(prodosVolumeDirectory[4]&0xf0) == 0xf0 &&
			(volDirEntryLength * volDirEntriesPerBlock <= BLOCK_SIZE);
	}
	
	/**
	 * Test the disk format to see if this is a DOS 3.3 formatted
	 * disk.  This is a little nasty - since 800KB and 140KB images have
	 * different characteristics.  This just tests 140KB images.
	 */
	public boolean isDosFormat() {
		boolean good = false;
		if (!is140KbDisk()) {
			return false;
		}
		try {
			byte[] vtoc = readSector(17, 0);
			good = (imageOrder.isSizeApprox(APPLE_140KB_DISK)
					 || imageOrder.isSizeApprox(APPLE_140KB_NIBBLE_DISK))						 
						&& vtoc[0x01] == 17		// expect catalog to start on track 17
			// can vary	&& vtoc[0x02] == 15		// expect catalog to start on sector 15 (140KB disk only!)
						&& vtoc[0x27] == 122	// expect 122 track/sector pairs per sector
						&& (vtoc[0x34] == 35 || vtoc[0x34] == 40) // expect 35 or 40 tracks per disk (140KB disk only!)
						&& (vtoc[0x35] == 16 || vtoc[0x35] == 13) // expect 13 or 16 sectors per disk (140KB disk only!)
						;
			if (good) {
				int catTrack = vtoc[0x01]; // Pull out the first catalog track/sector
				int catSect = vtoc[0x02];
				byte[] cat = readSector(catTrack, catSect);
				if (catTrack == cat[1] && catSect == cat[2] + 1) {
					// Still good... let's follow one more
					catTrack = cat[1];
					catSect = cat[2];
					cat = readSector(catTrack, catSect);
					if (catTrack == cat[1] && catSect == cat[2] + 1) {
						good = true;
					} else {
						good = false;
					}
				}
			}
		} catch (Exception ex) {
			/*
			 *  If we get various exceptions from reading tracks and sectors, then we
			 *  definitely don't have a valid DOS image. 
			 */
			good = false;
		}
		return good;
	}

	/**
	 * Test the disk format to see if this is a UniDOS formatted
	 * disk.  UniDOS creates two logical disks on an 800KB physical disk.
	 * The first logical disk takes up the first 400KB and the second
	 * logical disk takes up the second 400KB.
	 */
	public boolean isUniDosFormat() {
		if (!is800KbDisk()) return false;
		byte[] vtoc1 = readSector(17, 0);	// logical disk #1
		byte[] vtoc2 = readSector(67, 0);	// logical disk #2
		return
			// LOGICAL DISK #1
			vtoc1[0x01] == 17		// expect catalog to start on track 17
			&& vtoc1[0x02] == 31	// expect catalog to start on sector 31
			&& vtoc1[0x27] == 122	// expect 122 tract/sector pairs per sector
			&& vtoc1[0x34] == 50	// expect 50 tracks per disk
			&& vtoc1[0x35] == 32	// expect 32 sectors per disk
			&& vtoc1[0x36] == 0		// bytes per sector (low byte)
			&& vtoc1[0x37] == 1		// bytes per sector (high byte)
			// LOGICAL DISK #2
			&& vtoc2[0x01] == 17	// expect catalog to start on track 17
			&& vtoc2[0x02] == 31	// expect catalog to start on sector 31
			&& vtoc2[0x27] == 122	// expect 122 tract/sector pairs per sector
			&& vtoc2[0x34] == 50	// expect 50 tracks per disk
			&& vtoc2[0x35] == 32	// expect 32 sectors per disk
			&& vtoc2[0x36] == 0		// bytes per sector (low byte)
			&& vtoc2[0x37] == 1;	// bytes per sector (high byte)
	}

	/**
	 * Test the disk format to see if this is a OzDOS formatted
	 * disk.  OzDOS creates two logical disks on an 800KB physical disk.
	 * The first logical disk takes the first half of each block and
	 * the second logical disk takes the second half of each block.
	 */
	public boolean isOzDosFormat() {
		if (!is800KbDisk()) return false;
		byte[] vtoc = readBlock(544);	// contains BOTH VTOCs!
		return
			// LOGICAL DISK #1
			vtoc[0x001] == 17		// expect catalog to start on track 17
			&& vtoc[0x002] == 31	// expect catalog to start on sector 31
			&& vtoc[0x027] == 122	// expect 122 tract/sector pairs per sector
			&& vtoc[0x034] == 50	// expect 50 tracks per disk
			&& vtoc[0x035] == 32	// expect 32 sectors per disk
			&& vtoc[0x036] == 0		// bytes per sector (low byte)
			&& vtoc[0x037] == 1		// bytes per sector (high byte)
			// LOGICAL DISK #2
			&& vtoc[0x137] == 1		// bytes per sector (high byte)
			&& vtoc[0x101] == 17	// expect catalog to start on track 17
			&& vtoc[0x102] == 31	// expect catalog to start on sector 31
			&& vtoc[0x127] == 122	// expect 122 tract/sector pairs per sector
			&& vtoc[0x134] == 50	// expect 50 tracks per disk
			&& vtoc[0x135] == 32	// expect 32 sectors per disk
			&& vtoc[0x136] == 0		// bytes per sector (low byte)
			&& vtoc[0x137] == 1;	// bytes per sector (high byte)
	}
	
	/**
	 * Test the disk format to see if this is a NakedOS formatted
	 * disk.  
	 */
	public boolean isNakedosFormat() {
		if (!is140KbDisk()) return false;
		byte[] vtoc = readSector(0, 3); // VTOC starts on sector 9 (mapped to 3)
		return (imageOrder.isSizeApprox(APPLE_140KB_DISK)
				 || imageOrder.isSizeApprox(APPLE_140KB_NIBBLE_DISK))						 
			&& vtoc[0xd0] == -2		// expect DOS as reserved
			&& vtoc[0xd1] == -2		// expect DOS as reserved
			&& vtoc[0xd2] == -2		// expect DOS as reserved
			&& vtoc[0xd3] == -2		// expect DOS as reserved
			&& vtoc[0xd4] == -2		// expect DOS as reserved
			&& vtoc[0xd5] == -2		// expect DOS as reserved
			&& vtoc[0xd6] == -2		// expect DOS as reserved
			&& vtoc[0xd7] == -2		// expect DOS as reserved
			&& vtoc[0xd8] == -2		// expect DOS as reserved
			&& vtoc[0xd9] == -2		// expect DOS as reserved
			&& vtoc[0xda] == -2		// expect DOS as reserved
			&& vtoc[0xdb] == -2		// expect DOS as reserved
			&& vtoc[0xdc] != -2		// expect something besides DOS next
			;
	}

	/**
	 * Test the disk format to see if this is a Pascal formatted
	 * disk. Pascal disks may be either 140K or 800K.
	 */
	public boolean isPascalFormat() {
		if (!(is140KbDisk() || is800KbDisk())) return false;
		byte[] directory = readBlock(2);
		return directory[0] == 0 && directory[1] == 0
			&& directory[2] == 6 && directory[3] == 0
			&& directory[4] == 0 && directory[5] == 0;
	}
	
	/**
	 * Test the disk format to see if this is a CP/M formatted disk.
	 * Check the first 256 bytes of the CP/M directory for validity.
	 */
	public boolean isCpmFormat() {
		if (!is140KbDisk()) return false;
		byte[] directory = readSector(3, 0);
		int bytes[] = new int[256];
		for (int i=0; i<directory.length; i++) {
			bytes[i] = AppleUtil.getUnsignedByte(directory[i]);
		}
		int offset = 0;
		while (offset < directory.length) {
			// Check if this is an empty directory entry (and ignore it)
			int e5count = 0;
			for (int i=0; i<CpmFileEntry.ENTRY_LENGTH; i++) {
				e5count+= bytes[offset+i] == 0xe5 ? 1 : 0;
			}
			if (e5count != CpmFileEntry.ENTRY_LENGTH) {	// Not all bytes were 0xE5
				// Check user number. Should be 0-15 or 0xE5
				if (bytes[offset] > 15 && bytes[offset] != 0xe5) return false;
				// Validate filename has highbit off
				for (int i=0; i<8; i++) {
					if (bytes[offset+1+i] > 127) return false; 
				}
				// Extent should be 0-31 (low = 0-31 and high = 0)
				if (bytes[offset+0xc] > 31 || bytes[offset+0xe] > 0) return false;
				// Number of used records cannot exceed 0x80
				if (bytes[offset+0xf] > 0x80) return false;
			}
			// Next entry
			offset+= CpmFileEntry.ENTRY_LENGTH;
		}
		return true;
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
	 * Answers true if this disk image is within the expected 800K
	 * disk size.  Can vary if a 2IMG header has been applied.
	 */
	protected boolean is800KbDisk() {
		return getPhysicalSize() >= APPLE_800KB_DISK
			&& getPhysicalSize() <= APPLE_800KB_2IMG_DISK;
	}
	
	/**
	 * Test the disk format to see if this is a RDOS formatted
	 * disk.
	 */
	public boolean isRdosFormat() {
		if (!is140KbDisk()) return false;
		if (getImageOrder().getSectorsPerTrack() != 16) return false;
		byte[] block = readSector(0, 0x0d);
		String id = AppleUtil.getString(block, 0xe0, 4);
		return "RDOS".equals(id) || isRdos33Format(); //$NON-NLS-1$
	}
	
	/**
	 * Test the disk format to see if this is a RDOS 33 formatted
	 * disk.
	 */
	public boolean isRdos33Format() {
		if (!is140KbDisk()) return false;
		byte[] block = readSector(1, 0x0);
		String id = AppleUtil.getString(block, 0x0, 6);
		return "RDOS 3".equals(id); //$NON-NLS-1$
	}

	/**
	 * Test the disk format to see if this is a WP formatted
	 * disk.
	 */
	public boolean isWPFormat() {
		if (!is140KbDisk()) return false;
		byte[] vtoc = readSector(17, 7);
		return (imageOrder.isSizeApprox(APPLE_140KB_DISK)
				 || imageOrder.isSizeApprox(APPLE_140KB_NIBBLE_DISK))						 
			&& vtoc[0x00] == 17		// expect catalog to start on track 17
			&& vtoc[0x01] == 7		// expect catalog to start on sector 7
			&& vtoc[0x0f] == -115;		// expect 0x8d's every 16 bytes
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
