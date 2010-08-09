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
package com.webcodepro.applecommander.storage.os.nakedos;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.StorageBundle;
import com.webcodepro.applecommander.storage.filters.BinaryFileFilter;
import com.webcodepro.applecommander.util.AppleUtil;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Represents a Nakedos file entry on disk.
 * <p>
 * Date created: August 5, 2010 10:23:23 AM
 * @author David Schmidt
 */
public class NakedosFileEntry implements FileEntry {

	private TextBundle textBundle = StorageBundle.getInstance();
	/**
	 * Holds the disk the FileEntry is attached to.
	 */
	private NakedosFormatDisk disk;
	/**
	 * The file number/name of this file
	 */
	private int fileNumber;
	/**
	 * The number of sectors used by this file
	 */
	private int size;

	/**
	 * Constructor for NakedosFileEntry.
	 */
	public NakedosFileEntry(NakedosFormatDisk disk, int fileNumber, int size) {
		super();
		this.disk = disk;
		this.fileNumber = fileNumber;
		this.size = size;
	}

	/**
	 * Return the name of this file.
	 * @see com.webcodepro.applecommander.storage.FileEntry#getFilename()
	 */
	public String getFilename() {
		return AppleUtil.getFormattedByte(fileNumber);
	}

	/**
	 * Return the name of this file.
	 * @see com.webcodepro.applecommander.storage.FileEntry#getFilename()
	 */
	public int getFileNumber() {
		return fileNumber;
	}

	/**
	 * Set the name of this file.
	 */
	public void setFilename(String filename) {
		/* Not sure that there's a useful analogy for NakedOS... */
	}

	/**
	 * Return the filetype of this file.
	 * @see com.webcodepro.applecommander.storage.FileEntry#getFiletype()
	 */
	public String getFiletype() {
		return "B";	// Only one file type... binary //$NON-NLS-1$
	}

	/**
	 * Set the filetype (typeless - unused)
	 */
	public void setFiletype(String filetype) {
		/* Not sure that there's a useful analogy for NakedOS... */
	}

	/**
	 * Identify if this file is locked.
	 * @see com.webcodepro.applecommander.storage.FileEntry#isLocked()
	 */
	public boolean isLocked() {
		/* No file locking in NakedOS */
		return false;
	}

	/**
	 * Set the lock indicator (unused)
	 */
	public void setLocked(boolean lock) {
		/* No file locking in NakedOS */
	}

	/**
	 * Compute the size of this file (in bytes).
	 * @see com.webcodepro.applecommander.storage.FileEntry#getSize()
	 */
	public int getSize() {
		return size*256;
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
		return false;
	}

	/**
	 * Delete this file (unimplemented).
	 */
	public void delete() {
		// TODO Auto-generated method stub
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
				list.add(numberFormat.format(getSize()/256));
				list.add(getFilename());
				break;
			case FormattedDisk.FILE_DISPLAY_DETAIL:
				list.add(isLocked() ? "*" : " "); //$NON-NLS-1$ //$NON-NLS-2$
				list.add(getFiletype());
				list.add(getFilename());
				list.add(numberFormat.format(getSize()));
				numberFormat.setMinimumIntegerDigits(3);
				list.add(numberFormat.format(getSize()/256));
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
	 */
	public byte[] getFileData() {
		return disk.getFileData(this);
	}

	/**
	 * Set the file data.
	 * 
	 * Note: The address can be set before the data is saved or
	 * after the data is saved.  This is an attempt to make the
	 * API more easily usable.
	 * 
	 * Empirically, the data must be set before the address is set.
	 */
	public void setFileData(byte[] data) throws DiskFullException {
		disk.setFileData(this, data);
	}

	/**
	 * Get the suggested FileFilter.  This appears to be operating system
	 * specific, so each operating system needs to implement some manner
	 * of guessing the appropriate filter.
	 * FIXME - this code should be a helper class for DOS and RDOS!
	 */
	public FileFilter getSuggestedFilter() {
		return new BinaryFileFilter();
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
	 * Return the maximum filename length.
	 */
	public int getMaximumFilenameLength() {
		return 2;
	}

	/**
	 * Indicates if this filetype requires an address component.
	 * Note that the FormattedDisk also has this method - normally,
	 * this will defer to the method on FormattedDisk, as it will be
	 * more generic.
	 */
	public boolean needsAddress() {
		return false;
	}

	/**
	 * Set the address that this file loads at.
	 */
	public void setAddress(int address) {
	}

	/**
	 * Indicates that this filetype can be compiled.
	 */
	public boolean canCompile() {
		return false;
	}

	public boolean equals (Object o) {
		return this.getFilename().equals(((NakedosFileEntry)o).getFilename());
	}
}
