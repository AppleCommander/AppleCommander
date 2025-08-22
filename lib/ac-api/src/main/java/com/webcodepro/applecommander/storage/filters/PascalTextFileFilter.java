/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2004-2022 by Robert Greene
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
package com.webcodepro.applecommander.storage.filters;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;

/**
 * Filter the given file as a Pascal ".text" file.
 * <p>
 * From the Apple Pascal v1.2 Operating System Reference manual:
 * <quote>
 * <b>Text files in Apple Pascal</b>
 * <hr>
 * At the beginning of each textfile is a 1024-byte (two blocks on
 * diskette) header page, which contains information for the texteditor. 
 * This space is reserved for use by the text editor, and is respected 
 * by all portions of the system. When a user program opens a text file, 
 * and REWRITEs or RESETs it with a title ending in .TEXT, the I/O 
 * subsystem will create and skip over the initial header page. This is 
 * done to facilitate users editing their input and/or output data. The 
 * file-handler will transfer the header page only on a disk-to-disk 
 * transfer, and will omit it on a transfer to a serial device (thus 
 * transfers to PRINTER: and CONSOLE: will omit the header page).
 * <p>
 * Following the initial header page, the text itself appears in 
 * subsequent 1024-byte text pages (two block each, on diskette), 
 * where a text page is defined:<br>
 * <pre>
 *     [DLE] [indent] [text] [cr] [dle] [indent] [text] [CR] .. [nulls]
 * </pre>
 * DLE's (Data Link Escapes) are followed by an indent-code, which is a 
 * byte containing the value 32+(number to indent). The nulls at the 
 * end of the page follow a [CR} in all cases, and are a pad to the end 
 * of a 1024-byte page (because the compiler wants integral numbers of 
 * lines on a page). The Data Link Escape and corresponding indentation 
 * code are optional. In a given text file, some lines will have the 
 * codes, and some won't.
 * </quote>
 * (Thanks to Hans Otten for sending in this information.)
 *
 * @author Rob Greene
 */
public class PascalTextFileFilter implements FileFilter {
	private static final char NUL = 0x00;
	private static final char CR = 0x0d;
	private static final char DLE = 0x10;
	private static final int HEADER_SIZE = 1024;
	private static final int INDENT_BASE_VALUE = 0x20;
	
	/**
	 * Constructor for PascalTextFileFilter.
	 */
	public PascalTextFileFilter() {
		super();
	}

	/**
	 * Process the given FileEntry and return a byte array 
	 * with filtered data; use PrintWriter to get platform 
	 * agnostic line endings.
	 */
	public byte[] filter(FileEntry fileEntry) {
		byte[] fileData = fileEntry.getFileData();
		int offset = HEADER_SIZE;
		ByteArrayOutputStream byteArray = new
			ByteArrayOutputStream(fileData.length);
		PrintWriter printWriter = new PrintWriter(byteArray, true);
		while (offset < fileData.length) {
			char c = (char)(fileData[offset] & 0x7f);	// Will the bitwise AND be a problem??
			switch (c) {
				case NUL:	break;		// ignore
				case CR:	printWriter.println();
							break;
				case DLE:	if (offset+1 < fileData.length) {
								offset++;
								int indent = fileData[offset] - INDENT_BASE_VALUE;
								while (indent-- > 0) {
									printWriter.print(' ');
								}
							}
							break;
				default:	printWriter.print(c);
							break;
			}
			offset++;
		}
		return byteArray.toByteArray();
	}


	/**
	 * Give suggested file name.
	 */
	public String getSuggestedFileName(FileEntry fileEntry) {
		String fileName = fileEntry.getFilename().trim();
		if (!fileName.toLowerCase().endsWith(".txt")) { //$NON-NLS-1$
			fileName = fileName + ".txt"; //$NON-NLS-1$
		}
		return fileName;
	}
}
