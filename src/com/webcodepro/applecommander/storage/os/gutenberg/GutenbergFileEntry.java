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
package com.webcodepro.applecommander.storage.os.gutenberg;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.StorageBundle;
import com.webcodepro.applecommander.storage.filters.GutenbergFileFilter;
import com.webcodepro.applecommander.util.AppleUtil;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Represents a Gutenberg word processing file entry on disk.
 * <p>
 * Date created: Dec 17, 2008 04:29:23 PM
 * @author David Schmidt
 */
public class GutenbergFileEntry implements FileEntry {
	private TextBundle textBundle = StorageBundle.getInstance();
	/**
	 * Indicates the length in bytes of the DOS file entry field.
	 */
	public static final int FILE_DESCRIPTIVE_ENTRY_LENGTH = 16;
	/**
	 * Holds the disk the FileEntry is attached to.
	 */
	private GutenbergFormatDisk disk;
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
	 * Constructor for GutenbergFileEntry.
	 */
	public GutenbergFileEntry(GutenbergFormatDisk disk, int track, int sector, int offset) {
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
					format("GutenbergFileEntry.GutenbergFileEntryLengthError", //$NON-NLS-1$
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
		return 12;
	}

	/**
	 * Return the name of this file.
	 * @see com.webcodepro.applecommander.storage.FileEntry#getFilename()
	 */
	public String getFilename() {
		return AppleUtil.getString(readFileEntry(), 0, getMaximumFilenameLength()).trim();
	}
	
	/**
	 * Set the name of this file.
	 */
	public void setFilename(String filename) {
		byte[] data = readFileEntry();
		AppleUtil.setString(data, 0, filename.toUpperCase(), getMaximumFilenameLength());
		writeFileEntry(data);
	}

	/**
	 * Return the filetype of this file.
	 * @see com.webcodepro.applecommander.storage.FileEntry#getFiletype()
	 */
	public String getFiletype() {
		return "T";	// Only one file type... text //$NON-NLS-1$
	}

	/**
	 * Set the filetype (typeless - unused)
	 */
	public void setFiletype(String filetype) {
	}

	/**
	 * Identify if this file is locked.
	 * @see com.webcodepro.applecommander.storage.FileEntry#isLocked()
	 */
	public boolean isLocked() {
		return true;
	}

	/**
	 * Set the lock indicator (unused)
	 */
	public void setLocked(boolean lock) {
	}

	/**
	 * Compute the size of this file (in bytes).
	 * @see com.webcodepro.applecommander.storage.FileEntry#getSize()
	 */
	public int getSize() {
		// Nothing special, just compute from number of sectors
		int size = getSectorsUsed() * Disk.SECTOR_SIZE;
		return size;
	}
	
	/**
	 * Compute the number of sectors used.
	 */
	public int getSectorsUsed() {
		// Follow the chain of sectors to find the end.
		int track = getTrack();
		int sector = getSector();
		int sectors = 0;
		while (track < 128) {
			byte[] sectorData = disk.readSector(track, sector);
			track = AppleUtil.getUnsignedByte(sectorData[0x04]);
			sector = AppleUtil.getUnsignedByte(sectorData[0x05]);
			sectors++;
		}
		return sectors;
	}
	
	/**
	 * Set the number of sectors used.
	 */
	public void setSectorsUsed(int sectorsUsed) {
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
		return AppleUtil.getUnsignedByte(readFileEntry()[0x0d]) == 0x40;
	}
	
	/**
	 * Delete this file.
	 */
	public void delete() {
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
		return AppleUtil.getUnsignedByte(readFileEntry()[0x0c]);
	}

	/**
	 * Set the track of the first track/sector list sector.
	 */
	public void setTrack(int track) {
		byte[] data = readFileEntry();
		data[0x0c] = (byte) track;
		writeFileEntry(data);
	}
	
	/**
	 * Get the sector of first track/sector list sector.
	 */
	public int getSector() {
		return AppleUtil.getUnsignedByte(readFileEntry()[0x0d]);
	}
	
	/**
	 * Set the sector of the first track/sector list sector.
	 */
	public void setSector(int sector) {
		byte[] data = readFileEntry();
		data[0x0d] = (byte) sector;
		writeFileEntry(data);
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
		return new GutenbergFileFilter();
	}

	/**
	 * Determine if this is an assembly source code file.
	 */
	public boolean isAssemblySourceFile() {
		return false;
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
}
