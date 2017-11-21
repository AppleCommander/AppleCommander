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
 * @author Rob Greene
 */
public class AssemblySourceFileFilter implements FileFilter {
	private int[] tabStops = new int[] { 10, 15 };
	private int commentTabStop = 25;
	private String tabChars = "\t "; //$NON-NLS-1$
	private String commentTabChars = ";"; //$NON-NLS-1$
	private String commentNoTabChars = "*"; //$NON-NLS-1$

	/**
	 * Process the given FileEntry and return a byte array 
	 * with formatted assembly data; use PrintWriter to get platform 
	 * agnostic line endings.
	 */
	public byte[] filter(FileEntry fileEntry) {
		byte[] fileData = fileEntry.getFileData();
		int offset = 0;
		ByteArrayOutputStream byteArray = new
			ByteArrayOutputStream(fileData.length);
		PrintWriter printWriter = new PrintWriter(byteArray, true);
		int tabPosition = 0;
		int charPosition = 0;
		boolean noMoreTabs = false;
		while (offset < fileData.length) {
			char c = (char)(fileData[offset] & 0x7f);
			if (c != 0) {
				// Apple line end
				if (c == 0x0d) {
					printWriter.println();
					tabPosition = 0;
					charPosition = 0;
					noMoreTabs = false;
				// Tab character
				} else if (!noMoreTabs && tabChars.indexOf(c) > -1) { 
					if (tabPosition < tabStops.length) {
						int desiredPosition = tabStops[tabPosition];
						// Always need one space...
						printWriter.print(' ');
						charPosition++;
						// Do we need more?
						while (charPosition < desiredPosition) {
							printWriter.print(' ');
							charPosition++;
						}
					}
				// EOL comment
				} else if (!noMoreTabs && commentTabChars.indexOf(c) > -1) { 
					while (charPosition < commentTabStop) {
						printWriter.print(' ');
						charPosition++;
					}
					printWriter.print(c);
					charPosition++;
				// BOL comment
				} else if (charPosition == 0 && commentNoTabChars.indexOf(c) > -1) {
					noMoreTabs = true;
					printWriter.print(c);
					charPosition++;
				// Actual source code!
				} else {
					printWriter.print(c);
					charPosition++;
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
		if (!fileName.toLowerCase().endsWith(".s")) { //$NON-NLS-1$
			fileName += ".s"; //$NON-NLS-1$
		}
		return fileName;
	}
}
