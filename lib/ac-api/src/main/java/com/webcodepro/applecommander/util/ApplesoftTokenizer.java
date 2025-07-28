/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2003-2022 by Robert Greene
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
package com.webcodepro.applecommander.util;

import com.webcodepro.applecommander.storage.FileEntry;

/**
 * Tokenize the given file as an Applesoft file.
 * <p>
 * Applesoft memory format:<br>
 * [Line]<br>
 * ...
 * [Line]<br>
 * <br>
 * where [Line] is:<br>
 * [Next addr - $0000 is end of program]  (word)<br>
 * [Line no]    (word)<br>
 * [Tokens and/or characters]<br>
 * [End-of-line marker: $00 bytes]
 * <p>
 * Date created: May 26, 2003 10:36:04 PM
 * @author Rob Greene
 */
public class ApplesoftTokenizer {
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
		" LIST ",		" CLEAR ",	" GET ",	" NEW ",	" TAB( ",		" TO ",
		" FN ",			" SPC( ",	" THEN ",	" AT ",		" NOT ",		" STEP ",
		" + ",			" - ",		" * ",		" / ",		" ^ ",			" AND ",
		" OR ",			" > ",		" = ",		" < ",		" SGN",			" INT",
		" ABS",			" USR",		" FRE",		" SCRN( ",	" PDL",			" POS",
		" SQR",			" RND",		" LOG",		" EXP",		" COS",			" SIN",
		" TAN",			" ATN",		" PEEK",	" LEN",		" STR$",		" VAL",
		" ASC",			" CHR$",	" LEFT$",	" RIGHT$",	" MID$ "
	};
		
	private byte[] fileData;
	private int offset;
	private int nextAddress = -1;

	/**
	 * Constructor for ApplesoftTokenizer.
	 */
	public ApplesoftTokenizer(FileEntry fileEntry) {
		this(fileEntry.getFileData());
	}
	
	/**
	 * Constructor for ApplesoftTokenizer.
	 */
	public ApplesoftTokenizer(byte[] fileData) {
		this.fileData = fileData;
	}
	
	/**
	 * Indicates if there are more tokens in the Applesoft program.
	 */
	public boolean hasMoreTokens() {
		return (offset < fileData.length);
	}
	
	/**
	 * Answer with the next token in the Applesoft program.  This may be 
	 * code, string pieces, line numbers.
	 */
	public ApplesoftToken getNextToken() {
		if (hasMoreTokens()) {
			if (nextAddress == -1) {
				nextAddress = AppleUtil.getWordValue(fileData, offset);
				offset+= 2;
				if (nextAddress == 0) {
					// At end of file, ensure we don't try to continue processing...
					offset = fileData.length;
					return null;
				}
				int lineNumber = AppleUtil.getWordValue(fileData, offset);
				offset+= 2;
				return new ApplesoftToken(lineNumber);
			}
			byte byt = fileData[offset++];
			if (byt == 0) {
				nextAddress = -1;
				return getNextToken();
			} else if ((byt & 0x80) != 0) {
				int token = AppleUtil.getUnsignedByte(byt) - 0x80;
				if (token >= tokens.length) {
					return new ApplesoftToken(byt, "<UNKNOWN TOKEN>"); //$NON-NLS-1$
				}
				return new ApplesoftToken(byt, tokens[token]);
			} else if (byt == ':' || byt == ';' || byt == ',' || byt == '^'
				|| byt == '+' || byt == '-' || byt == '*' || byt == '/') {
				return new ApplesoftToken(new String(new byte[] { byt }));
			} else {
				StringBuffer string = new StringBuffer();
				while (true) {
					char ch = (char)byt;
					if (ch < 0x20) {
						string.append("<CTRL-"); //$NON-NLS-1$
						string.append((char)('@' + ch));
						string.append('>');
					} else {
						string.append(ch);
					}
					byt = fileData[offset];
					// FIXME: This is a hack to break on ":", ",", ";" but will fail on strings
					if ((byt & 0x80) != 0 || byt == 0
						|| byt == 0x3a || byt == 0x2c || byt == 0x3b) {
						break;
					}
					offset++;
				}
				return new ApplesoftToken(string.toString());
			}
		}
		return null;
	}
}
