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

import java.util.Date;

import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.util.AppleUtil;

/**
 * Represents the common Prodos entry behavior.
 * <p>
 * Date created: Oct 5, 2002 10:55:41 PM
 * @author Rob Greene
 */
public class ProdosCommonEntry {
	/**
	 * The standard ProDOS file entry length.
	 */
	public static final int ENTRY_LENGTH = 0x27;
	/**
	 * Reference to the disk this FileEntry is attached to.
	 */
	private ProdosFormatDisk disk;
	/**
	 * The block number this FileEntry is stored in.
	 */
	private int block;
	/**
	 * The offset into the block that the FileEntry is at.
	 */
	private int offset;
	
	/**
	 * Constructor for ProdosCommonEntry.
	 */
	public ProdosCommonEntry(ProdosFormatDisk disk, int block, int offset) {
		super();
		this.disk = disk;
		this.block = block;
		this.offset = offset;
	}
	
	/**
	 * Get the block in which the FileEntry resides.
	 */
	protected int getFileEntryBlock() {
		return block;
	}
	
	/**
	 * Get the ProdosFormatDisk that this FileEntry is attached to.
	 */
	protected ProdosFormatDisk getDisk() {
		return disk;
	}
	
	/**
	 * Read the fileEntry bytes from the disk image.
	 */
	protected byte[] readFileEntry() {
		byte[] data = disk.readBlock(block);
		byte[] entry = new byte[ENTRY_LENGTH];
		System.arraycopy(data, offset, entry, 0, ENTRY_LENGTH);
		return entry;
	}

	/**
	 * Indicates if this entry is empty - filled with $00.
	 */
	public boolean isEmpty() {
		byte[] entry = readFileEntry();
		for (int i=0; i<entry.length; i++) {
			if (entry[i] != 0x00) return false;
		}
		return true;
	}
	
	/**
	 * Write the fileEntry data to the disk image.
	 */
	protected void writeFileEntry(byte[] entry) {
		byte[] data = disk.readBlock(block);
		System.arraycopy(entry, 0, data, offset, ENTRY_LENGTH);
		disk.writeBlock(block, data);
	}

	/**
	 * Get storage type.
	 */
	protected int getStorageType() {
		return AppleUtil.getUnsignedByte(readFileEntry()[0]) >> 4;
	}

	/**
	 * Set the storage type.
	 */
	public void setStorageType(int storageType) {
		byte[] data = readFileEntry();
		int value = AppleUtil.getUnsignedByte(data[0]);
		value = (value & 0x0f) | ((storageType & 0xf) << 4);
		data[0] = (byte) value;
		writeFileEntry(data);
	}

	/**
	 * Indicates if this is a "seedling" file (only one data block).
	 */
	public boolean isSeedlingFile() {
		return getStorageType() == 0x01;
	}

	/**
	 * Sets the storage type to a "seedling" file (only one data block).
	 */
	public void setSeedlingFile() {
		setStorageType(0x01);
	}

	/**
	 * Indicates if this is a "sapling" file (2 to 256 data blocks).
	 */
	public boolean isSaplingFile() {
		return getStorageType() == 0x02;
	}

	/**
	 * Sets the storage type to a "sapling" file (2 to 256 data blocks).
	 */
	public void setSaplingFile() {
		setStorageType(0x02);
	}

	/**
	 * Indicates if this is a "tree" file (257 to 32768 data blocks).
	 */
	public boolean isTreeFile() {
		return getStorageType() == 0x03;
	}

	/**
	 * Sets the storage type to a "tree" file (257 to 32768 data blocks).
	 */
	public void setTreeFile() {
		setStorageType(0x03);
	}

	/**
	 * Indicates if this is a subdirectory entry.
	 */
	public boolean isSubdirectory() {
		return getStorageType() == 0x0d;
	}

	/**
	 * Sets the storage type to a subdirectory entry.
	 */
	public void setSubdirectory() {
		setStorageType(0x0d);
	}

	/**
	 * Indicates if this is a subdirectory header entry.
	 */
	public boolean isSubdirectoryHeader() {
		return getStorageType() == 0x0e;
	}

	/**
	 * Sets the storage type to a subdirectory header entry.
	 */
	public void setSubdirectoryHeader() {
		setStorageType(0x0e);
	}

	/**
	 * Indicates if this is a volume header entry.
	 */
	public boolean isVolumeHeader() {
		return getStorageType() == 0x0f;
	}

	/**
	 * Sets the storage type to a volume header entry.
	 */
	public void setVolumeHeader() {
		setStorageType(0x0f);
	}

	/**
	 * Get the creation date.
	 */
	public Date getCreationDate() {
		return AppleUtil.getProdosDate(readFileEntry(), 0x18);
	}

	/**
	 * Set the creation date.
	 */
	public void setCreationDate(Date date) {
		byte[] data = readFileEntry();
		AppleUtil.setProdosDate(data, 0x18, date);
		writeFileEntry(data);
	}

	/**
	 * Get the version of ProDOS that created this file.
	 */
	public int getProdosVersion() {
		return AppleUtil.getUnsignedByte(readFileEntry()[0x1c]);
	}

	/**
	 * Set the version of ProDOS that created this file.
	 */
	public void setProdosVersion(int version) {
		byte[] data = readFileEntry();
		data[0x1c] = (byte) version;
		writeFileEntry(data);
	}

	/**
	 * Get the minimum version of ProDOS which can access this file.
	 */
	public int getMinimumProdosVersion() {
		return AppleUtil.getUnsignedByte(readFileEntry()[0x1d]);
	}

	/**
	 * Set the minimum version of ProDOS which can access this file.
	 */
	public void setMinimumProdosVersion(int version) {
		byte[] data = readFileEntry();
		data[0x1d] = (byte) version;
		writeFileEntry(data);
	}

	/**
	 * Get the access byte.
	 */
	protected byte getAccess() {
		return readFileEntry()[0x1e];
	}

	/**
	 * Set the access byte.
	 */
	protected void setAccess(int bit, boolean set) {
		byte[] data = readFileEntry();
		byte value = data[0x1e];
		if (set) {
			value = AppleUtil.setBit(value, bit);
		} else {
			value = AppleUtil.clearBit(value, bit);
		}
		data[0x1e] = value;
		writeFileEntry(data);
	}

	/**
	 * Indicates if this file may be destroyed.
	 */
	public boolean canDestroy() {
		return AppleUtil.isBitSet(getAccess(), 7);
	}

	/**
	 * Set if this file may be destroyed.
	 */
	public void setCanDestroy(boolean destroy) {
		setAccess(7, destroy);
	}

	/**
	 * Indicates if this file may be renamed.
	 */
	public boolean canRename() {
		return AppleUtil.isBitSet(getAccess(), 6);
	}

	/**
	 * Set if this file may be renamed.
	 */
	public void setCanRename(boolean rename) {
		setAccess(6, rename);
	}

	/**
	 * Indicates if this file has changed since last backup.
	 */
	public boolean hasChanged() {
		return AppleUtil.isBitSet(getAccess(), 5);
	}

	/**
	 * Set if this file has changed since last backup.
	 */
	public void setHasChanged(boolean changed) {
		setAccess(5, changed);
	}

	/**
	 * Indicates if this file may be written.
	 */
	public boolean canWrite() {
		return AppleUtil.isBitSet(getAccess(), 1);
	}

	/**
	 * Set if this file may be written.
	 */
	public void setCanWrite(boolean write) {
		setAccess(1, write);
	}

	/**
	 * Indicates if this file may be read.
	 */
	public boolean canRead() {
		return AppleUtil.isBitSet(getAccess(), 0);
	}

	/**
	 * Set if this file may be read.
	 */
	public void setCanRead(boolean read) {
		setAccess(0, read);
	}

	/**
	 * Get the FormattedDisk associated with this FileEntry.
	 * This is useful to interfaces that need to retrieve the associated
	 * disk.
	 */
	public FormattedDisk getFormattedDisk() {
		return disk;
	}
}
