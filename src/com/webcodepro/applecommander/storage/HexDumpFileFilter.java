/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2003 by Robert Greene
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

import com.webcodepro.applecommander.util.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

/**
 * Filter the given file data to be the appropriate length.
 * <p>
 * Date created: Nov 2, 2002 9:07:52 PM
 * @author: Rob Greene
 */
public class HexDumpFileFilter implements FileFilter {
	/**
	 * This is the number of bytes to display per line.
	 */
	private static final int BYTES_PER_LINE = 16;
	/**
	 * This is the ASCII space character as used by the Apple ][.
	 * The high bit is off.
	 */
	private static final int APPLE_SPACE = 0x20;
	
	/**
	 * Constructor for BinaryFileFilter.
	 */
	public HexDumpFileFilter() {
		super();
	}

	/**
	 * Create the hex dump format.  This is in the general form of:<br>
	 * MMMMMM: HH HH HH HH HH HH HH HH  HH HH HH HH HH HH HH HH  AAAAAAAA AAAAAAAA<br>
	 * Where MMMMMM = memory address, HH = hex byte, and
	 * A = ASCII character.
	 * @see com.webcodepro.applecommander.storage.FileFilter#filter(byte[])
	 */
	public byte[] filter(FileEntry fileEntry) {
		byte[] fileData = fileEntry.getFileData();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		PrintWriter printer = new PrintWriter(output);
		printer.print(" Offset  ");
		printer.print("Hex Data                                          ");
		printer.println("Characters");
		printer.print("=======  ");
		printer.print("================================================  ");
		printer.println("=================");
		for (int offset=0; offset<fileData.length; offset+= BYTES_PER_LINE) {
			printer.print("$");
			printer.print(AppleUtil.getFormatted3ByteAddress(offset));
			printer.print("  ");
			for (int b=0; b<BYTES_PER_LINE; b++) {
				if (b == BYTES_PER_LINE / 2) printer.print(' ');
				int index = offset+b;
				printer.print( (index < fileData.length) ?
					AppleUtil.getFormattedByte(fileData[index]) : "..");
				printer.print(' ');
			}
			printer.print(' ');
			for (int a=0; a<BYTES_PER_LINE; a++) {
				if (a == BYTES_PER_LINE / 2) printer.print(' ');
				int index = offset+a;
				if (index < fileData.length) {
					char ch = (char) (fileData[index] & 0x7f);
					if ((byte)ch >= (byte)APPLE_SPACE) {
						printer.print(ch);
					} else {
						printer.print('.');
					}
				} else {
					printer.print(' ');
				}
			}
			printer.println();
		}
		printer.println("** END **");
		printer.flush();
		printer.close();
		return output.toByteArray();
	}

	/**
	 * Give suggested file name.
	 */
	public String getSuggestedFileName(FileEntry fileEntry) {
		String fileName = fileEntry.getFilename().trim();
		if (!fileName.toLowerCase().endsWith(".txt")) {
			fileName = fileName + ".txt";
		}
		return fileName;
	}
}
