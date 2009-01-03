/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002, 2008 by Robert Greene
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
import com.webcodepro.applecommander.util.BusinessBASICToken;
import com.webcodepro.applecommander.util.BusinessBASICTokenizer;

/**
 * Filter the given file as an Apple /// Business BASIC file.
 * <p>
 * Date created: Dec 15, 2008 11:12:10 PM
 * 
 * @author David Schmidt
 */
public class BusinessBASICFileFilter implements FileFilter {
	/**
	 * Constructor for BusinessBASICFileFilter.
	 */
	public BusinessBASICFileFilter() {
		super();
	}

	/**
	 * Process the given FileEntry and return a text image of the Business BASIC
	 * file.
	 * 
	 * @see com.webcodepro.applecommander.storage.FileFilter#filter(FileEntry)
	 */
	public byte[] filter(FileEntry fileEntry) {
		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
		PrintWriter printWriter = new PrintWriter(byteArray, true);
		BusinessBASICTokenizer tokenizer = new BusinessBASICTokenizer(fileEntry);
		boolean firstLine = true;
		boolean firstData = true;
		int nestLevels = 0;
		while (tokenizer.hasMoreTokens()) {
			BusinessBASICToken token = tokenizer.getNextToken();
			if (token == null) {
				break;
			} else if (token.isLineNumber()) {
				if (firstLine) {
					firstLine = false;
				} else {
					printWriter.println();
				}
				firstData = true;
				printWriter.print(token.getLineNumber());
				printWriter.print("   "); //$NON-NLS-1$
				if (nestLevels > 0) {
					for (int i = 0; i < nestLevels; i++)
						printWriter.print("  "); //$NON-NLS-1$
					}
			} else if (token.isToken()) {
				if (!firstData)
					printWriter.print(" "); //$NON-NLS-1$
				printWriter.print(token.getTokenString());
				firstData = false;
				if (token.isIndenter()) {
					nestLevels ++; }
				else if (token.isOutdenter()) {
					nestLevels --; }
			} else if (token.isCommandSeparator() || token.isExpressionSeparator()) {
				printWriter.print(token.getStringValue());
				firstData = true;
			} else {
				if (!firstData)
					printWriter.print(" "); //$NON-NLS-1$
				printWriter.print(token.getStringValue().trim());
				firstData = false;
			}
		}
		printWriter.close();
		return byteArray.toByteArray();
	}

	/**
	 * Give suggested file name.
	 */
	public String getSuggestedFileName(FileEntry fileEntry) {
		String fileName = fileEntry.getFilename().trim();
		if (!fileName.toLowerCase().endsWith(".ba3")) { //$NON-NLS-1$
			fileName = fileName + ".ba3"; //$NON-NLS-1$
		}
		return fileName;
	}
}
