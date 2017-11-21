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
import com.webcodepro.applecommander.storage.os.nakedos.NakedosFormatDisk;
import com.webcodepro.applecommander.storage.os.gutenberg.GutenbergFormatDisk;
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
	public static final int APPLE_800KB_DISK = 819200;
	public static final int APPLE_800KB_2IMG_DISK =
		APPLE_800KB_DISK + UniversalDiskImageLayout.OFFSET;
	public static final int APPLE_5MB_HARDDISK = 5242880;
	public static final int APPLE_10MB_HARDDISK = 10485760;
	public static final int APPLE_20MB_HARDDISK = 20971520;
	public static final int APPLE_32MB_HARDDISK = 33553920;	// short one block!

	private static FilenameFilter[] filenameFilters;
	private static String[] allFileExtensions = null;
	private TextBundle textBundle = StorageBundle.getInstance();
	private String filename;
	private boolean newImage = false;
	private boolean isDC42 = false;
	private ByteArrayImageLayout diskImageManager;
	private ImageOrder imageOrder = null;

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
	 * Get the supported file extensions supported by the Disk interface.
	 * This is used by the Swing UI to populate the open file dialog box.
	 */
	public static String[] getAllExtensions() {
		if (allFileExtensions == null) {
			new Disk();
		}
		return allFileExtensions;
	}

	/**
	 * Constructor for a Disk - used only to generate FilenameFilter objects.
	 */
	private Disk() {
		filenameFilters = new FilenameFilter[] {
			new FilenameFilter(textBundle.get("Disk.AllImages"),  //$NON-NLS-1$
				"*.do", "*.dsk", "*.po", "*.nib", "*.2mg", "*.2img", "*.hdv", "*.do.gz", "*.dsk.gz", "*.po.gz", "*.nib.gz", "*.2mg.gz", "*.2img.gz"), //$NON-NLS-1$
			new FilenameFilter(textBundle.get("Disk.140kDosImages"),  //$NON-NLS-1$
				"*.do", "*.dsk", "*.do.gz", "*.dsk.gz"), //$NON-NLS-1$
			new FilenameFilter(textBundle.get("Disk.140kNibbleImages"), //$NON-NLS-1$
				"*.nib", "*.nib.gz"), //$NON-NLS-1$
			new FilenameFilter(textBundle.get("Disk.140kProdosImages"),  //$NON-NLS-1$
				"*.po", "*.po.gz"), //$NON-NLS-1$
			new FilenameFilter(textBundle.get("Disk.800kProdosImages"),  //$NON-NLS-1$
				"*.2mg", "*.2img", "*.2mg.gz", "*.2img.gz"), //$NON-NLS-1$
			new FilenameFilter(textBundle.get("Disk.ApplePcImages"),  //$NON-NLS-1$
				"*.hdv"), //$NON-NLS-1$
			new FilenameFilter(textBundle.get("Disk.CompressedImages"),  //$NON-NLS-1$
				"*.sdk", "*.shk", "*.do.gz", "*.dsk.gz", "*.po.gz", "*.2mg.gz", "*.2img.gz"), //$NON-NLS-1$
			new FilenameFilter(textBundle.get("Disk.AllFiles"),  //$NON-NLS-1$
				"*.*") //$NON-NLS-1$
		};
		allFileExtensions = new String[] {
			".do",		//$NON-NLS-1$
			".dsk",		//$NON-NLS-1$
			".po",		//$NON-NLS-1$
			".nib",		//$NON-NLS-1$
			".sdk",		//$NON-NLS-1$
			".shk",		//$NON-NLS-1$
			".2mg",		//$NON-NLS-1$
			".2img",	//$NON-NLS-1$
			".hdv",		//$NON-NLS-1$
			".do.gz",	//$NON-NLS-1$
			".dsk.gz",	//$NON-NLS-1$
			".po.gz",	//$NON-NLS-1$
			".nib.gz",	//$NON-NLS-1$
			".2mg.gz",	//$NON-NLS-1$
			".2img.gz"	//$NON-NLS-1$
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
		this.filename = filename;
		int diskSize = 0;
		byte[] diskImage = null;
		byte[] diskImageDC42 = null;

		if (isSDK() || isSHK() || isBXY()) {
			// If we have an SDK, unpack it and send along the byte array
			// If we have a SHK, build a new disk and unpack the contents on to it
			diskImage = com.webcodepro.shrinkit.Utilities.unpackSHKFile(filename, startBlocks);
			diskSize = diskImage.length;
			// Since we don't want to overwrite their shrinkit with a raw ProDOS image,
			// add a .po extension to it
			this.filename += ".po"; //$NON-NLS-1$
		} else {
			File file = new File(filename);
			diskSize = (int) file.length();
			InputStream input = new FileInputStream(file);
			if (isCompressed()) {
				input = new GZIPInputStream(input);
			}
			ByteArrayOutputStream diskImageByteArray = new ByteArrayOutputStream(diskSize);
			StreamUtil.copy(input, diskImageByteArray);
			diskImage = diskImageByteArray.toByteArray();
		}
		boolean is2img = false;
		/* Does it have the 2IMG header? */
		if ((diskImage[0] == 0x32) && (diskImage[1] == 0x49) && (diskImage[2] == 0x4D) && (diskImage[3]) == 0x47) {
			is2img = true;
		}
		/* Does it have the DiskCopy 4.2 header? */
		else if (Disk.isDC42(diskImage)) {
			isDC42 = true;
			long end = AppleUtil.getLongValue(diskImage,0x40);
			if (end < diskImage.length - 83) {
				diskImageDC42 = new byte[(int)end];
				System.arraycopy(diskImage, 84, diskImageDC42, 0, (int)end); // 84 bytes into the DC42 stream is where the real data starts
				diskImageManager = new ByteArrayImageLayout(diskImageDC42);
				// Since we don't want to overwrite their dmg or dc42 with a raw ProDOS image,
				// add a .po extension to it
				this.filename += ".po"; //$NON-NLS-1$
			}
			else
				throw new IllegalArgumentException(textBundle.get("CommandLineDC42Bad")); //$NON-NLS-1$
		}
		if (is2img == true || diskImage.length == APPLE_800KB_DISK + UniversalDiskImageLayout.OFFSET 
				|| diskImage.length == APPLE_5MB_HARDDISK + UniversalDiskImageLayout.OFFSET 
				|| diskImage.length == APPLE_10MB_HARDDISK + UniversalDiskImageLayout.OFFSET 
				|| diskImage.length == APPLE_20MB_HARDDISK + UniversalDiskImageLayout.OFFSET 
				|| diskImage.length == APPLE_32MB_HARDDISK + UniversalDiskImageLayout.OFFSET) {
			diskImageManager = new UniversalDiskImageLayout(diskImage);
		} else if (isDC42) {
			diskImageManager = new ByteArrayImageLayout(diskImageDC42);
		} else {
			diskImageManager = new ByteArrayImageLayout(diskImage);
		}

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
					if (knownProDOSOrder || isProdosFormat() || isDosFormat()) {
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
		int rc = (true == isProdosFormat() ? 1 : 0) + (true == isDosFormat() ? 2 : 0) + (true == isCpmFormat() ? 4 : 0) + (true == isUniDosFormat() ? 8 : 0) + (true == isPascalFormat() ? 16 : 0) + (true == isOzDosFormat() ? 32 : 0);
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
		} else if (isNakedosFormat()) {
			return new FormattedDisk[]
				{ new NakedosFormatDisk(filename, imageOrder) };
		} else if (isPascalFormat()) {
			return new FormattedDisk[]
				{ new PascalFormatDisk(filename, imageOrder) };
		} else if (isRdosFormat()) {
			return new FormattedDisk[]
				{ new RdosFormatDisk(filename, imageOrder) };
		} else if (isCpmFormat()) {
			return new FormattedDisk[]
				{ new CpmFormatDisk(filename, imageOrder) };
		} else if (isWPFormat()) {
			return new FormattedDisk[]
				{ new GutenbergFormatDisk(filename, imageOrder) };
		}
		return null;
	}

	/**
	 * Returns the diskImageManager.
	 * @return ByteArrayImageLayout diskImageManager The disk Image Manager of this disk
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
	 * Indicate if this disk is a ShrinkIt-compressed package.
	 */
	public boolean isSHK()
	{
		return filename.toLowerCase().endsWith(".shk"); //$NON-NLS-1$
	}

	/**
	 * Indicate if this disk is a ShrinkIt-compressed binary II archive.
	 */
	public boolean isBXY()
	{
		return filename.toLowerCase().endsWith(".bxy"); //$NON-NLS-1$
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
						&& vtoc[0x35] == 16		// expect 16 sectors per disk (140KB disk only!)
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
		byte[] block = readSector(0, 0x0d);
		String id = AppleUtil.getString(block, 0xe0, 4);
		return "RDOS".equals(id); //$NON-NLS-1$
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
	 * Indicates if a given byte sequence is likely to be a DiskCopy 42 stream.
	 * 
	 * @return boolean liklihood it is a DC42 stream
	 */
	private static boolean isDC42(byte[] buffer) {
		boolean truth = ((buffer[0x52] == 0x01) && (buffer[0x53] == 0x00)) &&
		((buffer[0x51] == 0x02) || (buffer[0x51] == 0x22) || (buffer[0x51] == 0x24));
		return (((buffer[0x52] == 0x01) && (buffer[0x53] == 0x00)) &&
				((buffer[0x51] == 0x02) || (buffer[0x51] == 0x22) || (buffer[0x51] == 0x24)));
	}
	public boolean isDC42() {
		return isDC42;
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
	 * Change underlying image order to DOS ImageOrder.
	 * Assumes this is a 140k disk image.
	 * 
	 * @see ImageOrder
	 */
	public void makeDosOrder()
	{
		DosOrder doso = new DosOrder(new ByteArrayImageLayout(Disk.APPLE_140KB_DISK));
		changeImageOrderByTrackAndSector(getImageOrder(), doso);
		setImageOrder(doso);
	}

	/**
	 * Change to a different ImageOrder. Remains in ProDOS format but the
	 * underlying order can change.
	 * 
	 * @see ImageOrder
	 */
	public void makeProdosOrder()
	{
		ProdosOrder pdo = new ProdosOrder(new ByteArrayImageLayout(Disk.APPLE_140KB_DISK));
		changeImageOrderByBlock(getImageOrder(), pdo);
		setImageOrder(pdo);
	}

	/**
	 * Find the standard sized disk that will fit the requested number of bytes.
	 * @returns int size of the disk if it will satisfy the request, -1 otherwise 
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
			return APPLE_20MB_HARDDISK;
		} else if (bytes < APPLE_32MB_HARDDISK) {
			return APPLE_32MB_HARDDISK;
		}
		return -1;
	}

	/**
	 * Change ImageOrder from source order to target order by copying sector by
	 * sector.
	 */
	private void changeImageOrderByTrackAndSector(ImageOrder sourceOrder, ImageOrder targetOrder)
	{
		if (!sameSectorsPerDisk(sourceOrder, targetOrder)) {
			throw new IllegalArgumentException(textBundle.get("Disk.ResizeDiskError"));
		}
		for (int track = 0; track < sourceOrder.getTracksPerDisk(); track++) {
			for (int sector = 0; sector < sourceOrder.getSectorsPerTrack(); sector++) {
				byte[] data = sourceOrder.readSector(track, sector);
				targetOrder.writeSector(track, sector, data);
			}
		}
	}

	/**
	 * Change ImageOrder from source order to target order by copying block by
	 * block.
	 */
	private void changeImageOrderByBlock(ImageOrder sourceOrder, ImageOrder targetOrder)
	{
		if (!sameBlocksPerDisk(sourceOrder, targetOrder)) {
			throw new IllegalArgumentException(textBundle.get("Disk.ResizeDiskError"));
		}
		for (int block = 0; block < sourceOrder.getBlocksOnDevice(); block++) {
			byte[] blockData = sourceOrder.readBlock(block);
			targetOrder.writeBlock(block, blockData);
		}
	}

	/**
	 * Answers true if the two disks have the same number of blocks per disk.
	 */
	private static boolean sameBlocksPerDisk(ImageOrder sourceOrder, ImageOrder targetOrder)
	{
		return sourceOrder.getBlocksOnDevice() == targetOrder.getBlocksOnDevice();
	}

	/**
	 * Answers true if the two disks have the same number of sectors per disk.
	 */
	private static boolean sameSectorsPerDisk(ImageOrder sourceOrder, ImageOrder targetOrder)
	{
		return sourceOrder.getSectorsPerDisk() == targetOrder.getSectorsPerDisk();
	}

}
