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

import java.util.Date;

/**
 * Represents the common Prodos entry behavior.
 * <p>
 * Date created: Oct 5, 2002 10:55:41 PM
 * @author: Rob Greene
 */
public class ProdosCommonEntry {
	private byte[] fileEntry;

	/**
	 * Constructor for ProdosCommonEntry.
	 */
	public ProdosCommonEntry(byte[] fileEntry) {
		super();
		this.fileEntry = fileEntry;
	}

	/**
	 * Get the fileEntry bytes.
	 */
	protected byte[] getFileEntry() {
		return fileEntry;
	}

	/**
	 * Get storage type.
	 */
	protected int getStorageType() {
		return AppleUtil.getUnsignedByte(getFileEntry()[0]) >> 4;
	}

	/**
	 * Indicates if this is a "seedling" file (only one data block).
	 */
	public boolean isSeedlingFile() {
		return getStorageType() == 0x01;
	}

	/**
	 * Indicates if this is a "sapling" file (2 to 256 data blocks).
	 */
	public boolean isSaplingFile() {
		return getStorageType() == 0x02;
	}

	/**
	 * Indicates if this is a "tree" file (257 to 32768 data blocks).
	 */
	public boolean isTreeFile() {
		return getStorageType() == 0x03;
	}

	/**
	 * Indicates if this is a subdirectory header entry.
	 */
	public boolean isSubdirectoryHeader() {
		return getStorageType() == 0x0e;
	}

	/**
	 * Indicates if this is a volume header entry.
	 */
	public boolean isVolumeHeader() {
		return getStorageType() == 0x0f;
	}

	/**
	 * Get the creation date.
	 */
	public Date getCreationDate() {
		return AppleUtil.getProdosDate(getFileEntry(), 0x18);
	}

	/**
	 * Get the version of ProDOS that created this file.
	 */
	public int getProdosVersion() {
		return AppleUtil.getUnsignedByte(getFileEntry()[0x1c]);
	}

	/**
	 * Get the minimum version of ProDOS which can access this file.
	 */
	public int getMinimumProdosVersion() {
		return AppleUtil.getUnsignedByte(getFileEntry()[0x1d]);
	}

	/**
	 * Get the access byte.
	 */
	protected byte getAccess() {
		return getFileEntry()[0x1e];
	}

	/**
	 * Indicates if this file may be destroyed.
	 */
	public boolean canDestroy() {
		return AppleUtil.isBitSet(getAccess(), 7);
	}

	/**
	 * Indicates if this file may be renamed.
	 */
	public boolean canRename() {
		return AppleUtil.isBitSet(getAccess(), 6);
	}

	/**
	 * Indicates if this file has changed since last backup.
	 */
	public boolean hasChanged() {
		return AppleUtil.isBitSet(getAccess(), 5);
	}

	/**
	 * Indicates if this file may be written.
	 */
	public boolean canWrite() {
		return AppleUtil.isBitSet(getAccess(), 1);
	}

	/**
	 * Indicates if this file may be read.
	 */
	public boolean canRead() {
		return AppleUtil.isBitSet(getAccess(), 0);
	}

}
