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
import com.webcodepro.applecommander.util.AppleUtil;

/**
 * Filter the given file as an Integer BASIC file.
 * <p>
 * On disk, it looks similar to Applesoft - first two bytes are length; rest of
 * image is raw data for Integer Basic.
 * <p>
 * [byte] length of line<br>
 * [word] line number<br>
 * [byte]* line data<br>
 * $01 end of line<br>
 * Repeat until end of program (line length of 0).
 * <p>
 * Tokens are $00 - $7F, some are duplicated.<br>
 * $01 = end of line.<br>
 * $B0 - $B9 = signifies a number stored in a word.<br>
 * <p>
 * Date created: Nov 3, 2002 1:14:47 AM
 * @author Rob Greene
 */
public class IntegerBasicFileFilter implements FileFilter {
	private static String[] tokens = {
		null,	 	null, 		null,	": ",		"LOAD ",	"SAVE ",	null, 		"RUN ",	// $00-$07
		null,		"DEL ",		", ",	"NEW ",		"CLR ",		"AUTO ",	null,		"MAN ",	// $08-$0F
		"HIMEM: ",	"LOMEM: ",	"+",	"-",		"*",		"/",		"=",		"#",	// $10-$17
		">=",		">",		"<=",	"<>",		"<",		" AND ",	" OR ",		" MOD ",// $18-$1F
		"^",		null,		"(",	",",		" THEN ",	" THEN ",	",",		",",	// $20-$27
		"\"",		"\"",		"(",	null,		null,		"(",		" PEEK ",	"RND ",	// $28-$2F
		"SGN ",		"ABS ",		"PDL ",	null,		"(",		"+",		"-",		"NOT ",	// $30-$37
		"(",		"=",		"#",	" LEN(",	" ASC(",	" SCRN(",	",",		" (",	// $38-$3F
		"$",		null,		"(",	",",		",",		";",		";",		";",	// $40-$47
		",",		",",		",",	"TEXT ",	"GR ",		"CALL ",	"DIM ",		"DIM ",	// $48-$4F
		"TAB ",		"END ",		"INPUT ",	"INPUT ",	"INPUT ",	"FOR ",	"=",		" TO ",	// $50-$57
		" STEP ",	"NEXT ",	",",	"RETURN ",	"GOSUB ",	"REM ",		"LET ",		"GOTO ",// $58-$5F
		"IF ",		"PRINT ",	"PRINT ",	"PRINT ",	" POKE ",	",",	"COLOR= ",	"PLOT ",// $60-$67
		",",		"HLIN ",	",",	" AT ",		"VLIN ",	",",		" AT ",		"VTAB ",// $68-$6F
		"=",		"=",		")",	null,		"LIST ",	",",		null,		"POP ",	// $70-$77
		null,		"NO DSP ",	"NO TRACE ",	"DSP ",	"DSP ",	"TRACE ",	"PR # ",	"IN # "	// $78-$7F
	};

	/**
	 * Constructor for IntegerBasicFileFilter.
	 */
	public IntegerBasicFileFilter() {
		super();
	}

	/**
	 * Process the given FileEntry and return a text image of the Integer BASIC file.
	 * @see com.webcodepro.applecommander.storage.FileFilter#filter(FileEntry)
	 */
	public byte[] filter(FileEntry fileEntry) {
		byte[] fileData = fileEntry.getFileData();
		int offset = 0;
		ByteArrayOutputStream byteArray = new ByteArrayOutputStream(fileData.length * 2);
		PrintWriter printWriter = new PrintWriter(byteArray, true);
		while (offset < fileData.length) {
			int lineLength = AppleUtil.getUnsignedByte(fileData[offset]);
			int lineNumber = AppleUtil.getWordValue(fileData, offset+1);
			boolean inComment = false;
			printWriter.print(lineNumber);
			printWriter.print(' ');
			for (int i=offset+3; i<(offset+lineLength); i++) {
				byte byt = fileData[i];
				if ((byt & 0x80) != 0) {
					int value = AppleUtil.getUnsignedByte(byt);
					// numbers follow a number digit
					if (!inComment && value >= 0xb1 && value <= 0xb9) {
						int integer = AppleUtil.getWordValue(fileData, i+1);
						printWriter.print(integer);
						i+= 2;
					} else {
						char ch = (char)(byt&0x7f);
						if (ch < 0x20) {	// handle control characters
							printWriter.print("<CTRL-");
							printWriter.print((char)('@' + ch));
							printWriter.print(">");
						} else {
							printWriter.print(ch);
						}
					}
				} else {
					String token = tokens[(int)byt];
					if (token != null) {
						printWriter.print(token);
						inComment = (byt == 0x5d);	// REM statement
					} else {
						// ignoring unknown tokens
						// $00 and $01 seem to be valid; the others may or may not be valid
					}
				}
			}
			offset+= lineLength;
			printWriter.println();
		}
		return byteArray.toByteArray();
	}

	/**
	 * Give suggested file name.
	 */
	public String getSuggestedFileName(FileEntry fileEntry) {
		String fileName = fileEntry.getFilename().trim();
		if (!fileName.toLowerCase().endsWith(".int")) {
			fileName = fileName + ".int";
		}
		return fileName;
	}
}
