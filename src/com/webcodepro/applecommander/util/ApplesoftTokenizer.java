/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2003 by Robert Greene
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
		" END ",		" FOR ",	" NEXT ",	" DATA ",	" INPUT ",		" DEL ", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		" DIM ",		" READ ",	" GR ",		" TEXT ",	" PR# ",		" IN# ", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		" CALL ",		" PLOT ",	" HLIN ",	" VLIN ",	" HGR2 ",		" HGR ", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		" HCOLOR= ",	" HPLOT ",	" DRAW ",	" XDRAW ",	" HTAB ",		" HOME ", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		" ROT= ",		" SCALE= ",	" SHLOAD ",	" TRACE ",	" NOTRACE ",	" NORMAL ", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		" INVERSE ",	" FLASH ",	" COLOR= ",	" POP ",	" VTAB ",		" HIMEM: ", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		" LOMEM: ",		" ONERR ",	" RESUME ",	" RECALL ",	" STORE ",		" SPEED= ", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		" LET ",		" GOTO ",	" RUN ",	" IF ",		" RESTORE ",	" & ", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		" GOSUB ",		" RETURN ",	" REM ",	" STOP ",	" ON ",			" WAIT ", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		" LOAD ",		" SAVE ",	" DEF ",	" POKE ",	" PRINT ",		" CONT ", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		" LIST ",		" CLEAR ",	" GET ",	" NEW ",	" TAB( ",		" TO ", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		" FN ",			" SPC( ",	"  THEN ",	" AT ",		"  NOT ",		"  STEP ", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		" +",			" -",		" *",		"/",		" ^",			"  AND ", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		"  OR ",		" >",		" = ",		" <",		" SGN",			" INT", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		" ABS",			" USR",		" FRE",		" SCRN( ",	" PDL",			" POS", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		" SQR",			" RND",		" LOG",		" EXP",		" COS",			" SIN", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		" TAN",			" ATN",		" PEEK",	" LEN",		" STR$",		" VAL", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		" ASC",			" CHR$",	" LEFT$",	" RIGHT$",	" MID$ " }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		
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
