/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2022 by Robert Greene
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

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.util.AppleUtil;
import com.webcodepro.applecommander.util.ApplesoftToken;
import com.webcodepro.applecommander.util.ApplesoftTokenizer;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

/**
 * Filter the given file as an Applesoft file.
 * <p>
 * Date created: Nov 2, 2002 10:04:10 PM
 * @author Rob Greene
 */
public class ApplesoftFileFilter implements FileFilter {
	/**
	 * Constructor for ApplesoftFileFilter.
	 */
	public ApplesoftFileFilter() {
		super();
	}

	/**
	 * Process the given FileEntry and return a text image of the Applesoft file.
	 * @see com.webcodepro.applecommander.storage.FileFilter#filter(FileEntry)
	 */
	public byte[] filter(FileEntry fileEntry) {
		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
		PrintWriter printWriter = new PrintWriter(byteArray, true);
		ApplesoftTokenizer tokenizer = new ApplesoftTokenizer(fileEntry);
		generateProgramCode(tokenizer, printWriter);
		// should this be a while?
		int lastOffset = tokenizer.getOffset();
		if (tokenizer.getOffset() < fileEntry.getSize()) {
			for (int n = tokenizer.getOffset(); n < fileEntry.getSize(); n++) {
				if (tokenizer.testValidity(n)) {
					if (n - lastOffset > 10) {
						byte[] hexData = new byte[n - lastOffset];
						System.arraycopy(fileEntry.getFileData(), lastOffset, hexData, 0, hexData.length);
						printWriter.println();
						printWriter.println(AppleUtil.getHexDump(hexData));
					}
					printWriter.println();
					tokenizer.setOffset(n);
					generateProgramCode(tokenizer, printWriter);
					lastOffset = n;
				}
			}
			if (fileEntry.getSize() - lastOffset > 10) {
				byte[] hexData = new byte[fileEntry.getSize() - lastOffset];
				System.arraycopy(fileEntry.getFileData(), lastOffset, hexData, 0, hexData.length);
				printWriter.println();
				printWriter.println(AppleUtil.getHexDump(hexData));
			}
		}
		printWriter.close();
		return byteArray.toByteArray();
	}

	protected void generateProgramCode(ApplesoftTokenizer tokenizer, PrintWriter printWriter) {
		boolean firstLine = true;
		while (tokenizer.hasMoreTokens()) {
			ApplesoftToken token = tokenizer.getNextToken();
			if (token == null) {
				break;
			} else if (token.isLineNumber()) {
				if (firstLine) {
					firstLine = false;
				} else {
					printWriter.println();
				}
				printWriter.print(token.getLineNumber());
				printWriter.print(" "); //$NON-NLS-1$
			} else if (token.isToken()) {
				printWriter.print(token.getTokenString());
			} else {
				printWriter.print(token.getStringValue());
			}
		}
		printWriter.println();
	}

	/**
	 * Give suggested file name.
	 */
	public String getSuggestedFileName(FileEntry fileEntry) {
		String fileName = fileEntry.getFilename().trim();
		if (!fileName.toLowerCase().endsWith(".bas")) { //$NON-NLS-1$
			fileName = fileName + ".bas"; //$NON-NLS-1$
		}
		return fileName;
	}
}
