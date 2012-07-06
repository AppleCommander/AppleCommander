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

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Abstract representation of a formatted Apple2 disk (floppy, 800k, hard disk).
 * <p>
 * Date created: Oct 5, 2002 3:51:44 PM
 * @author Rob Greene
 */
public abstract class FormattedDisk extends Disk implements DirectoryEntry {
	private TextBundle textBundle = StorageBundle.getInstance();
	/**
	 * Use this inner class for label/value mappings in the disk info page.
	 */
	public class DiskInformation {
		private String label;
		private String value;
		public DiskInformation(String label, String value) {
			this.label = label;
			this.value = value;
		}
		public DiskInformation(String label, int value) {
			this.label = label;
			this.value = Integer.toString(value);
		}
		public DiskInformation(String label, Date value) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(
					StorageBundle.getInstance().get("DateFormat")); //$NON-NLS-1$
			this.label = label;
			if (value != null) {
				this.value = dateFormat.format(value);
			} else {
				this.value = StorageBundle.getInstance()
					.get("FormattedDisk.NullDate"); //$NON-NLS-1$
			}
		}
		public String getLabel() {
			return this.label;
		}
		public String getValue() {
			return this.value;
		}
	}
	
	/**
	 * Use this inner interface for managing the disk usage data.
	 * This offloads format-specific implementation to the implementing class.
	 * The usage is very similar to a Java2 Iterator - next must be called to
	 * set the value and isFree/isUsed are available for that location.
	 */
	public interface DiskUsage {
		public boolean hasNext();
		public void next();
		public boolean isFree();
		public boolean isUsed();
	}
	
	/**
	 * This inner class represents the column header information used
	 * in the directory display.  Note that this needs to be synchronized
	 * with the appropriate FileEntry objects.
	 */
	public static final int FILE_DISPLAY_STANDARD = 1;
	public static final int FILE_DISPLAY_NATIVE = 2;
	public static final int FILE_DISPLAY_DETAIL = 3;
	public class FileColumnHeader {
		public static final int ALIGN_LEFT = 1;
		public static final int ALIGN_CENTER = 2;
		public static final int ALIGN_RIGHT = 3;
		private String title;
		private int maximumWidth;
		private int alignment;
		public FileColumnHeader(String title, int maximumWidth, int alignment) {
			this.title = title;
			this.maximumWidth = maximumWidth;
			this.alignment = alignment;
		}
		public String getTitle() {
			return title;
		}
		public int getMaximumWidth() {
			return maximumWidth;
		}
		public int getAlignment() {
			return alignment;
		}
		public boolean isLeftAlign() {
			return alignment == ALIGN_LEFT;
		}
		public boolean isCenterAlign() {
			return alignment == ALIGN_CENTER;
		}
		public boolean isRightAlign() {
			return alignment == ALIGN_RIGHT;
		}
	}
	
	/**
	 * Constructor for FormattedDisk.
	 */
	public FormattedDisk(String filename, ImageOrder imageOrder) {
		super(filename, imageOrder);
	}

	/**
	 * Return the name of the disk.  Not the physical file name,
	 * but "DISK VOLUME #xxx" (DOS 3.3) or "/MY.DISK" (ProDOS).
	 */
	public abstract String getDiskName();
	
	/**
	 * Set the name of the disk to volumeName.
	 */
	public void setDiskName(String volumeName) {
	}

	/**
	 * Identify the operating system format of this disk.
	 */
	public abstract String getFormat();

	/**
	 * Return the amount of free space in bytes.
	 */
	public abstract int getFreeSpace();

	/**
	 * Return the amount of used space in bytes.
	 */
	public abstract int getUsedSpace();

	/**
	 * Get suggested dimensions for display of bitmap.
	 * Typically, this will be only used for 5.25" floppies.
	 * This can return null if there is no suggestion.
	 */
	public abstract int[] getBitmapDimensions();
	
	/**
	 * Get the length of the bitmap.
	 */
	public abstract int getBitmapLength();
	
	/**
	 * Get the disk usage iterator.
	 */
	public abstract DiskUsage getDiskUsage();
	
	/**
	 * Get the labels to use in the bitmap.
	 * Note that this should, at a minimum, return an array of
	 * String[1] unless the bitmap has not been implemented.
	 */
	public abstract String[] getBitmapLabels();
	
	/**
	 * Get disk information.  This is intended to be pretty generic -
	 * each disk format can build this as appropriate.  Each subclass should
	 * override this method and add its own detail.
	 */
	public List getDiskInformation() {
		List list = new ArrayList();
		list.add(new DiskInformation(textBundle.get("FormattedDisk.FileName"), getFilename())); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("FormattedDisk.DiskName"), getDiskName())); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("FormattedDisk.PhysicalSizeInBytes"), getPhysicalSize())); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("FormattedDisk.FreeSpaceInBytes"), getFreeSpace())); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("FormattedDisk.UsedSpaceInBytes"), getUsedSpace())); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("FormattedDisk.PhysicalSizeInKb"), getPhysicalSize() / 1024)); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("FormattedDisk.FreeSpaceInKb"), getFreeSpace() / 1024)); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("FormattedDisk.UsedSpaceInKb"), getUsedSpace() / 1024)); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("FormattedDisk.ArchiveOrder"),  getOrderName())); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("FormattedDisk.DiskFormat"), getFormat())); //$NON-NLS-1$
		return list;
	}
	
	/**
	 * Get the standard file column header information.
	 * This default implementation is intended only for standard mode.
	 */
	public List getFileColumnHeaders(int displayMode) {
		List list = new ArrayList();
		list.add(new FileColumnHeader(textBundle
				.get("Name"), 30, FileColumnHeader.ALIGN_LEFT)); //$NON-NLS-1$
		list.add(new FileColumnHeader(textBundle
				.get("Type"), 8, FileColumnHeader.ALIGN_CENTER)); //$NON-NLS-1$
		list.add(new FileColumnHeader(textBundle
				.get("SizeInBytes"), 6, FileColumnHeader.ALIGN_RIGHT)); //$NON-NLS-1$
		list.add(new FileColumnHeader(textBundle
				.get("LockedQ"), 6, FileColumnHeader.ALIGN_CENTER)); //$NON-NLS-1$
		return list;
	}
	
	/**
	 * Indicates if this disk format supports "deleted" files.
	 * Not to be confused with being able to delete a file, this indicates that
	 * deleted entries remain in the filesystem after the file has been deleted.
	 * There are some filesystems that "compress" the file out of the structure
	 * by completely removing the entry instead of marking it deleted (like 
	 * Apple Pascal).
	 */
	public abstract boolean supportsDeletedFiles();
	
	/**
	 * Indicates if this disk image can read data from a file.
	 * If not, the reason may be as simple as it has not beem implemented
	 * to something specific about the disk.
	 */
	public abstract boolean canReadFileData();
	
	/**
	 * Indicates if this disk image can write data to a file.
	 * If not, the reason may be as simple as it has not beem implemented
	 * to something specific about the disk (such as read-only image).
	 */
	public abstract boolean canWriteFileData();

	/**
	 * Identify if this disk format is capable of having directories.
	 */
	public abstract boolean canHaveDirectories();
	
	/**
	 * Indicates if this disk image can delete a file.
	 * If not, the reason may be as simple as it has not beem implemented
	 * to something specific about the disk.
	 */
	public abstract boolean canDeleteFile();
	
	/**
	 * Get the data associated with the specified FileEntry.
	 * This is just the raw data.  Use the FileEntry itself to read
	 * data appropriately!  For instance, DOS "B" (binary) files store
	 * length and address as part of the file itself, but it is not treated
	 * as file data.
	 * @see FileEntry#getFileData()
	 */
	public abstract byte[] getFileData(FileEntry fileEntry);
	
	/**
	 * Locate a specific file by filename.
	 * Returns a null if specific filename is not located.
	 */
	public FileEntry getFile(String filename) {
		List files = getFiles();
		return getFile(files, filename.trim());
	}
	
	/**
	 * Recursive routine to locate a specific file by filename.
	 * Note that in the instance of a system with directories (ie, ProDOS),
	 * this really returns the first file with the given filename.
	 */
	protected FileEntry getFile(List files, String filename) {
		FileEntry theFileEntry = null;
		if (files != null) {
			for (int i=0; i<files.size(); i++) {
				FileEntry entry = (FileEntry) files.get(i);
				if (entry.isDirectory()) {
					theFileEntry = getFile(
						((DirectoryEntry)entry).getFiles(), filename);
					if (theFileEntry != null) break;
				}
				String otherFilename = entry.getFilename();
				if (otherFilename != null) otherFilename = otherFilename.trim();
				if (filename.equalsIgnoreCase(otherFilename)) {
					theFileEntry = entry;
					break;
				}
			}
		}
		return theFileEntry;
	}
	
	/**
	 * Format the disk.  Make sure that this is what is intended -
	 * there is no backing out!
	 */
	public abstract void format();
	
	/**
	 * Write the AppleCommander boot code to track 0 sector 0 of
	 * the disk.  This will work for a floppy, but may cause problems
	 * for other devices.
	 */
	protected void writeBootCode() {
		InputStream inputStream = getClass().
			getResourceAsStream("/com/webcodepro/applecommander/storage/AppleCommander-boot.dump"); //$NON-NLS-1$
		if (inputStream != null) {
			byte[] bootCode = new byte[SECTOR_SIZE];
			try {
				inputStream.read(bootCode, 0, bootCode.length);
				writeSector(0, 0, bootCode);
			} catch (IOException ignored) {
				// Ignored
			}
		}
	}
	
	/**
	 * Returns the logical disk number.  This can be used to identify
	 * between disks when a format supports multiple logical volumes.
	 * If a value of 0 is returned, there is not multiple logical
	 * volumes to distinguish.
	 */
	public abstract int getLogicalDiskNumber();
	
	/**
	 * Returns a valid filename for the given filename.  This does not
	 * necessarily guarantee a unique filename - just validity of 
	 * the filename.
	 */
	public abstract String getSuggestedFilename(String filename);
	
	/**
	 * Returns a valid filetype for the given filename.  The most simple
	 * format will just assume a filetype of binary.  This method is
	 * available for the interface to make an intelligent first guess
	 * as to the filetype.
	 */
	public abstract String getSuggestedFiletype(String filename);
	
	/**
	 * Returns a list of possible file types.  Since the filetype is
	 * specific to each operating system, a simple String is used.
	 */
	public abstract String[] getFiletypes();

	/**
	 * Indicates if this filetype requires an address component.
	 */
	public abstract boolean needsAddress(String filetype);

	/**
	 * Get the FormattedDisk associated with this DirectoryEntry.
	 * This is useful to interfaces that need to retrieve the associated
	 * disk.
	 */
	public FormattedDisk getFormattedDisk() {
		return this;
	}
	
	/**
	 * Resize the disk image to be its full size.  Only invole this
	 * method if a size does not match exception is thrown.
	 */
	public void resizeDiskImage() {
		resizeDiskImage(getFreeSpace() + getUsedSpace());
	}

	/**
	 * Indicates if this FormattedDisk supports a disk map.
	 */	
	public boolean supportsDiskMap() {
		return false;
	}

	/**
	 * Change the physical ordering of the disk.  This must be implemented by all
	 * subclasses.  See AppleUtil for common utility methods.  (It is assumed that a
	 * disk needs to be copied in the appropriate order - ie, by track and sector for
	 * a DOS type disk or by blocks in a ProDOS type disk.)
	 */
	public abstract void changeImageOrder(ImageOrder imageOrder);
	
	/**
	 * Writes the raw bytes into the file.  This bypasses any special formatting
	 * of the data (such as prepending the data with a length and/or an address).
	 * Typically, the FileEntry.setFileData method should be used. 
	 */
	public abstract void setFileData(FileEntry fileEntry, byte[] fileData) throws DiskFullException;
}
