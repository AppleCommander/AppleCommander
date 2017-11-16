/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2008 by David Schmidt
 * david__schmidt at users.sourceforge.net
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
 * Tokenize the given file as an Apple /// Business BASCIC file.
 * <p>
 * Apple /// Business BASIC memory format:<br>
 * [Line]<br>
 * ...
 * [Line]<br>
 * <br>
 * where [Line] is:<br>
 * [Offset to next line - $0000 is end of program]  (byte)<br>
 * [Line no]    (word)<br>
 * [Tokens and/or characters]<br>
 * [End-of-line marker: $00 bytes]
 * <p>
 * Date created: Dec 15, 2008 11:17:04 PM
 * @author David Schmidt
 */
public class BusinessBASICTokenizer {
	private static String tokens[] = {	// starts at $80
		"END",      "FOR",      "NEXT",     "INPUT",    "OUTPUT",   "DIM",      "READ",     "WRITE",   //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"OPEN",     "CLOSE",    "*error*",  "TEXT",     "*error*",  "BYE",      "*error*",  "*error*", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"*error*",  "*error*",  "*error*",  "WINDOW",   "INVOKE",   "PERFORM",  "*error*",  "*error*", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"FRE",      "HPOS",     "VPOS",     "ERRLIN",   "ERR",      "KBD",      "EOF",      "TIME$",   //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"DATE$",    "PREFIX$",  "EXFN.",    "EXFN%.",   "OUTREC",   "INDENT",   "*error*",  "*error*", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"*error*",  "*error*",  "*error*",  "*error*",  "*error*",  "POP",      "HOME",     "*error*", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"SUB$(",    "OFF",      "TRACE",    "NOTRACE",  "NORMAL",   "INVERSE",  "SCALE(",   "RESUME",  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"*error*",  "LET",      "GOTO",     "IF",       "RESTORE",  "SWAP",     "GOSUB",    "RETURN",  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"REM",      "STOP",     "ON",       "*error*",  "LOAD",     "SAVE",     "DELETE",   "RUN",     //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"RENAME",   "LOCK",     "UNLOCK",   "CREATE",   "EXEC",     "CHAIN",    "*error*",  "*error*", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"*error*",  "CATALOG",  "*error*",  "*error*",  "DATA",     "IMAGE",    "CAT",      "DEF",     //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"*error*",  "PRINT",    "DEL",      "ELSE",     "CONT",     "LIST",     "CLEAR",    "GET",     //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"NEW",      "TAB",      "TO",       "SPC(",     "USING",    "THEN",     "*error*",  "MOD",     //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"STEP",     "AND",      "OR",       "EXTENSION"," DIV",      "*error*",  "FN",       "NOT",     //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"*error*",  "*error*",  "*error*",  "*error*",  "*error*",  "*error*",  "*error*",  "*error*", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"*error*",  "*error*",  "*error*",  "*error*",  "*error*",  "*error*",  "*error*",  "tf7",     //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"TAB(",     "TO",       "SPC(",     "USING",    "THEN",     "*error*",  "MOD",      "STEP",    //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"AND",      "OR",       "EXTENSION"," DIV",      "*error*",  "FN",       "NOT",      "*error*", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"*error*",  "*error*",  "*error*",  "*error*",  "*error*",  "*error*",  "*error*",  "*error*", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"*error*",  "*error*",  "*error*",  "*error*",  "AS",       "SGN(",     "INT(",     "ABS(",    //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"*error*",  "TYP(",     "REC(",     "*error*",  "*error*",  "*error*",  "*error*",  "*error*", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"*error*",  "*error*",  "*error*",  "*error*",  "*error*",  "PDL(",     "BUTTON(",  "SQR(",    //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"RND(",     "LOG(",     "EXP(",     "COS(",     "SIN(",     "TAN(",     "ATN(",     "*error*", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"*error*",  "*error*",  "*error*",  "*error*",  "*error*",  "*error*",  "*error*",  "*error*", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"*error*",  "*error*",  "*error*",  "STR$(",    "HEX$(",    "CHR$(",    "LEN(",     "VAL(",    //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"ASC(",     "TEN(",     "*error*",  "*error*",  "CONV(",    "CONV&(",   "CONV$(",   "CONV%(",  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"LEFT$(",   "RIGHT$(",  "MID$(",    "INSTR$(",  "*error*",  "*error*",  "*error*",  "*error*"};//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$

	private byte[] fileData;
	private int offset = 2;
	private int nextAddress = -1;

	/**
	 * Constructor for BusinessBASICTokenizer.
	 */
	public BusinessBASICTokenizer(FileEntry fileEntry) {
		this(fileEntry.getFileData());
	}

	/**
	 * Constructor for BusinessBASICTokenizer.
	 */
	public BusinessBASICTokenizer(byte[] fileData) {
		this.fileData = fileData;
	}

	/**
	 * Indicates if there are more tokens in the Business BASIC program.
	 */
	public boolean hasMoreTokens() {
		return (offset < fileData.length);
	}

	/**
	 * Answer with the next token in the Business BASIC program.  This may be 
	 * code, string pieces, line numbers.
	 */
	public BusinessBASICToken getNextToken() {
		if (hasMoreTokens()) {
			if (nextAddress == -1) {
				nextAddress = AppleUtil.getUnsignedByte(fileData, offset);
				offset+= 1;
				if (nextAddress == 0) {
					// At end of file, ensure we don't try to continue processing...
					offset = fileData.length;
					return null;
				}
				int lineNumber = AppleUtil.getWordValue(fileData, offset);
				offset+= 2;
				return new BusinessBASICToken(lineNumber);
			}
			byte byt = fileData[offset++];
			if (byt == 0) {
				nextAddress = -1;
				return getNextToken();
			} else if ((byt & 0x80) != 0) {
				int token = AppleUtil.getUnsignedByte(byt) - 0x80;
				if (token == 0x7f) {
					// Shift to the lower part of the table
					byt = fileData[offset++];
					token = AppleUtil.getUnsignedByte(byt);
				}
				if (token >= tokens.length) {
					return new BusinessBASICToken(byt, "<UNKNOWN TOKEN>"); //$NON-NLS-1$
				}
				return new BusinessBASICToken(byt, tokens[token]);
			} else if (byt == ':' || byt == ';' || byt == ',' || byt == '^'
				|| byt == '+' || byt == '-' || byt == '*' || byt == '/') {
				return new BusinessBASICToken(new String(new byte[] { byt }));
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
				return new BusinessBASICToken(string.toString());
			}
		}
		return null;
	}
}
