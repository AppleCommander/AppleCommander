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
package com.webcodepro.applecommander.storage.os.dos33;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.StorageBundle;
import com.webcodepro.applecommander.storage.filters.ApplesoftFileFilter;
import com.webcodepro.applecommander.storage.filters.AssemblySourceFileFilter;
import com.webcodepro.applecommander.storage.filters.BinaryFileFilter;
import com.webcodepro.applecommander.storage.filters.GraphicsFileFilter;
import com.webcodepro.applecommander.storage.filters.IntegerBasicFileFilter;
import com.webcodepro.applecommander.storage.filters.TextFileFilter;
import com.webcodepro.applecommander.util.AppleUtil;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Represents a DOS file entry on disk.
 * <p>
 * Date created: Oct 4, 2002 5:15:25 PM
 * @author Rob Greene
 */
public class DosFileEntry implements FileEntry {
	private TextBundle textBundle = StorageBundle.getInstance();
	/**
	 * Indicates the length in bytes of the DOS file entry field.
	 */
	public static final int FILE_DESCRIPTIVE_ENTRY_LENGTH = 35;
	/**
	 * Holds the disk the FileEntry is attached to.
	 */
	private DosFormatDisk disk;
	/**
	 * Track of the FileEntry location.
	 */
	private int track;
	/**
	 * Sector of the FileEntry location.
	 */
	private int sector;
	/**
	 * Offset into sector of FileEntry location.
	 */
	private int offset;
	/**
	 * Temporary location to store the DOS address (which is stored with
	 * the contents of the data file).
	 */
	private Integer address;

	/**
	 * Constructor for DosFileEntry.
	 */
	public DosFileEntry(DosFormatDisk disk, int track, int sector, int offset) {
		super();
		this.disk = disk;
		this.track = track;
		this.sector = sector;
		this.offset = offset;
	}
	
	/**
	 * Read the FileEntry from the disk image.
	 */
	protected byte[] readFileEntry() {
		byte[] sectorData = disk.readSector(track, sector);
		byte[] fileEntry = new byte[FILE_DESCRIPTIVE_ENTRY_LENGTH];
		System.arraycopy(sectorData, offset, fileEntry, 0, fileEntry.length);
		return fileEntry;
	}
	
	/**
	 * Write the FileEntry to the disk image.
	 */
	protected void writeFileEntry(byte[] fileEntry) {
		if (fileEntry.length != FILE_DESCRIPTIVE_ENTRY_LENGTH) {
			throw new IllegalArgumentException(textBundle.
					format("DosFileEntry.DosFileEntryLengthError", //$NON-NLS-1$
							FILE_DESCRIPTIVE_ENTRY_LENGTH));
		}
		byte[] sectorData = disk.readSector(track, sector);
		System.arraycopy(fileEntry, 0, sectorData, offset, fileEntry.length);
		disk.writeSector(track, sector, sectorData);
	}

	/**
	 * Return the maximum filename length.
	 */
	public int getMaximumFilenameLength() {
		return 30;
	}

	/**
	 * Return the name of this file.
	 * @see com.webcodepro.applecommander.storage.FileEntry#getFilename()
	 */
	public String getFilename() {
		return AppleUtil.getString(readFileEntry(), 3, 30).trim();
	}
	
	/**
	 * Set the name of this file.
	 */
	public void setFilename(String filename) {
		byte[] data = readFileEntry();
		AppleUtil.setString(data, 3, filename.toUpperCase(), 30);
		writeFileEntry(data);
	}

	/**
	 * Return the filetype of this file.
	 * @see com.webcodepro.applecommander.storage.FileEntry#getFiletype()
	 */
	public String getFiletype() {
		int filetype = (AppleUtil.getUnsignedByte(readFileEntry()[2]) & 0x7f);
		if (filetype == 0x00) return "T"; //$NON-NLS-1$
		// the "^" operator is exclusive or - used to ensure only that
		// bit was turned on.  if others are turned on, fall through and
		// return a "?" as the file type
		if ((filetype ^ 0x01) == 0) return "I"; //$NON-NLS-1$
		if ((filetype ^ 0x02) == 0) return "A"; //$NON-NLS-1$
		if ((filetype ^ 0x04) == 0) return "B"; //$NON-NLS-1$
		if ((filetype ^ 0x08) == 0) return "S"; //$NON-NLS-1$
		if ((filetype ^ 0x10) == 0) return "R"; //$NON-NLS-1$
		if ((filetype ^ 0x20) == 0) return "a"; //$NON-NLS-1$
		if ((filetype ^ 0x40) == 0) return "b"; //$NON-NLS-1$
		return "?";	// should never occur (read the code!) //$NON-NLS-1$
	}

	/**
	 * Set the filetype.
	 */
	public void setFiletype(String filetype) {
		byte[] data = readFileEntry();
		int type = 0x04;	// assume binary
		if ("T".equals(filetype)) type = 0x00; //$NON-NLS-1$
		if ("I".equals(filetype)) type = 0x01; //$NON-NLS-1$
		if ("A".equals(filetype)) type = 0x02; //$NON-NLS-1$
		if ("B".equals(filetype)) type = 0x04; //$NON-NLS-1$
		if ("S".equals(filetype)) type = 0x08; //$NON-NLS-1$
		if ("R".equals(filetype)) type = 0x10; //$NON-NLS-1$
		if ("a".equals(filetype)) type = 0x20; //$NON-NLS-1$
		if ("b".equals(filetype)) type = 0x40; //$NON-NLS-1$
		type = (type | (data[2] & 0x80));
		data[2] = (byte) type;
		writeFileEntry(data);
	}

	/**
	 * Identify if this file is locked.
	 * @see com.webcodepro.applecommander.storage.FileEntry#isLocked()
	 */
	public boolean isLocked() {
		return (readFileEntry()[2] & 0x80) != 0;
	}

	/**
	 * Set the lock indicator.
	 */
	public void setLocked(boolean lock) {
		byte[] data = readFileEntry();
		if (lock) {
			data[2] = (byte)(AppleUtil.getUnsignedByte(data[2]) | 0x80);
		} else {
			data[2] = (byte)(AppleUtil.getUnsignedByte(data[2]) & 0x7f);
		}
		writeFileEntry(data);
	}

	/**
	 * Compute the size of this file (in bytes).
	 * @see com.webcodepro.applecommander.storage.FileEntry#getSize()
	 */
	public int getSize() {
		byte[] rawdata = null;
		if (!isDeleted()) {
			rawdata = disk.getFileData(this);
		}
		// default to nothing special, just compute from number of sectors
		int size = (getSectorsUsed()-1) * Disk.SECTOR_SIZE;
		if (size < 1) size = 0;	// we assume a T/S block is included (may not be)
		if (rawdata != null) {
			if ("B".equals(getFiletype())) { //$NON-NLS-1$
				// binary
				return AppleUtil.getWordValue(rawdata, 2);
			} else if ("A".equals(getFiletype()) || "I".equals(getFiletype())) { //$NON-NLS-1$ //$NON-NLS-2$
				// applesoft, integer basic
				return AppleUtil.getWordValue(rawdata, 0);
			}
		}
		return size;
	}
	
	/**
	 * Compute the number of sectors used.
	 */
	public int getSectorsUsed() {
		return AppleUtil.getWordValue(readFileEntry(), 0x21);
	}
	
	/**
	 * Set the number of sectors used.
	 */
	public void setSectorsUsed(int sectorsUsed) {
		byte[] data = readFileEntry();
		AppleUtil.setWordValue(data, 0x21, sectorsUsed);
		writeFileEntry(data);
	}

	/**
	 * Identify if this is a directory file.
	 * @see com.webcodepro.applecommander.storage.FileEntry#isDirectory()
	 */
	public boolean isDirectory() {
		return false;
	}

	/**
	 * Identify if this file has been deleted.
	 * @see com.webcodepro.applecommander.storage.FileEntry#isDeleted()
	 */
	public boolean isDeleted() {
		return AppleUtil.getUnsignedByte(readFileEntry()[0]) == 0xff;
	}
	
	/**
	 * Delete this file.
	 */
	public void delete() {
		disk.freeSectors(this);
		byte[] fileEntry = readFileEntry();
		fileEntry[0x20] = fileEntry[0x00];
		fileEntry[0x00] = (byte)0xff;
		writeFileEntry(fileEntry);
	}

	/**
	 * Get the standard file column header information.
	 * This default implementation is intended only for standard mode.
	 * displayMode is specified in FormattedDisk.
	 */
	public List getFileColumnData(int displayMode) {
		NumberFormat numberFormat = NumberFormat.getNumberInstance();

		List list = new ArrayList();
		switch (displayMode) {
			case FormattedDisk.FILE_DISPLAY_NATIVE:
				list.add(isLocked() ? "*" : " "); //$NON-NLS-1$ //$NON-NLS-2$
				list.add(getFiletype());
				numberFormat.setMinimumIntegerDigits(3);
				list.add(numberFormat.format(getSectorsUsed()));
				list.add(getFilename());
				break;
			case FormattedDisk.FILE_DISPLAY_DETAIL:
				list.add(isLocked() ? "*" : " "); //$NON-NLS-1$ //$NON-NLS-2$
				list.add(getFiletype());
				list.add(getFilename());
				list.add(numberFormat.format(getSize()));
				numberFormat.setMinimumIntegerDigits(3);
				list.add(numberFormat.format(getSectorsUsed()));
				list.add(isDeleted() ? textBundle.get("Deleted") : "");  //$NON-NLS-1$//$NON-NLS-2$
				list.add("T" + getTrack() + " S" + getSector()); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			default:	// FILE_DISPLAY_STANDARD
				list.add(getFilename());
				list.add(getFiletype());
				list.add(numberFormat.format(getSize()));
				list.add(isLocked() ? textBundle.get("Locked") : "");  //$NON-NLS-1$//$NON-NLS-2$
				break;
		}
		return list;
	}
	
	/**
	 * Get the track of first track/sector list sector.
	 */
	public int getTrack() {
		return AppleUtil.getUnsignedByte(readFileEntry()[0x00]);
	}

	/**
	 * Set the track of the first track/sector list sector.
	 */
	public void setTrack(int track) {
		byte[] data = readFileEntry();
		data[0x00] = (byte) track;
		writeFileEntry(data);
	}
	
	/**
	 * Get the sector of first track/sector list sector.
	 */
	public int getSector() {
		return AppleUtil.getUnsignedByte(readFileEntry()[0x01]);
	}
	
	/**
	 * Set the sector of the first track/sector list sector.
	 */
	public void setSector(int sector) {
		byte[] data = readFileEntry();
		data[0x01] = (byte) sector;
		writeFileEntry(data);
	}

	/**
	 * Get file data.  This handles any operating-system specific issues.
	 * Specifically, DOS 3.3 places address and length into binary files
	 * and length into Applesoft files.
	 */
	public byte[] getFileData() {
		byte[] rawdata = disk.getFileData(this);
		byte[] filedata;
		if (isBinaryFile()) {
			int length = AppleUtil.getWordValue(rawdata, 2);
			filedata = new byte[length];
			System.arraycopy(rawdata, 4, filedata, 0, length);
		} else if (isApplesoftBasicFile() || isIntegerBasicFile()) {
			filedata = new byte[getSize()];
			System.arraycopy(rawdata, 2, filedata, 0, filedata.length);
		} else {
			filedata = rawdata;
		}
		return filedata;
	}
	
	/**
	 * Set the file data.  This is essentially the save operation.
	 * Specifically, if the filetype is binary, the length and
	 * address need to be set.  If the filetype is applesoft or
	 * integer basic, the start address needs to be set.
	 * 
	 * Note: The address can be set before the data is saved or
	 * after the data is saved.  This is an attempt to make the
	 * API more easily usable.
	 * 
	 * Empirically, the data must be set before the address is set.
	 */
	public void setFileData(byte[] data) throws DiskFullException {
		if (isBinaryFile()) {
			byte[] filedata = new byte[data.length + 4];
			if (address != null) {
				AppleUtil.setWordValue(filedata, 0, address.intValue());
				address = null;
			} else {
				AppleUtil.setWordValue(filedata, 0, 0); // Needs to be set via setAddress
			}
			AppleUtil.setWordValue(filedata, 2, data.length);
			System.arraycopy(data, 0, filedata, 4, data.length);
			disk.setFileData(this, filedata);
		} else if (isApplesoftBasicFile() || isIntegerBasicFile()) {
			byte[] filedata = new byte[data.length + 2];
			AppleUtil.setWordValue(filedata, 0, data.length);
			System.arraycopy(data, 0, filedata, 2, data.length);
			disk.setFileData(this, filedata);
		} else if (isTextFile()) {
			for (int i=0; i<data.length; i++) {
				data[i] = (byte)(data[i] | 0x80);
			}
			disk.setFileData(this, data);
		} else {
			disk.setFileData(this, data);
		}
	}

	/**
	 * Get the suggested FileFilter.  This appears to be operating system
	 * specific, so each operating system needs to implement some manner
	 * of guessing the appropriate filter.
	 * FIXME - this code should be a helper class for DOS and RDOS!
	 */
	public FileFilter getSuggestedFilter() {
		if (isApplesoftBasicFile()) {
			return new ApplesoftFileFilter();
		} else if (isIntegerBasicFile()) {
			return new IntegerBasicFileFilter();
		} else if (isAssemblySourceFile()) {
			return new AssemblySourceFileFilter();
		} else if (isTextFile()) {
			return new TextFileFilter();
		} else if (isBinaryFile()) {
			int size = getSize();
			// the minimum size is guessed a bit - I don't remember, but maybe there
			// are 8 spare bytes at the end of the graphics screen
			GraphicsFileFilter filter = new GraphicsFileFilter();
			if (size >= 8184 && size <= 8192) {
				filter.setMode(GraphicsFileFilter.MODE_HGR_COLOR);
				return filter;
			} else if (size >= 16377 && size <= 16384) {
				filter.setMode(GraphicsFileFilter.MODE_DHR_COLOR);
				return filter;
			}
			// fall through to BinaryFileFilter...
		}
		return new BinaryFileFilter();
	}
	
	/**
	 * Determine if this is an assembly source code file.
	 */
	public boolean isAssemblySourceFile() {
		boolean rightFiletype = isTextFile() || isBinaryFile();
		if (rightFiletype && getFilename().endsWith(".S")) return true; //$NON-NLS-1$
		if (rightFiletype && getFilename().startsWith("T.")) return true; //$NON-NLS-1$
		return false;
	}

	/**
	 * Determine if this is a text file.
	 */
	public boolean isTextFile() {
		return "T".equals(getFiletype()); //$NON-NLS-1$
	}
	
	/**
	 * Determine if this is an Applesoft BASIC file.
	 */
	public boolean isApplesoftBasicFile() {
		return "A".equals(getFiletype()); //$NON-NLS-1$
	}

	/**
	 * Determine if this is an Integer BASIC file.
	 */
	public boolean isIntegerBasicFile() {
		return "I".equals(getFiletype()); //$NON-NLS-1$
	}

	/**
	 * Determine if this is a binary file.
	 */
	public boolean isBinaryFile() {
		return "B".equals(getFiletype()); //$NON-NLS-1$
	}
	
	/**
	 * Get the FormattedDisk associated with this FileEntry.
	 * This is useful to interfaces that need to retrieve the associated
	 * disk.
	 */
	public FormattedDisk getFormattedDisk() {
		return disk;
	}

	/**
	 * Indicates if this filetype requires an address component.
	 * Note that the FormattedDisk also has this method - normally,
	 * this will defer to the method on FormattedDisk, as it will be
	 * more generic.
	 */
	public boolean needsAddress() {
		return disk.needsAddress(getFiletype());
	}
	
	/**
	 * Set the address that this file loads at.
	 */
	public void setAddress(int address) {
		try {
			byte[] rawdata = disk.getFileData(this);
			if (rawdata == null || rawdata.length == 0) {
				this.address = new Integer(address);
			} else {
				AppleUtil.setWordValue(rawdata, 0, address);
				disk.setFileData(this, rawdata);
			}
		} catch (DiskFullException e) {
			// Should not be possible when the file isn't being modified!!
			throw new IllegalStateException(textBundle.
					format("DosFileEntry.UnableToSetAddressError", getFilename())); //$NON-NLS-1$
		}
	}

	/**
	 * Indicates that this filetype can be compiled.
	 */
	public boolean canCompile() {
		return isApplesoftBasicFile();
	}
}
