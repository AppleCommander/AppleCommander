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
package com.webcodepro.applecommander.storage.os.prodos;

import com.webcodepro.applecommander.util.AppleUtil;

/**
 * Provides common subdirectory attributes.
 * <p>
 * Date created: Oct 5, 2002 11:17:57 PM
 * @author Rob Greene
 */
public class ProdosSubdirectoryHeader extends ProdosCommonDirectoryHeader {
	private ProdosDirectoryEntry directoryEntry;

	/**
	 * Constructor for ProdosSubdirectoryHeader.
	 */
	public ProdosSubdirectoryHeader(ProdosFormatDisk disk, int block) {
		super(disk, block);
	}

	/**
	 * Return the name of this subdirectory.
	 */
	public String getSubdirectoryName() {
		return AppleUtil.getProdosString(readFileEntry(), 0);
	}
	
	/**
	 * Return the block number of the parent directory which contains the
	 * file entry for this subdirectory.
	 */
	public int getParentPointer() {
		return AppleUtil.getWordValue(readFileEntry(), 0x23);
	}
	
	/**
	 * Set the block number of the parent directory which contains the
	 * file entry for this subdirectory.
	 */
	public void setParentPointer(int block) {
		byte[] data = readFileEntry();
		AppleUtil.setWordValue(data, 0x23, block);
		writeFileEntry(data);
	}
	
	/**
	 * Return the number of the file entry within the parent block.
	 */
	public int getParentEntry() {
		return AppleUtil.getUnsignedByte(readFileEntry()[0x25]);
	}
	
	/**
	 * Sets the number of the file entry within the parent block.
	 */
	public void setParentEntry(int entryNum) {
		byte[] data = readFileEntry();
		data[0x25] = (byte) entryNum;
		writeFileEntry(data);
	}
	
	/**
	 * Return the length of the parent entry.
	 */
	public int getParentEntryLength() {
		return AppleUtil.getWordValue(readFileEntry(), 0x26);
	}
	
	/**
	 * Sets the number of the file entry within the parent block.
	 */
	public void setParentEntryLength(int length) {
		byte[] data = readFileEntry();
		data[0x26] = (byte) length;
		writeFileEntry(data);
	}
	
	/**
	 * Set the related ProDOS directory entry.
	 */
	public void setProdosDirectoryEntry(ProdosDirectoryEntry directoryEntry) {
		this.directoryEntry = directoryEntry;
	}
	
	/**
	 * Get the related ProDOS directory entry.
	 */
	public ProdosDirectoryEntry getProdosDirectoryEntry() {
		return directoryEntry;
	}

	/**
	 * Set up some housekeeping bits
	 */
	public void setHousekeeping() {
		byte[] data = readFileEntry();
		data[0x00] = (byte) (0xe0 | (data[0x00] & 0x0f)); // Subdirectories have the high nibble set to 0x0e
		data[0x10] = 0x75; // Reserved - must be $75
		data[0x1f] = (byte) ENTRY_LENGTH;
		data[0x20] = 0x0d;
		AppleUtil.setWordValue(data, 0x21, 0); // Set file count to zero
		writeFileEntry(data);
	}
}
