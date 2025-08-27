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

import com.webcodepro.applecommander.util.TextBundle;
import org.applecommander.source.Source;
import org.applecommander.util.DataBuffer;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * Abstract representation of a formatted Apple2 disk (floppy, 800k, hard disk).
 * <p>
 * Date created: Oct 5, 2002 3:51:44 PM
 * @author Rob Greene
 */
public abstract class FormattedDisk implements DirectoryEntry {
	private final TextBundle textBundle = StorageBundle.getInstance();
	/**
	 * Use this inner class for label/value mappings in the disk info page.
	 */
	public static class DiskInformation {
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
	public static class FileColumnHeader {
		public static final int ALIGN_LEFT = 1;
		public static final int ALIGN_CENTER = 2;
		public static final int ALIGN_RIGHT = 3;
		private String title;
		private int maximumWidth;
		private int alignment;
		private String key;
		public FileColumnHeader(String title, int maximumWidth, int alignment, String key) {
			this.title = title;
			this.maximumWidth = maximumWidth;
			this.alignment = alignment;
			this.key = key;
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
		public String getKey() {
			return key;
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

    private String filename;
    private boolean newImage = false;
    private final Source source;

    /**
	 * Constructor for FormattedDisk.
	 */
	public FormattedDisk(String filename, Source source) {
        this.source = source;
        this.filename = filename;
        this.newImage = true;
	}

	/**
	 * Return the name of the disk.  Not the physical file name,
	 * but "DISK VOLUME #xxx" (DOS 3.3) or "/MY.DISK" (ProDOS).
	 */
	public abstract String getDiskName();

	/**
	 * Return a name for this directory.
	 */
	public String getDirname(){
		return getDiskName();
	}

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
	public List<DiskInformation> getDiskInformation() {
		List<DiskInformation> list = new ArrayList<>();
		list.add(new DiskInformation(textBundle.get("FormattedDisk.FileName"), getFilename())); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("FormattedDisk.DiskName"), getDiskName())); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("FormattedDisk.FreeSpaceInBytes"), getFreeSpace())); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("FormattedDisk.UsedSpaceInBytes"), getUsedSpace())); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("FormattedDisk.FreeSpaceInKb"), getFreeSpace() / 1024)); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("FormattedDisk.UsedSpaceInKb"), getUsedSpace() / 1024)); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("FormattedDisk.DiskFormat"), getFormat())); //$NON-NLS-1$
		return list;
	}
	
	/**
	 * Get the standard file column header information.
	 * This default implementation is intended only for standard mode.
	 */
	public List<FileColumnHeader> getFileColumnHeaders(int displayMode) {
		List<FileColumnHeader> list = new ArrayList<>();
		list.add(new FileColumnHeader(textBundle
                .get("Name"), 30, FileColumnHeader.ALIGN_LEFT, "name"));
		list.add(new FileColumnHeader(textBundle
                .get("Type"), 8, FileColumnHeader.ALIGN_CENTER, "type"));
		list.add(new FileColumnHeader(textBundle
                .get("SizeInBytes"), 6, FileColumnHeader.ALIGN_RIGHT, "sizeInBytes"));
		list.add(new FileColumnHeader(textBundle
                .get("LockedQ"), 6, FileColumnHeader.ALIGN_CENTER, "locked"));
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
	public FileEntry getFile(String filename) throws DiskException {
		List<FileEntry> files = getFiles();
		return getFile(files, filename.trim());
	}
	
	/**
	 * Recursive routine to locate a specific file by filename.
	 * Note that in the instance of a system with directories (ie, ProDOS),
	 * this really returns the first file with the given filename.
	 */
	protected FileEntry getFile(List<FileEntry> files, String filename) throws DiskException {
		FileEntry theFileEntry = null;
		if (files != null) {
		    for (FileEntry entry : files) {
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
	 * Indicates if this FormattedDisk supports a disk map.
	 */	
	public boolean supportsDiskMap() {
		return false;
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
        if (getFilename().toLowerCase().endsWith(".gz")) {
            output = new GZIPOutputStream(output);
        }
        DataBuffer data = getSource().readAllBytes();
        byte[] fileData = new byte[data.limit()];
        data.read(fileData);
        output.write(fileData);
        output.close();
        getSource().clearChanges();
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
     * Returns the source.
     */
    public Source getSource() {
        return source;
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
     * Indicates if the disk has changed. Triggered when data is
     * written and cleared when data is saved.
     */
    public boolean hasChanged() {
        return getSource().hasChanged();
    }

    /**
     * Indicates if the disk image is new.  This can be used
     * for Save As processing.
     */
    public boolean isNewImage() {
        return newImage;
    }

	/**
	 * Writes the raw bytes into the file.  This bypasses any special formatting
	 * of the data (such as prepending the data with a length and/or an address).
	 * Typically, the FileEntry.setFileData method should be used. 
	 */
	public abstract void setFileData(FileEntry fileEntry, byte[] fileData) throws DiskFullException;
	
	/**
	 * Gives an indication on how this disk's geometry should be handled.
	 */
	public abstract DiskGeometry getDiskGeometry();

	/**
	 * Provides conversation from a given ProDOS file type since as it is common across
	 * many archiving tools. This should also allow "native" filetypes to just pass-through.
	 * (For example, a "B" in a DOS disk should just return a "B" while a "BAS" would be
	 * transformed to a "B" filetype.)
	 */
	public abstract String toNativeFiletype(String prodosFiletype);
	/**
	 * Provides conversation to a given ProDOS file type since as it is common across
	 * many archiving tools.
	 */
	public abstract String toProdosFiletype(String nativeFiletype);
}
