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

import com.webcodepro.applecommander.util.AppleUtil;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents a ProDOS file entry on disk.
 * <p>
 * Date created: Oct 5, 2002 6:01:15 PM
 * @author: Rob Greene
 */
public class ProdosFileEntry extends ProdosCommonEntry implements FileEntry {
	/**
	 * Constructor for ProdosFileEntry.
	 */
	public ProdosFileEntry(ProdosFormatDisk disk, int block, int offset) {
		super(disk, block, offset);
	}
	
	/**
	 * Return the name of this file.
	 * This handles normal files, deleted files, and AppleWorks files - which use
	 * the AUXTYPE attribute to indicate upper/lower-case in the filename.
	 */
	public String getFilename() {
		String fileName;
		if (isDeleted()) {
			fileName = AppleUtil.getString(readFileEntry(), 1, 15);
			StringBuffer buf = new StringBuffer();
			for (int i=0; i<fileName.length(); i++) {
				char ch = fileName.charAt(i);
				if (ch != 0) {
					buf.append(ch);
				} else {
					break;
				}
			}
			fileName = buf.toString();
		} else {
			fileName = AppleUtil.getProdosString(readFileEntry(), 0);
		}
		if (isAppleWorksFile()) {
			int auxtype = getAuxiliaryType();
			StringBuffer mixedCase = new StringBuffer(fileName);
			// the highest bit of the least significant byte is the first
			// character through the 2nd bit of the most significant byte
			// being the 15th character.  Bit is on indicates lowercase or
			// a space if a "." is present.
			for (int i=0; i<16 && i<fileName.length(); i++) {
				boolean lowerCase;
				if (i < 8) {
					lowerCase = AppleUtil.isBitSet((byte)auxtype, 7-i);
				} else {
					lowerCase = AppleUtil.isBitSet((byte)(auxtype >> 8), 7-(i%8));
				}
				if (lowerCase) {
					char ch = mixedCase.charAt(i);
					if (ch == '.') {
						mixedCase.setCharAt(i, ' ');
					} else {
						mixedCase.setCharAt(i, Character.toLowerCase(ch));
					}
				}
			}
			fileName = mixedCase.toString();
		}
		return fileName;
	}

	/**
	 * Return the maximum filename length.
	 */
	public int getMaximumFilenameLength() {
		return 15;
	}

	/**
	 * Set the name of this file.
	 */
	public void setFilename(String filename) {
		byte[] fileEntry = readFileEntry();
		if (isDeleted()) {
			AppleUtil.setString(fileEntry, 1, filename.toUpperCase(), 15);
		} else {
			AppleUtil.setProdosString(fileEntry, 0, filename.toUpperCase(), 15);
		}
		if (isAppleWorksFile()) {
			byte lowByte = 0;
			byte highByte = 0;
			for (int i=0; i<filename.length(); i++) {
				if (Character.isLowerCase(filename.charAt(i))) {
					if (i < 8) {
						lowByte = AppleUtil.setBit(lowByte, 7-i);
					} else if (i < 16) {
						highByte = AppleUtil.setBit(highByte, 7-(i%8));
					}
				}
			}
			setAuxiliaryType(fileEntry, lowByte, highByte);
		}
		writeFileEntry(fileEntry);
	}

	/**
	 * Return the filetype of this file.  This will be three characters,
	 * according to ProDOS - a "$xx" if unknown.
	 */
	public String getFiletype() {
		int filetype = AppleUtil.getUnsignedByte(readFileEntry()[0x10]);
		return getDisk().getFiletype(filetype);
	}

	/**
	 * Set the filetype.
	 */
	public void setFiletype(String filetype) {
		byte[] entry = readFileEntry();
		entry[0x10] = getDisk().getFiletype(filetype);
		writeFileEntry(entry);
	}
	
	/**
	 * Indicate if this is an AppleWorks file.
	 * Intended to force upper/lowercase into the filename.
	 */
	public boolean isAppleWorksFile()	{
		int filetype = AppleUtil.getUnsignedByte(readFileEntry()[0x10]);
		return (filetype == 0x19 || filetype == 0x1a || filetype == 0x1b);
	}

	/**
	 * Get the key pointer.  This is either the data block (seedling),
	 * index block (sapling), or master index block (tree).
	 */
	public int getKeyPointer() {
		return AppleUtil.getWordValue(readFileEntry(), 0x11);
	}
	
	/**
	 * Set the key pointer.  This is either the data block (seedling),
	 * index block (sapling), or master index block (tree).
	 */
	public void setKeyPointer(int keyPointer) {
		byte[] entry = readFileEntry();
		AppleUtil.setWordValue(entry, 0x11, keyPointer);
		writeFileEntry(entry);
	}

	/**
	 * Get the number of blocks used.
	 */
	public int getBlocksUsed() {
		return AppleUtil.getWordValue(readFileEntry(), 0x13);
	}
	
	/**
	 * Set the number of blocks used.
	 */
	public void setBlocksUsed(int blocksUsed) {
		byte[] entry = readFileEntry();
		AppleUtil.setWordValue(entry, 0x13, blocksUsed);
		writeFileEntry(entry);
	}

	/**
	 * Get the EOF position.  This can indicate the length of a file.
	 */
	public int getEofPosition() {
		return AppleUtil.get3ByteValue(readFileEntry(), 0x15);
	}
	
	/**
	 * Set the EOF position.
	 */
	public void setEofPosition(int eofPosition) {
		byte[] entry = readFileEntry();
		AppleUtil.set3ByteValue(entry, 0x15, eofPosition);
		writeFileEntry(entry);
	}

	/**
	 * Get the auxiliary type for this file.
	 * TXT - random access record length.
	 * BIN - load address for binary image.
	 * BAS - load address for program image.
	 * VAR - address of compressed variables image.
	 * SYS - load address for system program (usually 0x2000).
	 * AWP/ADB/ASP - upper/lowercase flags
	 */
	public int getAuxiliaryType() {
		return AppleUtil.getWordValue(readFileEntry(), 0x1f);
	}
	
	/**
	 * Set the auxiliary type for this file.
	 */
	public void setAuxiliaryType(byte[] entry, byte low, byte high) {
		entry[0x1f] = low;
		entry[0x20] = high;
	}
	
	/**
	 * Set the auxiliary type for this file.
	 */
	public void setAuxiliaryType(byte[] entry, int auxiliaryType) {
		setAuxiliaryType(entry, (byte)(auxiliaryType%256), 
			(byte)(auxiliaryType/256));
	}

	/**
	 * Get the last modification date.
	 */
	public Date getLastModificationDate() {
		return AppleUtil.getProdosDate(readFileEntry(), 0x21);
	}
	
	/**
	 * Set the last modification date.
	 */
	public void setLastModificationDate(Date date) {
		byte[] entry = readFileEntry();
		AppleUtil.setProdosDate(entry, 0x21, date);
		writeFileEntry(entry);
	}

	/**
	 * Get the block number of the key block for the directory which describes this file.
	 */
	public int getHeaderPointer() {
		return AppleUtil.getWordValue(readFileEntry(), 0x25);
	}

	/**
	 * Identify if this file is locked.
	 */
	public boolean isLocked() {
		return !canDestroy() && !canRename() && !canWrite();
	}

	/**
	 * Set the lock indicator.
	 */
	public void setLocked(boolean lock) {
		setCanDestroy(lock);
		setCanRename(lock);
		setCanWrite(lock);
	}

	/**
	 * Compute the size of this file (in bytes).
	 */
	public int getSize() {
		return getEofPosition();
	}

	/**
	 * Identify if this is a directory file.
	 */
	public boolean isDirectory() {
		return getStorageType() == 0x0d;
	}

	/**
	 * Identify if this file has been deleted.
	 */
	public boolean isDeleted() {
		return getStorageType() == 0;
	}

	/**
	 * Delete the file.
	 */
	public void delete() {
		getDisk().freeBlocks(this);
		setStorageType(0);
	}

	/**
	 * Get the standard file column header information.
	 * This default implementation is intended only for standard mode.
	 * displayMode is specified in FormattedDisk.
	 */
	public List getFileColumnData(int displayMode) {
		NumberFormat numberFormat = NumberFormat.getNumberInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

		List list = new ArrayList();
		switch (displayMode) {
			case FormattedDisk.FILE_DISPLAY_NATIVE:
				list.add(isLocked() ? "*" : " ");
				list.add(getFilename());
				list.add(getFiletype());
				numberFormat.setMinimumIntegerDigits(3);
				list.add(numberFormat.format(getBlocksUsed()));
				list.add(getLastModificationDate() == null ? "<NO DATE> " : 
					dateFormat.format(getLastModificationDate()));
				list.add(getCreationDate() == null ? "<NO DATE> " : 
					dateFormat.format(getCreationDate()));
				numberFormat.setMinimumIntegerDigits(1);
				list.add(numberFormat.format(getEofPosition()));
				if ("TXT".equals(getFiletype()) && getAuxiliaryType() > 0) {
					numberFormat.setMinimumIntegerDigits(1);
					list.add("L=" + numberFormat.format(getAuxiliaryType()).trim());
				} else if (("BIN".equals(getFiletype()) || "BAS".equals(getFiletype())
						|| "VAR".equals(getFiletype()) || "SYS".equals(getFiletype()))
						&&  getAuxiliaryType() > 0) {
					list.add("A=$" + AppleUtil.getFormattedWord(getAuxiliaryType()));
				} else {
					list.add("");
				}
				break;
			case FormattedDisk.FILE_DISPLAY_DETAIL:
				list.add(isLocked() ? "*" : " ");
				list.add(getFilename());
				list.add(isDeleted() ? "Deleted" : "");
				String permissions = "";
				if (canDestroy()) permissions+= "Destroy ";
				if (canRead()) permissions+= "Read ";
				if (canRename()) permissions+= "Rename ";
				if (canWrite()) permissions+= "Write ";
				list.add(permissions);
				list.add(getFiletype());
				list.add(isDirectory() ? "Directory" : "");
				numberFormat.setMinimumIntegerDigits(3);
				list.add(numberFormat.format(getBlocksUsed()));
				list.add(getLastModificationDate() == null ? "<NO DATE> " : 
					dateFormat.format(getLastModificationDate()));
				list.add(getCreationDate() == null ? "<NO DATE> " : 
					dateFormat.format(getCreationDate()));
				numberFormat.setMinimumIntegerDigits(1);
				list.add(numberFormat.format(getEofPosition()));
				if ("TXT".equals(getFiletype()) && getAuxiliaryType() > 0) {
					numberFormat.setMinimumIntegerDigits(1);
					list.add("L=" + numberFormat.format(getAuxiliaryType()).trim());
				} else if (("BIN".equals(getFiletype()) || "BAS".equals(getFiletype())
						|| "VAR".equals(getFiletype()) || "SYS".equals(getFiletype()))
						&&  getAuxiliaryType() > 0) {
					list.add("A=$" + AppleUtil.getFormattedWord(getAuxiliaryType()));
				} else {
					list.add("$" + AppleUtil.getFormattedWord(getAuxiliaryType()));
				}
				list.add(AppleUtil.getFormattedWord(getHeaderPointer()));
				list.add(AppleUtil.getFormattedWord(getKeyPointer()));
				list.add(isSaplingFile() ? "Sapling" : isSeedlingFile() ? "Seedling" : 
					isTreeFile() ? "Tree" : "Unknown");
				list.add(hasChanged() ? "Changed" : "");
				numberFormat.setMinimumIntegerDigits(1);
				list.add(numberFormat.format(getMinimumProdosVersion()));
				list.add(numberFormat.format(getProdosVersion()));
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
	 * Get file data.  This handles any operating-system specific issues.
	 * Currently, the disk itself handles this.
	 */
	public byte[] getFileData() {
		return getDisk().getFileData(this);
	}

	/**
	 * Set the file data.  This is essentially the save operation.
	 * Specifically, if the filetype is binary, the length and
	 * address need to be set.  If the filetype is applesoft or
	 * integer basic, the start address needs to be set.
	 */
	public void setFileData(byte[] data) throws DiskFullException {
		getDisk().setFileData(this, data);
	}

	/**
	 * Get the suggested FileFilter.  This appears to be operating system
	 * specific, so each operating system needs to implement some manner
	 * of guessing the appropriate filter.
	 */
	public FileFilter getSuggestedFilter() {
		if ("TXT".equals(getFiletype()) || "SRC".equals(getFiletype())) {
			return new TextFileFilter();
		} else if ("AWP".equals(getFiletype())) {
			return new AppleWorksWordProcessorFileFilter();
		} else if ("ADB".equals(getFiletype())) {
			return new AppleWorksDataBaseFileFilter();
		} else if ("ASP".equals(getFiletype())) {
			return new AppleWorksSpreadSheetFileFilter();
		} else if ("BAS".equals(getFiletype())) {
			return new ApplesoftFileFilter();
		} else if ("INT".equals(getFiletype())) {	// supposedly not available in ProDOS, however
			return new IntegerBasicFileFilter();
		} else if ("PNT".equals(getFiletype())) {
			if (getAuxiliaryType() == 0x0001) {
				GraphicsFileFilter filter = new GraphicsFileFilter();
				filter.setMode(GraphicsFileFilter.MODE_SHR);
				return filter;
			}
		} else if ("PIC".equals(getFiletype())) {
			if (getAuxiliaryType() == 0x0000) {
				GraphicsFileFilter filter = new GraphicsFileFilter();
				filter.setMode(GraphicsFileFilter.MODE_SHR);
				return filter;
			}
		} else if ("BIN".equals(getFiletype())) {
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
	 * Indicates if this filetype requires an address component.
	 * Note that the FormattedDisk also has this method - normally,
	 * this will defer to the method on FormattedDisk, as it will be
	 * more generic.
	 */
	public boolean needsAddress() {
		return getDisk().needsAddress(getFiletype());
	}
	
	/**
	 * Set the address that this file loads at.
	 */
	public void setAddress(int address) {
		byte[] fileEntry = readFileEntry();
		setAuxiliaryType(fileEntry, address);
		writeFileEntry(fileEntry);
	}
}
