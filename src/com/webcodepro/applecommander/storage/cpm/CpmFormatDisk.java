/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2003 by Robert Greene
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
package com.webcodepro.applecommander.storage.cpm;

import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.FormattedDisk.DiskUsage;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a disk that is in the Apple CP/M format.
 * <p>
 * @author Rob Greene
 */
public class CpmFormatDisk extends FormattedDisk {
	/**
	 * Create a CP/M formatted disk.
	 */
	public CpmFormatDisk(String filename, byte[] diskImage) {
		super(filename, diskImage);
	}

	/**
	 * There apparantly is no corresponding CP/M disk name.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getDiskName()
	 */
	public String getDiskName() {
		return "CP/M Volume";
	}

	/**
	 * Identify the operating system format of this disk.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getFormat()
	 */
	public String getFormat() {
		return "CP/M";
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getFreeSpace()
	 */
	public int getFreeSpace() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getUsedSpace()
	 */
	public int getUsedSpace() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getBitmapDimensions()
	 */
	public int[] getBitmapDimensions() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getBitmapLength()
	 */
	public int getBitmapLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getDiskUsage()
	 */
	public DiskUsage getDiskUsage() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getBitmapLabels()
	 */
	public String[] getBitmapLabels() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Indicates if this disk format supports "deleted" files.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#supportsDeletedFiles()
	 */
	public boolean supportsDeletedFiles() {
		return true;
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#canReadFileData()
	 */
	public boolean canReadFileData() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#canWriteFileData()
	 */
	public boolean canWriteFileData() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#canHaveDirectories()
	 */
	public boolean canHaveDirectories() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#canDeleteFile()
	 */
	public boolean canDeleteFile() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getFileData(com.webcodepro.applecommander.storage.FileEntry)
	 */
	public byte[] getFileData(FileEntry fileEntry) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#format()
	 */
	public void format() {
		// TODO Auto-generated method stub

	}

	/**
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getLogicalDiskNumber()
	 */
	public int getLogicalDiskNumber() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getSuggestedFilename(java.lang.String)
	 */
	public String getSuggestedFilename(String filename) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getSuggestedFiletype(java.lang.String)
	 */
	public String getSuggestedFiletype(String filename) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getFiletypes()
	 */
	public String[] getFiletypes() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#needsAddress(java.lang.String)
	 */
	public boolean needsAddress(String filetype) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Answer with a list of file entries.
	 * @see com.webcodepro.applecommander.storage.DirectoryEntry#getFiles()
	 */
	public List getFiles() {
		List files = new ArrayList();
		for (int i=0; i<64; i++) {
			CpmFileEntry fileEntry = new CpmFileEntry(this, i*CpmFileEntry.ENTRY_LENGTH);
			if (!fileEntry.isEmpty()) {
				files.add(fileEntry);
			}
		}
		return files;
	}

	/**
	 * @see com.webcodepro.applecommander.storage.DirectoryEntry#createFile()
	 */
	public FileEntry createFile() throws DiskFullException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see com.webcodepro.applecommander.storage.DirectoryEntry#canCreateDirectories()
	 */
	public boolean canCreateDirectories() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @see com.webcodepro.applecommander.storage.DirectoryEntry#canCreateFile()
	 */
	public boolean canCreateFile() {
		// TODO Auto-generated method stub
		return false;
	}
	
	/**
	 * Read a CP/M block (1K in size).
	 * <p>
	 * Apparantly, data block #0 starts at track 3, sector 0.
	 * There should be 64 directory entries.
	 * Each block is 1024 bytes (16 128-byte sectors).
	 * One block should be the entire directory... 
	 */
	public byte[] readCpmBlock(int block) {
		byte[] data = new byte[1024];
		int track = 3 + (block / 4);
		int sector = block % 4;
		for (int i=0; i<4; i++) {
			System.arraycopy(readSector(track, sector+i), 
				0, data, i*SECTOR_SIZE, SECTOR_SIZE);
		}
		return data;
	}

}
