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

import com.webcodepro.applecommander.util.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

/**
 * Filter the given file as an Applesoft file.
 * <p>
 * Applesoft memory format:<br>
 * [Line]<br>
 * ...
 * [Line]<br>
 * <br>
 * where <Line> is:<br>
 * [Next addr - $0000 is end of program]  (word)<br>
 * [Line no]    (word)<br>
 * [Tokens and/or characters]<br>
 * [End-of-line marker: $00 bytes]
 * <p>
 * Date created: Nov 2, 2002 10:04:10 PM
 * @author: Rob Greene
 */
public class ApplesoftFileFilter implements FileFilter {
	private static String tokens[] = {	// starts at $80
		" END ",		" FOR ",	" NEXT ",	" DATA ",	" INPUT ",		" DEL ",
		" DIM ",		" READ ",	" GR ",		" TEXT ",	" PR# ",		" IN# ",
		" CALL ",		" PLOT ",	" HLIN ",	" VLIN ",	" HGR2 ",		" HGR ",
		" HCOLOR= ",	" HPLOT ",	" DRAW ",	" XDRAW ",	" HTAB ",		" HOME ",
		" ROT= ",		" SCALE= ",	" SHLOAD ",	" TRACE ",	" NOTRACE ",	" NORMAL ",
		" INVERSE ",	" FLASH ",	" COLOR= ",	" POP ",	" VTAB ",		" HIMEM: ",
		" LOMEM: ",		" ONERR ",	" RESUME ",	" RECALL ",	" STORE ",		" SPEED= ",
		" LET ",		" GOTO ",	" RUN ",	" IF ",		" RESTORE ",	" & ",
		" GOSUB ",		" RETURN ",	" REM ",	" STOP ",	" ON ",			" WAIT ",
		" LOAD ",		" SAVE ",	" DEF ",	" POKE ",	" PRINT ",		" CONT ",
		" LIST ",		" CLEAR ",	" GET ",	" NEW ",	" TAB( ",		"  TO ",
		" FN ",			" SPC( ",	"  THEN ",	" AT ",		"  NOT ",		"  STEP ",
		" +",			" -",		" *",		"/",		" ^",			"  AND ",
		"  OR ",		" >",		" =",		" <",		" SGN",			" INT",
		" ABS",			" USR",		" FRE",		" SCRN( ",	" PDL",			" POS",
		" SQR",			" RND",		" LOG",		" EXP",		" COS",			" SIN",
		" TAN",			" ATN",		" PEEK",	" LEN",		" STR$",		" VAL",
		" ASC",			" CHR$",	" LEFT$",	" RIGHT$",	" MID$ " };

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
		byte[] fileData = fileEntry.getFileData();
		int offset = 0;
		ByteArrayOutputStream byteArray = new ByteArrayOutputStream(fileData.length * 2);
		PrintWriter printWriter = new PrintWriter(byteArray, true);
		while (offset < fileData.length) {
			int nextAddress = AppleUtil.getWordValue(fileData, offset);
			if (nextAddress == 0) break;	// next address of 0 indicates end of program
			offset+= 2;
			int lineNumber = AppleUtil.getWordValue(fileData, offset);
			offset+= 2;
			printWriter.print(lineNumber);
			printWriter.print(' ');
			while (fileData[offset] != 0) {
				byte byt = fileData[offset++];
				if ((byt & 0x80) != 0) {
					int token = AppleUtil.getUnsignedByte(byt) - 0x80;
					if (token >= tokens.length) {
						printWriter.print("<UNKNOWN TOKEN>");
					} else {
						String tokenString = tokens[token];
						printWriter.print(tokenString);
					}
				} else {
					char ch = (char)byt;
					if (ch < 0x20) {
						printWriter.print("<CTRL-");
						printWriter.print((char)('@' + ch));
						printWriter.print(">");
					} else {
						printWriter.print(ch);
					}
				}
			}
			printWriter.println();
			offset++;	// skip to next line
		}
		return byteArray.toByteArray();
	}

	/**
	 * Give suggested file name.
	 */
	public String getSuggestedFileName(FileEntry fileEntry) {
		String fileName = fileEntry.getFilename().trim();
		if (!fileName.toLowerCase().endsWith(".bas")) {
			fileName = fileName + ".bas";
		}
		return fileName;
	}
}
