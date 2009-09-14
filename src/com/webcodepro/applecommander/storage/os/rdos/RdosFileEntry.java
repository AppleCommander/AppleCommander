/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-3 by Robert Greene
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
package com.webcodepro.applecommander.storage.os.rdos;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.StorageBundle;
import com.webcodepro.applecommander.storage.filters.ApplesoftFileFilter;
import com.webcodepro.applecommander.storage.filters.BinaryFileFilter;
import com.webcodepro.applecommander.storage.filters.GraphicsFileFilter;
import com.webcodepro.applecommander.storage.filters.IntegerBasicFileFilter;
import com.webcodepro.applecommander.storage.filters.TextFileFilter;
import com.webcodepro.applecommander.util.AppleUtil;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Handle RDOS file entry format.
 * <p>
 * Since I was unable to locate the file entries on the internet, it is documented here:
 * <tt>
 * Offset   Description<br>
 * ======   ====================================================<br>
 * $00-$17  File name; space-filled.  If the first byte is $00, that is the end of the<br>
 *          directory.  If the first byte is $80, the file is deleted.<br>
 * $18      File type. Appears to be actual letter ('A'=Applesoft, etc)<br>
 * $19      File length in blocks (block = sector = 256 bytes)<br>
 * $1A-$1B  Address of application.  For Applesoft and binary; others may vary.<br>
 * $1C-$1D  Length in bytes of file.<br>
 * $1E-$1F  Starting block of application.<br>
 * </tt>
 * <p>
 * Date created: Oct 7, 2002 1:36:56 PM
 * @author Rob Greene
 */
public class RdosFileEntry implements FileEntry {
	private TextBundle textBundle = StorageBundle.getInstance();
	private byte[] fileEntry;
	private RdosFormatDisk disk;

	/**
	 * Constructor for RdosFileEntry.
	 */
	public RdosFileEntry(byte[] fileEntry, RdosFormatDisk disk) {
		super();
		this.fileEntry = fileEntry;
		this.disk = disk;
	}
	
	/**
	 * Return the number of blocks this file uses.
	 */
	public int getSizeInBlocks() {
		return AppleUtil.getUnsignedByte(fileEntry[0x19]);
	}
	
	/**
	 * Return the starting block of this application.
	 */
	public int getStartingBlock() {
		return AppleUtil.getWordValue(fileEntry, 0x1e);
	}
	
	/**
	 * Return the address of application.
	 */
	public int getAddress() {
		return AppleUtil.getWordValue(fileEntry, 0x1a);
	}

	/**
	 * Return the name of this file.
	 */
	public String getFilename() {
		return isDeleted() ? textBundle.get("RdosFileEntry.NotInUse") : AppleUtil.getString(fileEntry, 0, 24).trim(); //$NON-NLS-1$
	}

	/**
	 * Set the name of this file.
	 */
	public void setFilename(String filename) {
		// FIXME: Need to implement!
	}

	/**
	 * Return the maximum filename length.
	 */
	public int getMaximumFilenameLength() {
		return 24;
	}
	
	/**
	 * Return the filetype of this file.
	 */
	public String getFiletype() {
		return isDeleted() ? " " : AppleUtil.getString(fileEntry, 0x18, 1); //$NON-NLS-1$
	}

	/**
	 * Set the filetype.
	 */
	public void setFiletype(String filetype) {
		// FIXME: Implement!
	}
	
	/**
	 * Locked doesn't appear to be a concept under RDOS.
	 */
	public boolean isLocked() {
		return false;
	}

	/**
	 * Set the lock indicator.
	 */
	public void setLocked(boolean lock) {
		// FIXME: Implement!
	}
	
	/**
	 * Compute the size of this file (in bytes).
	 */
	public int getSize() {
		return AppleUtil.getWordValue(fileEntry, 0x1c);
	}
	
	/**
	 * RDOS does not support directories.
	 */
	public boolean isDirectory() {
		return false;
	}
	
	/**
	 * Retrieve the list of files in this directory.
	 * Since RDOS does not support directories, this will always return null.
	 */
	public List getFiles() {
		return null;
	}
	
	/**
	 * Identify if this file has been deleted.
	 */
	public boolean isDeleted() {
		return AppleUtil.getUnsignedByte(fileEntry[0]) == 0x80;
	}

	/**
	 * Delete the file.
	 */
	public void delete() {
		// FIXME: Need to implement!
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
				list.add(getFiletype());
				numberFormat.setMinimumIntegerDigits(3);
				list.add(numberFormat.format(getSizeInBlocks()));
				list.add(getFilename());
				numberFormat.setMinimumIntegerDigits(1);
				list.add(numberFormat.format(getSize()));
				numberFormat.setMinimumIntegerDigits(3);
				list.add(numberFormat.format(getStartingBlock()));
				break;
			case FormattedDisk.FILE_DISPLAY_DETAIL:
				list.add(getFiletype());
				numberFormat.setMinimumIntegerDigits(3);
				list.add(numberFormat.format(getSizeInBlocks()));
				list.add(getFilename());
				numberFormat.setMinimumIntegerDigits(1);
				list.add(numberFormat.format(getSize()));
				numberFormat.setMinimumIntegerDigits(3);
				list.add(numberFormat.format(getStartingBlock()));
				list.add("$" + AppleUtil.getFormattedWord(getAddress())); //$NON-NLS-1$
				list.add(isDeleted() ? textBundle.get("Deleted") : "");  //$NON-NLS-1$//$NON-NLS-2$
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
	 * Get file data.  This handles any operating-system specific issues.
	 * Currently, the disk itself handles this.
	 */
	public byte[] getFileData() {
		byte[] rawdata = disk.getFileData(this);
		byte[] filedata = new byte[getSize()];
		System.arraycopy(rawdata, 0, filedata, 0, filedata.length);
		return filedata;
	}

	/**
	 * Set file data.  This, essentially, is saving data to disk using this
	 * file entry.
	 */
	public void setFileData(byte[] data) throws DiskFullException {
		// FIXME: Implement!
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
		// FIXME - need to implement
	}

	/**
	 * Indicates that this filetype can be compiled.
	 * WARNING: RDOS programs most likely will not have the
	 * DOS routines handled by the compiler.
	 */
	public boolean canCompile() {
		return isApplesoftBasicFile();
	}
}
