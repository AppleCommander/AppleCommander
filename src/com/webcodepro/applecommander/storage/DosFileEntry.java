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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a DOS file entry on disk.
 * <p>
 * Date created: Oct 4, 2002 5:15:25 PM
 * @author: Rob Greene
 */
public class DosFileEntry implements FileEntry {
	private byte[] fileEntry;
	private DosFormatDisk disk;

	/**
	 * Constructor for DosFileEntry.
	 */
	public DosFileEntry(byte[] fileEntry, DosFormatDisk disk) {
		super();
		this.fileEntry = fileEntry;
		this.disk = disk;
	}

	/**
	 * Return the name of this file.
	 * @see com.webcodepro.applecommander.storage.FileEntry#getFilename()
	 */
	public String getFilename() {
		byte[] filename = new byte[30];
		System.arraycopy(fileEntry, 3, filename, 0, filename.length);
		for (int i=0; i<filename.length; i++) {
			filename[i] &= 0x7f;
		}
		return new String(filename).trim();
	}

	/**
	 * Return the filetype of this file.
	 * @see com.webcodepro.applecommander.storage.FileEntry#getFiletype()
	 */
	public String getFiletype() {
		int filetype = (AppleUtil.getUnsignedByte(fileEntry[2]) & 0x7f);
		if (filetype == 0x00) return "T";
		// the "^" operator is exclusive or - used to ensure only that
		// bit was turned on.  if others are turned on, fall through and
		// return a "?" as the file type
		if ((filetype ^ 0x01) == 0) return "I";
		if ((filetype ^ 0x02) == 0) return "A";
		if ((filetype ^ 0x04) == 0) return "B";
		if ((filetype ^ 0x08) == 0) return "S";
		if ((filetype ^ 0x10) == 0) return "R";
		if ((filetype ^ 0x20) == 0) return "a";
		if ((filetype ^ 0x40) == 0) return "b";
		return "?";	// should never occur (read the code!)
	}

	/**
	 * Identify if this file is locked.
	 * @see com.webcodepro.applecommander.storage.FileEntry#isLocked()
	 */
	public boolean isLocked() {
		return (fileEntry[2] & 0x80) != 0;
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
		if (rawdata != null) {
			if ("B".equals(getFiletype())) {
				// binary
				return AppleUtil.getWordValue(rawdata, 2);
			} else if ("A".equals(getFiletype()) || "I".equals(getFiletype())) {
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
		return AppleUtil.getUnsignedByte(fileEntry[0x21]) 
			+ AppleUtil.getUnsignedByte(fileEntry[0x22])*16;
	}

	/**
	 * Identify if this is a directory file.
	 * @see com.webcodepro.applecommander.storage.FileEntry#isDirectory()
	 */
	public boolean isDirectory() {
		return false;
	}

	/**
	 * Retrieve the list of files in this directory.
	 * Always returns null for DOS.
	 * @see com.webcodepro.applecommander.storage.FileEntry#getFiles()
	 */
	public List getFiles() {
		return null;
	}

	/**
	 * Identify if this file has been deleted.
	 * @see com.webcodepro.applecommander.storage.FileEntry#isDeleted()
	 */
	public boolean isDeleted() {
		return AppleUtil.getUnsignedByte(fileEntry[0]) == 0xff;
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
				list.add(isLocked() ? "*" : " ");
				list.add(getFiletype());
				numberFormat.setMinimumIntegerDigits(3);
				list.add(numberFormat.format(getSectorsUsed()));
				list.add(getFilename());
				break;
			case FormattedDisk.FILE_DISPLAY_DETAIL:
				list.add(isLocked() ? "*" : " ");
				list.add(getFiletype());
				list.add(getFilename());
				list.add(numberFormat.format(getSize()));
				numberFormat.setMinimumIntegerDigits(3);
				list.add(numberFormat.format(getSectorsUsed()));
				list.add(isDeleted() ? "Deleted" : "");
				list.add("T" + getTrack() + " S" + getSector());
				break;
			default:	// FILE_DISPLAY_STANDARD
				list.add(getFilename());
				list.add(getFiletype());
				list.add(numberFormat.format(getSize()));
				list.add(isLocked() ? "Locked" : "");
				break;
		}
		return list;
	}
	
	/**
	 * Get the track of first track/sector list sector.
	 */
	public int getTrack() {
		return AppleUtil.getUnsignedByte(fileEntry[0x00]);
	}
	
	/**
	 * Get the sector of first track/sector list sector.
	 */
	public int getSector() {
		return AppleUtil.getUnsignedByte(fileEntry[0x01]);
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
		} else if (isTextFile()) {
			return new TextFileFilter();
		} else if (isBinaryFile()) {
			int size = getSize();
			// the minimum size is guessed a bit - I don't remember, but maybe there
			// are 8 spare bytes at the end of the graphics screen
			GraphicsFileFilter filter = new GraphicsFileFilter();
			if (size >= 8185 && size <= 8192) {
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
	 * Determine if this is a text file.
	 */
	public boolean isTextFile() {
		return "T".equals(getFiletype());
	}
	
	/**
	 * Determine if this is an Applesoft BASIC file.
	 */
	public boolean isApplesoftBasicFile() {
		return "A".equals(getFiletype());
	}

	/**
	 * Determine if this is an Integer BASIC file.
	 */
	public boolean isIntegerBasicFile() {
		return "I".equals(getFiletype());
	}

	/**
	 * Determine if this is a binary file.
	 */
	public boolean isBinaryFile() {
		return "B".equals(getFiletype());
	}
}
