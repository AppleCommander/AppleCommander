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
package com.webcodepro.applecommander.storage.filters;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;

/**
 * Filter the given file data for text.
 * <p>
 * Date created: Nov 2, 2002 9:11:27 PM
 * @author Rob Greene
 */
public class TextFileFilter implements FileFilter {
	/**
	 * Constructor for TextFileFilter.
	 */
	public TextFileFilter() {
		super();
	}

	/**
	 * Process the given FileEntry and return a byte array 
	 * with filtered data; use PrintWriter to get platform 
	 * agnostic line endings.
	 */
	public byte[] filter(FileEntry fileEntry) {
		byte[] fileData = fileEntry.getFileData();
		int offset = 0;
		ByteArrayOutputStream byteArray = new
			ByteArrayOutputStream(fileData.length);
		PrintWriter printWriter = new PrintWriter(byteArray, true);
		while (offset < fileData.length) {
			char c = (char)(fileData[offset] & 0x7f);
			if (c != 0) {
				if (c == 0x0d) { //Apple line end
					printWriter.println();
				} else {
					printWriter.print(c);
				}
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
