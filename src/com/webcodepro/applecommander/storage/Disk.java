/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002 by Robert Greene
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.webcodepro.applecommander.storage.os.cpm.CpmFileEntry;
import com.webcodepro.applecommander.storage.os.cpm.CpmFormatDisk;
import com.webcodepro.applecommander.storage.os.dos33.DosFormatDisk;
import com.webcodepro.applecommander.storage.os.dos33.OzDosFormatDisk;
import com.webcodepro.applecommander.storage.os.dos33.UniDosFormatDisk;
import com.webcodepro.applecommander.storage.os.pascal.PascalFormatDisk;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;
import com.webcodepro.applecommander.storage.os.rdos.RdosFormatDisk;
import com.webcodepro.applecommander.storage.physical.ByteArrayImageLayout;
import com.webcodepro.applecommander.storage.physical.DosOrder;
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import com.webcodepro.applecommander.storage.physical.NibbleOrder;
import com.webcodepro.applecommander.storage.physical.ProdosOrder;
import com.webcodepro.applecommander.storage.physical.UniversalDiskImageLayout;
import com.webcodepro.applecommander.util.AppleUtil;
import com.webcodepro.applecommander.util.StreamUtil;
import com.webcodepro.applecommander.util.TextBundle;

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
	public class FilenameFilter {
		private String names;
		private String extensions;
		public FilenameFilter(String names, String extensions) {
			this.names = names;
			this.extensions = extensions;
		}
		public String getExtensions() {
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
	public static final int APPLE_800KB_DISK = 819200;
	public static final int APPLE_800KB_2IMG_DISK = APPLE_800KB_DISK + 0x40;
	public static final int APPLE_5MB_HARDDISK = 5242880;
	public static final int APPLE_10MB_HARDDISK = 10485760;
	public static final int APPLE_20MB_HARDDISK = 20971520;
	public static final int APPLE_32MB_HARDDISK = 33553920;	// short one block!

	private static FilenameFilter[] filenameFilters;
	private TextBundle textBundle = StorageBundle.getInstance();
	private String filename;
	private boolean newImage = false;
	private ByteArrayImageLayout diskImageManager;
	private ImageOrder imageOrder;
	
	/**
	 * Get the supported file filters supported by the Disk interface.
	 * This is due to the fact that FilenameFilter is an innerclass of Disk -
	 * without an instance of the class, the filters cannot be created.
	 */
	public static FilenameFilter[] getFilenameFilters() {
		if (filenameFilters == null) {
			new Disk();
		}
		return filenameFilters;
	}
	
	/**
	 * Constructor for a Disk - used only to generate FilenameFilter objects.
	 */
	private Disk() {
		filenameFilters = new FilenameFilter[] {
			new FilenameFilter(textBundle.get("Disk.AllImages"),  //$NON-NLS-1$
				"*.do; *.dsk; *.po; *.nib; *.2mg; *.2img; *.hdv; *.do.gz; *.dsk.gz; *.po.gz; *.nib.gz; *.2mg.gz; *.2img.gz"), //$NON-NLS-1$
			new FilenameFilter(textBundle.get("Disk.140kDosImages"),  //$NON-NLS-1$
				"*.do; *.dsk; *.do.gz; *.dsk.gz"), //$NON-NLS-1$
			new FilenameFilter(textBundle.get("Disk.140kNibbleImages"), //$NON-NLS-1$
				"*.nib; *.nib.gz"), //$NON-NLS-1$
			new FilenameFilter(textBundle.get("Disk.140kProdosImages"),  //$NON-NLS-1$
				"*.po; *.po.gz"), //$NON-NLS-1$
			new FilenameFilter(textBundle.get("Disk.800kProdosImages"),  //$NON-NLS-1$
				"*.2mg; *.2img; *.2mg.gz, *.2img.gz"), //$NON-NLS-1$
			new FilenameFilter(textBundle.get("Disk.ApplePcImages"),  //$NON-NLS-1$
				"*.hdv"), //$NON-NLS-1$
			new FilenameFilter(textBundle.get("Disk.CompressedImages"),  //$NON-NLS-1$
				"*.do.gz; *.dsk.gz; *.po.gz; *.2mg.gz; *.2img.gz"), //$NON-NLS-1$
			new FilenameFilter(textBundle.get("Disk.AllFiles"),  //$NON-NLS-1$
				"*.*") //$NON-NLS-1$
		};
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
		this.filename = filename;
		File file = new File(filename);
		InputStream input = new FileInputStream(file);
		if (isCompressed()) {
			input = new GZIPInputStream(input);
		}
		int diskSize = (int) file.length();
		ByteArrayOutputStream diskImageByteArray = 
			new ByteArrayOutputStream(diskSize);
		StreamUtil.copy(input, diskImageByteArray);
		byte[] diskImage = diskImageByteArray.toByteArray();
		if ((diskImage[00] == '2') && (diskImage[01] == 'I') &&
			(diskImage[02] == 'M') && (diskImage[03] == 'G')) {
			diskImageManager = new UniversalDiskImageLayout(diskImage);
		} else {
			diskImageManager = new ByteArrayImageLayout(diskImage);
		}
		if (isProdosOrder()) {
			imageOrder = new ProdosOrder(diskImageManager);
		} else if (isDosOrder()) {
			imageOrder = new DosOrder(diskImageManager);
		} else if (isNibbleOrder()) {
			imageOrder = new NibbleOrder(diskImageManager);
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
		output.write(getDiskImageManager().getDiskImage());
		output.close();
		getDiskImageManager().setChanged(false);
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
	 * FormattedDisk object.  Returns null if none are
	 * recognized.
	 */
	public FormattedDisk[] getFormattedDisks() {
		if (isProdosFormat()) {
			return new FormattedDisk[]
				{ new ProdosFormatDisk(filename, imageOrder) };
		} else if (isUniDosFormat()) {
			return new FormattedDisk[] {
				new UniDosFormatDisk(filename, imageOrder, 
									UniDosFormatDisk.UNIDOS_DISK_1),
				new UniDosFormatDisk(filename, imageOrder, 
									UniDosFormatDisk.UNIDOS_DISK_2) };
		} else if (isOzDosFormat()) {
			return new FormattedDisk[] {
				new OzDosFormatDisk(filename, imageOrder,
									OzDosFormatDisk.OZDOS_DISK_1),
				new OzDosFormatDisk(filename, imageOrder,
									OzDosFormatDisk.OZDOS_DISK_2) };
		} else if (isDosFormat()) {
			return new FormattedDisk[]
				{ new DosFormatDisk(filename, imageOrder) };
		} else if (isPascalFormat()) {
			return new FormattedDisk[]
				{ new PascalFormatDisk(filename, imageOrder) };
		} else if (isRdosFormat()) {
			return new FormattedDisk[]
				{ new RdosFormatDisk(filename, imageOrder) };
		} else if (isCpmFormat()) {
			return new FormattedDisk[]
				{ new CpmFormatDisk(filename, imageOrder) };
		}
		return null;
	}
	
	/**
	 * Returns the diskImage.
	 * @return byte[]
	 */
	public ByteArrayImageLayout getDiskImageManager() {
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
	 * Indicate if this disk is GZIP compressed.
	 */
	public boolean isCompressed() {
		return filename.toLowerCase().endsWith(".gz"); //$NON-NLS-1$
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
		if (getDiskImageManager() != null) {
			return getDiskImageManager().getPhysicalSize();
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
		byte[] newDiskImage = new byte[newSize];
		byte[] oldDiskImage = imageOrder.getDiskImageManager().getDiskImage();
		System.arraycopy(oldDiskImage, 0, newDiskImage, 0, oldDiskImage.length);
		imageOrder.getDiskImageManager().setDiskImage(newDiskImage);
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
		return prodosVolumeDirectory[0] == 0 &&
			prodosVolumeDirectory[1] == 0 &&
			(prodosVolumeDirectory[4]&0xf0) == 0xf0;
	}
	
	/**
	 * Test the disk format to see if this is a DOS 3.3 formatted
	 * disk.  This is a little nasty - since 800KB and 140KB images have
	 * different characteristics.  This just tests 140KB images.
	 */
	public boolean isDosFormat() {
		if (!is140KbDisk()) return false;
		byte[] vtoc = readSector(17, 0);
		return (imageOrder.isSizeApprox(APPLE_140KB_DISK)
				 || imageOrder.isSizeApprox(APPLE_140KB_NIBBLE_DISK))						 
			&& vtoc[0x01] == 17		// expect catalog to start on track 17
// can vary	&& vtoc[0x02] == 15		// expect catalog to start on sector 15 (140KB disk only!)
			&& vtoc[0x27] == 122	// expect 122 tract/sector pairs per sector
			&& vtoc[0x34] == 35		// expect 35 tracks per disk (140KB disk only!)
			&& vtoc[0x35] == 16;		// expect 16 sectors per disk (140KB disk only!)
//			&& vtoc[0x36] == 0		// bytes per sector (low byte)
//			&& vtoc[0x37] == 1;		// bytes per sector (high byte)
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
		byte[] block = readSector(0, 0x0d);
		String id = AppleUtil.getString(block, 0xe0, 4);
		return "RDOS".equals(id); //$NON-NLS-1$
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
	 * Answer with the phyiscal ordering of the disk.
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
}
