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

/**
 * Filter the given file data for text.
 * <p>
 * Date created: Nov 2, 2002 9:11:27 PM
 * @author: Rob Greene
 */
public class TextFileFilter implements FileFilter {
	/**
	 * Constructor for TextFileFilter.
	 */
	public TextFileFilter() {
		super();
	}

	/**
	 * Process the given FileEntry and return a byte array with filtered data.
	 * @see com.webcodepro.applecommander.storage.FileFilter#filter(byte[])
	 */
	public byte[] filter(FileEntry fileEntry) {
		byte[] fileData = fileEntry.getFileData();
		byte[] workingData = new byte[fileData.length];
		int position = 0;
		for (int i=0; i<fileData.length; i++) {
			byte byt = fileData[i];
			if (byt != 0) {
				if ((byt & 0x80) != 0) {	// high bit set
					workingData[position++] = (byte)(byt & 0x7f);
				} else {
					workingData[position++] = byt;
				}
			}
		}
		byte[] filteredData = new byte[position];
		System.arraycopy(workingData, 0, filteredData, 0, filteredData.length);
		return filteredData;
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
