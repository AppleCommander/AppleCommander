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
	private List files;
	private ProdosSubdirectoryHeader subdirectoryHeader;

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
	 * Return the filetype of this file.  This will be three characters,
	 * according to ProDOS - a "$xx" if unknown.
	 * <p>
	 * This could be improved should specific information regarding file types
	 * be needed; the file type could become a separate object which works with
	 * the file in some manner.
	 * <p>
	 * Note: Source of information is the following url -
	 * http://www.apple2.org.za/gswv/gsezine/GS.WorldView/ProDOS.File.Types.v2.0.txt
	 */
	public String getFiletype() {
		int filetype = AppleUtil.getUnsignedByte(readFileEntry()[0x10]);
		switch (filetype) {
			case 0x00:	return "UNK";
			case 0x01:	return "BAD";
			case 0x02:	return "PCD";
			case 0x03:	return "PTX";
			case 0x04:	return "TXT";
			case 0x05:	return "PDA";
			case 0x06:	return "BIN";
			case 0x07:	return "FNT";
			case 0x08:	return "FOT";
			case 0x09:	return "BA3";
			case 0x0a:	return "DA3";
			case 0x0b:	return "WPF";
			case 0x0c:	return "SOS";
			case 0x0f:	return "DIR";
			case 0x10:	return "RPD";
			case 0x11:	return "RPI";
			case 0x12:	return "AFD";
			case 0x13:	return "AFM";
			case 0x14:	return "AFR";
			case 0x15:	return "SCL";
			case 0x16:	return "PFS";
			case 0x19:	return "ADB";	// AppleWorks: AUX TYPE indicates UPPER/lower case
			case 0x1a:	return "AWP";	// AppleWorks: AUX TYPE indicates UPPER/lower case
			case 0x1b:	return "ASP";	// AppleWorks: AUX TYPE indicates UPPER/lower case
			case 0x20:	return "TDM";
			case 0x21:	return "IPS";
			case 0x22:	return "UPV";
			case 0x29:	return "3SD";
			case 0x2a:	return "8SC";
			case 0x2b:	return "8OB";
			case 0x2c:	return "8IC";
			case 0x2d:	return "8LD";
			case 0x2e:	return "P8C";	// P8C or PTP, depending on AUX TYPE
			case 0x41:	return "OCR";
			case 0x42:	return "FTD";
			case 0x50:	return "GWP";
			case 0x51:	return "GSS";
			case 0x52:	return "GDB";
			case 0x53:	return "DRW";
			case 0x54:	return "GDP";
			case 0x55:	return "HMD";
			case 0x56:	return "EDU";
			case 0x57:	return "STN";
			case 0x58:	return "HLP";
			case 0x59:	return "COM";
			case 0x5a:	return "CFG";	// CFG or PTP, depending on AUX TYPE
			case 0x5b:	return "ANM";
			case 0x5c:	return "MUM";
			case 0x5d:	return "ENT";
			case 0x5e:	return "DVU";
			case 0x60:	return "PRE";
			case 0x6b:	return "BIO";
			case 0x6d:	return "DVR";	// DVR/TDR
			case 0x6e:	return "PRE";
			case 0x6f:	return "HDV";	// PC Volume
			case 0xa0:	return "WP_";
			case 0xab:	return "GSB";
			case 0xac:	return "TDF";
			case 0xad:	return "BDF";
			case 0xb0:	return "SRC";
			case 0xb1:	return "OBJ";
			case 0xb2:	return "LIB";
			case 0xb3:	return "S16";
			case 0xb4:	return "RTL";
			case 0xb5:	return "EXE";
			case 0xb6:	return "STR";	// STR/PIF
			case 0xb7:	return "TSF";	// TSF/TIF
			case 0xb8:	return "NDA";
			case 0xb9:	return "CDA";
			case 0xba:	return "TOL";
			case 0xbb:	return "DRV";	// DRV/DVR
			case 0xbc:	return "LDF";
			case 0xbd:	return "FST";
			case 0xbf:	return "DOC";
			case 0xc0:	return "PNT";
			case 0xc1:	return "PIC";
			case 0xc2:	return "ANI";
			case 0xc3:	return "PAL";
			case 0xc5:	return "OOG";
			case 0xc6:	return "SCR";
			case 0xc7:	return "CDV";
			case 0xc8:	return "FON";
			case 0xc9:	return "FND";
			case 0xca:	return "ICN";
			case 0xd5:	return "MUS";
			case 0xd6:	return "INS";
			case 0xd7:	return "MDI";
			case 0xd8:	return "SND";
			case 0xdb:	return "DBM";
			case 0xe0:	return "SHK";
			case 0xe2:	return "DTS";	// DTS/ATK
			case 0xee:	return "R16";
			case 0xef:	return "PAS";
			case 0xf0:	return "CMD";
			// Left $F1 - $F8 alone as these are user-defined types
			case 0xf9:	return "P16";
			case 0xfa:	return "INT";
			case 0xfb:	return "IVR";
			case 0xfc:	return "BAS";
			case 0xfd:	return "VAR";
			case 0xfe:	return "REL";
			case 0xff:	return "SYS";
			default :
				return "$" + AppleUtil.getFormattedByte(filetype);
		}
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
	 * Get the number of blocks used.
	 */
	public int getBlocksUsed() {
		return AppleUtil.getWordValue(readFileEntry(), 0x13);
	}

	/**
	 * Get the EOF position.  This can indicate the length of a file.
	 */
	public int getEofPosition() {
		return AppleUtil.get3ByteValue(readFileEntry(), 0x15);
	}


	/**
	 * Get the auxiliary type for this file.
	 * TXT - random access record length.
	 * BIN - load address for binary image.
	 * BAS - load address for program image.
	 * VAR - address of compressed variables image.
	 * SYS - load address for system program (usually 0x2000).
	 */
	public int getAuxiliaryType() {
		return AppleUtil.getWordValue(readFileEntry(), 0x1f);
	}

	/**
	 * Get the last modification date.
	 */
	public Date getLastModificationDate() {
		return AppleUtil.getProdosDate(readFileEntry(), 0x21);
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
	 * Retrieve the list of files in this directory.
	 * Note that if this is not a directory, the return
	 * value should be null.  If this a directory, the
	 * return value should always be a list - a directory
	 * with 0 entries returns an empty list.
	 */
	public List getFiles() {
		return files;
	}

	/**
	 * Set the list of files.
	 */
	public void setFiles(List files) {
		this.files = files;
	}

	/**
	 * Identify if this file has been deleted.
	 */
	public boolean isDeleted() {
		return getStorageType() == 0;
	}

	/**
	 * Set the subdirectory header.
	 */
	public void setSubdirectoryHeader(ProdosSubdirectoryHeader subdirectoryHeader) {
		this.subdirectoryHeader = subdirectoryHeader;
	}
	
	/**
	 * Get the subdirectory header.
	 */
	public ProdosSubdirectoryHeader getSubdirectoryHeader() {
		return this.subdirectoryHeader;
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
					list.add("");
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
	 * Get the suggested FileFilter.  This appears to be operating system
	 * specific, so each operating system needs to implement some manner
	 * of guessing the appropriate filter.
	 */
	public FileFilter getSuggestedFilter() {
		if ("TXT".equals(getFiletype()) || "SRC".equals(getFiletype())) {
			return new TextFileFilter();
		} else if ("AWP".equals(getFiletype())) {
			return new AppleWorksWordProcessorFileFilter();
		} else if ("BAS".equals(getFiletype())) {
			return new ApplesoftFileFilter();
		} else if ("INT".equals(getFiletype())) {	// supposedly not available in ProDOS, however
			return new IntegerBasicFileFilter();
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
}
