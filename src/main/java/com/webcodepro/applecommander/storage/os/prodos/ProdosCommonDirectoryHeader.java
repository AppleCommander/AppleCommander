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
 * Provide common directory header attributes.
 * <p>
 * Date created: Oct 5, 2002 11:17:00 PM
 * @author Rob Greene
 */
public class ProdosCommonDirectoryHeader extends ProdosCommonEntry {

	/**
	 * Constructor for ProdosCommonDirectoryHeader.
	 */
	public ProdosCommonDirectoryHeader(ProdosFormatDisk disk, int block) {
		super(disk, block, 4);	// directory entries are always offset 4, right?
	}

	/**
	 * Get the length of each entry.  Expected to be 0x27.
	 */
	public int getEntryLength() {
		return AppleUtil.getUnsignedByte(readFileEntry()[0x1f]);
	}
	
	/**
	 * Set the length of each entry.
	 */
	public void setEntryLength() {
		byte[] data = readFileEntry();
		data[0x1f] = (byte) ENTRY_LENGTH;
		writeFileEntry(data);
	}
	
	/**
	 * Get the number of entries per block. Expected to be 0x0d.
	 */
	public int getEntriesPerBlock() {
		return AppleUtil.getUnsignedByte(readFileEntry()[0x20]);
	}

	/**
	 * Set the number of entries per block.
	 */
	public void setEntriesPerBlock() {
		byte[] data = readFileEntry();
		data[0x20] = 0x0d;
		writeFileEntry(data);
	}
	
	/**
	 * Get the number of active entries in the directory.
	 */
	public int getFileCount() {
		return AppleUtil.getWordValue(readFileEntry(), 0x21);
	}

	/**
	 * Set the number of active entries in the directory.
	 */
	public void setFileCount(int fileCount) {
		byte[] data = readFileEntry();
		AppleUtil.setWordValue(data, 0x21, fileCount);
		writeFileEntry(data);
	}
	
	/**
	 * Increment the number of active entries by one.
	 */
	public void incrementFileCount() {
		byte[] data = readFileEntry();
		data[0x21]++;
		writeFileEntry(data);
	}
	
	/**
	 * Decrement the number of active entries by one.
	 */
	public void decrementFileCount() {
		byte[] data = readFileEntry();
		if (data[0x21] != 0) data[0x21]--;
		writeFileEntry(data);
	}
	
	/**
	 * Get the block number of the bit map.
	 */
	public int getBitMapPointer() {
		return AppleUtil.getWordValue(readFileEntry(), 0x23);
	}

	/**
	 * Set the block number of the bit map.
	 */
	public void setBitMapPointer(int blockNumber) {
		byte[] data = readFileEntry();
		AppleUtil.setWordValue(data, 0x23, blockNumber);
		writeFileEntry(data);
	}
	
	/**
	 * Get the total number of blocks on this volume (only valid for volume directory block).
	 */
	public int getTotalBlocks() {
		return AppleUtil.getWordValue(readFileEntry(), 0x25);
	}

	/**
	 * Set the total number of blocks on this volume (only valid for volume directory block).
	 */
	public void setTotalBlocks(int totalBlocks) {
		byte[] data = readFileEntry();
		AppleUtil.setWordValue(data, 0x25, totalBlocks);
		writeFileEntry(data);
	}
}
