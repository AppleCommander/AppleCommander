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
package com.webcodepro.applecommander.storage.os.prodos;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.StorageBundle;
import com.webcodepro.applecommander.storage.filters.AppleWorksDataBaseFileFilter;
import com.webcodepro.applecommander.storage.filters.AppleWorksSpreadSheetFileFilter;
import com.webcodepro.applecommander.storage.filters.AppleWorksWordProcessorFileFilter;
import com.webcodepro.applecommander.storage.filters.ApplesoftFileFilter;
import com.webcodepro.applecommander.storage.filters.AssemblySourceFileFilter;
import com.webcodepro.applecommander.storage.filters.BinaryFileFilter;
import com.webcodepro.applecommander.storage.filters.BusinessBASICFileFilter;
import com.webcodepro.applecommander.storage.filters.GraphicsFileFilter;
import com.webcodepro.applecommander.storage.filters.IntegerBasicFileFilter;
import com.webcodepro.applecommander.storage.filters.TextFileFilter;
import com.webcodepro.applecommander.util.AppleUtil;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Represents a ProDOS file entry on disk.
 * <p>
 * Date created: Oct 5, 2002 6:01:15 PM
 * @author Rob Greene
 */
public class ProdosFileEntry extends ProdosCommonEntry implements FileEntry {
	private TextBundle textBundle = StorageBundle.getInstance();
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
	 * Copy GEOS-specific metadata to the directory entry verbatim:
	 * Bytes $00-$10 ($11 bytes)
	 * Bytes $18-$1d ($06 bytes)
	 * Bytes $21-$24 ($04 bytes)
	 */
	public void setGEOSMeta(byte[] metaData) {
		byte[] fileEntry = readFileEntry();
		// GEOS metadata lives at $180 offset from the beginning of the first block.
		// Copy that to the file entry, skipping the bytes that are locally created
		// (i.e. pointers, etc.)
		System.arraycopy(metaData,0x180+0x00,fileEntry,0x00,0x11);
		System.arraycopy(metaData,0x180+0x18,fileEntry,0x18,0x06);
		System.arraycopy(metaData,0x180+0x21,fileEntry,0x21,0x04);
		writeFileEntry(fileEntry);
	}

	/**
	 * Return the filetype of this file.  This will be three characters,
	 * according to ProDOS - a "$xx" if unknown.
	 */
	public String getFiletype() {
		int filetype = getFiletypeByte();
		return getDisk().getFiletype(filetype);
	}
	
	public int getFiletypeByte() {
		return AppleUtil.getUnsignedByte(readFileEntry()[0x10]);
	}

	/**
	 * Set the filetype based on a string value.
	 */
	public void setFiletype(String filetype) {
		byte[] entry = readFileEntry();
		entry[0x10] = getDisk().getFiletype(filetype);
		writeFileEntry(entry);
	}
	
	/**
	 * Set the filetype based on a long - thunk into a byte
	 */
	public void setFiletype(long fileType) {
		byte[] entry = readFileEntry();
		entry[0x10] = (byte)fileType;
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
	 * Indicate if this is a GEOS file.
	 */
	public boolean isGEOSFile()	{
		int filetype = AppleUtil.getUnsignedByte(readFileEntry()[0x10]);
		return (filetype >= 0x80 && filetype <= 0x8f);
	}

	/**
	 * Indicate if this is a GEOS file.
	 */
	public boolean isForkedFile()	{
		int storageType = AppleUtil.getUnsignedByte(readFileEntry()[0x00]);
		return ((storageType & 0x50) == 0x50);
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
	public void setAuxiliaryType(int auxiliaryType) {
		byte[] entry = readFileEntry();
		setAuxiliaryType(entry, auxiliaryType);
		writeFileEntry(entry);
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
	 * Set the block number of the  block for the directory which describes this file.
	 */
	public void setHeaderPointer(int headerPointer) {
		byte[] entry = readFileEntry();
		AppleUtil.setWordValue(entry, 0x25, headerPointer);
		writeFileEntry(entry);
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
		setCanDestroy(!lock);
		setCanRename(!lock);
		setCanWrite(!lock);
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

		//decrement file count in header block
		int headerBlock = getHeaderPointer();
		byte[] data = getDisk().readBlock(headerBlock);
		int fileCount = AppleUtil.getWordValue(data, 0x25);
		if (fileCount != 0) fileCount--;
		AppleUtil.setWordValue(data, 0x25, fileCount);
		getDisk().writeBlock(headerBlock, data);

		//clear storage type and name length
		data = readFileEntry();
		data[0] = 0;
		writeFileEntry(data);
	}

	/**
	 * Get the standard file column header information.
	 * This default implementation is intended only for standard mode.
	 * displayMode is specified in FormattedDisk.
	 */
	public List getFileColumnData(int displayMode) {
		NumberFormat numberFormat = NumberFormat.getNumberInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				textBundle.get("DateFormat")); //$NON-NLS-1$

		List list = new ArrayList();
		switch (displayMode) {
			case FormattedDisk.FILE_DISPLAY_NATIVE:
				list.add(isLocked() ? "*" : " "); //$NON-NLS-1$ //$NON-NLS-2$
				list.add(getFilename());
				list.add(getFiletype());
				numberFormat.setMinimumIntegerDigits(3);
				list.add(numberFormat.format(getBlocksUsed()));
				list.add(getLastModificationDate() == null ? 
					textBundle.get("ProdosFileEntry.NullDate") :  //$NON-NLS-1$
					dateFormat.format(getLastModificationDate()));
				list.add(getCreationDate() == null ? 
					textBundle.get("ProdosFileEntry.NullDate") :  //$NON-NLS-1$
					dateFormat.format(getCreationDate()));
				numberFormat.setMinimumIntegerDigits(1);
				list.add(numberFormat.format(getEofPosition()));
				if ("TXT".equals(getFiletype()) && getAuxiliaryType() > 0) { //$NON-NLS-1$
					numberFormat.setMinimumIntegerDigits(1);
					list.add("L=" + numberFormat.format(getAuxiliaryType()).trim()); //$NON-NLS-1$
				} else if (("BIN".equals(getFiletype()) || "BAS".equals(getFiletype()) //$NON-NLS-1$ //$NON-NLS-2$
						|| "VAR".equals(getFiletype()) || "SYS".equals(getFiletype())) //$NON-NLS-1$ //$NON-NLS-2$
						&&  getAuxiliaryType() > 0) {
					list.add("A=$" + AppleUtil.getFormattedWord(getAuxiliaryType())); //$NON-NLS-1$
				} else {
					list.add(""); //$NON-NLS-1$
				}
				break;
			case FormattedDisk.FILE_DISPLAY_DETAIL:
				list.add(isLocked() ? "*" : " "); //$NON-NLS-1$ //$NON-NLS-2$
				list.add(getFilename());
				list.add(isDeleted() ? textBundle.get("Deleted") : "");  //$NON-NLS-1$//$NON-NLS-2$
				String permissions = ""; //$NON-NLS-1$
				if (canDestroy()) permissions+= textBundle.get("Destroy"); //$NON-NLS-1$
				if (canRead()) permissions+= textBundle.get("Read"); //$NON-NLS-1$
				if (canRename()) permissions+= textBundle.get("Rename"); //$NON-NLS-1$
				if (canWrite()) permissions+= textBundle.get("Write"); //$NON-NLS-1$
				list.add(permissions);
				list.add(getFiletype());
				list.add(isDirectory() ? textBundle.get("ProdosFileEntry.Directory") : "");  //$NON-NLS-1$//$NON-NLS-2$
				numberFormat.setMinimumIntegerDigits(3);
				list.add(numberFormat.format(getBlocksUsed()));
				list.add(getLastModificationDate() == null ? 
					textBundle.get("ProdosFileEntry.NullDate") :  //$NON-NLS-1$
					dateFormat.format(getLastModificationDate()));
				list.add(getCreationDate() == null ? textBundle.get("ProdosFileEntry.NullDate") :  //$NON-NLS-1$
					dateFormat.format(getCreationDate()));
				numberFormat.setMinimumIntegerDigits(1);
				list.add(numberFormat.format(getEofPosition()));
				if ("TXT".equals(getFiletype()) && getAuxiliaryType() > 0) { //$NON-NLS-1$
					numberFormat.setMinimumIntegerDigits(1);
					list.add("L=" + numberFormat.format(getAuxiliaryType()).trim()); //$NON-NLS-1$
				} else if (("BIN".equals(getFiletype()) || "BAS".equals(getFiletype()) //$NON-NLS-1$ //$NON-NLS-2$
						|| "VAR".equals(getFiletype()) || "SYS".equals(getFiletype())) //$NON-NLS-1$ //$NON-NLS-2$
						&&  getAuxiliaryType() > 0) {
					list.add("A=$" + AppleUtil.getFormattedWord(getAuxiliaryType())); //$NON-NLS-1$
				} else {
					list.add("$" + AppleUtil.getFormattedWord(getAuxiliaryType())); //$NON-NLS-1$
				}
				list.add(AppleUtil.getFormattedWord(getHeaderPointer()));
				list.add(AppleUtil.getFormattedWord(getKeyPointer()));
				list.add(isSaplingFile() ? textBundle.get("ProdosFileEntry.Sapling") :  //$NON-NLS-1$
					isSeedlingFile() ? textBundle.get("ProdosFileEntry.Seedling") :  //$NON-NLS-1$
					isTreeFile() ? textBundle.get("ProdosFileEntry.Tree") :  //$NON-NLS-1$
					textBundle.format("ProdosFileEntry.UnknownFileType", getFileTypeString())); //$NON-NLS-1$
				list.add(hasChanged() ? 
					textBundle.get("ProdosFileEntry.Changed") : "");  //$NON-NLS-1$//$NON-NLS-2$
				numberFormat.setMinimumIntegerDigits(1);
				list.add(numberFormat.format(getMinimumProdosVersion()));
				list.add(numberFormat.format(getProdosVersion()));
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
	 * Return the ProDOS file type as a hex string.
	 */
	public String getFileTypeString() {
		return "$" + AppleUtil.getFormattedByte(getStorageType()); //$NON-NLS-1$
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
	 * Set the file data, with the expectation that both data and resource forks
	 * are present (storage type $05).  See:
	 * http://www.1000bit.it/support/manuali/apple/technotes/pdos/tn.pdos.25.html
	 */
	public void setFileData(byte[] dataFork, byte[] resourceFork) throws DiskFullException {
		getDisk().setFileData(this, dataFork, resourceFork);
	}

	/**
	 * Get the suggested FileFilter.  This appears to be operating system
	 * specific, so each operating system needs to implement some manner
	 * of guessing the appropriate filter.
	 */
	public FileFilter getSuggestedFilter() {
		int filetype = getFiletypeByte();
		int auxtype = getAuxiliaryType();
		int filesize = getSize();
		
		switch (filetype) {
		case 0x04:		// TXT
			if (getFilename().endsWith(".S")) { //$NON-NLS-1$
				return new AssemblySourceFileFilter();			
			}
			return new TextFileFilter();
		case 0x09:		// BA3
			return new BusinessBASICFileFilter();
		case 0xb0:		// SRC
			return new TextFileFilter();
		case 0x19:		// ADB
			return new AppleWorksDataBaseFileFilter();
		case 0x1a:		// AWP
			return new AppleWorksWordProcessorFileFilter();
		case 0x1b:		// ASP
			return new AppleWorksSpreadSheetFileFilter();
		case 0xfc:		// BAS
			return new ApplesoftFileFilter();
		case 0xfa:		// INT
			return new IntegerBasicFileFilter();
		case 0xc0:		// PNT
			if (auxtype == 0x0001) {
				GraphicsFileFilter filter = new GraphicsFileFilter();
				filter.setMode(GraphicsFileFilter.MODE_SHR_16);
				return filter;
			}
			break;
		case 0xc1: 	// PIC
			// AUX TYPE $0002 is sometimes mislabeled and should be $0000
			// the OR attempts to identify these
			if (auxtype == 0x0000 || (auxtype == 0x0002 && filesize == 32768) ) {
				GraphicsFileFilter filter = new GraphicsFileFilter();
				filter.setMode(GraphicsFileFilter.MODE_SHR_16);
				return filter;
			} else if (auxtype == 0x0002 && filesize == 38400) {
				GraphicsFileFilter filter = new GraphicsFileFilter();
				filter.setMode(GraphicsFileFilter.MODE_SHR_3200);
				return filter;
			}
			// fall through to BinaryFileFilter...
			break;
		case 0x06: 	// BIN
			// the minimum size is guessed a bit - I don't remember, but maybe there
			// are 8 spare bytes at the end of the graphics screen
			if (filesize >= 8184 && filesize <= 8192) {
				GraphicsFileFilter filter = new GraphicsFileFilter();
				filter.setMode(GraphicsFileFilter.MODE_HGR_COLOR);
				return filter;
			} else if (filesize >= 16376 && filesize <= 16384) {
				GraphicsFileFilter filter = new GraphicsFileFilter();
				filter.setMode(GraphicsFileFilter.MODE_DHR_COLOR);
				return filter;
			}
			// fall through to BinaryFileFilter...
			break;
		case 0xca:		// ICN
			{	// This is a trick to fix the scope on the filter variable...
				GraphicsFileFilter filter = new GraphicsFileFilter();
				filter.setMode(GraphicsFileFilter.MODE_QUICKDRAW2_ICON);
				return filter;
			}
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

	/**
	 * Indicates that this filetype can be compiled.
	 */
	public boolean canCompile() {
		return getDisk().canCompile(getFiletype());
	}
}
