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

/**
 * This class contains all Applesoft tokens.
 * Note that invalid source characters (&amp;, &lt;, etc)
 * are spelled out.
 * @author Rob
 */
public interface BusinessBASICTokens {
	public static final byte END = (byte) 0x80;
	public static final byte FOR = (byte) 0x81;
	public static final byte NEXT = (byte) 0x82;
	public static final byte DATA = (byte) 0x83;
	public static final byte INPUT = (byte) 0x84;
	public static final byte DEL = (byte) 0x85;
	public static final byte DIM = (byte) 0x86;
	public static final byte READ = (byte) 0x87;
	public static final byte GR = (byte) 0x88;
	public static final byte TEXT = (byte) 0x89;
	public static final byte PRnbr = (byte) 0x8a;
	public static final byte INnbr = (byte) 0x8b;
	public static final byte CALL = (byte) 0x8c;
	public static final byte PLOT = (byte) 0x8d;
	public static final byte HLIN = (byte) 0x8e;
	public static final byte VLIN = (byte) 0x8f;
	public static final byte HGR2 = (byte) 0x90;
	public static final byte HGR = (byte) 0x91;
	public static final byte HCOLOR = (byte) 0x92;
	public static final byte HPLOT = (byte) 0x93;
	public static final byte DRAW = (byte) 0x94;
	public static final byte XDRAW = (byte) 0x95;
	public static final byte HTAB = (byte) 0x96;
	public static final byte HOME = (byte) 0x97;
	public static final byte ROT = (byte) 0x98;
	public static final byte SCALE = (byte) 0x99;
	public static final byte SHLOAD = (byte) 0x9a;
	public static final byte TRACE = (byte) 0x9b;
	public static final byte NOTRACE = (byte) 0x9c;
	public static final byte NORMAL = (byte) 0x9d;
	public static final byte INVERSE = (byte) 0x9e;
	public static final byte FLASH = (byte) 0x9f;
	public static final byte COLOR = (byte) 0xa0;
	public static final byte POP = (byte) 0xa1;
	public static final byte VTAB = (byte) 0xa2;
	public static final byte HIMEM = (byte) 0xa3;
	public static final byte LOMEM = (byte) 0xa4;
	public static final byte ONERR = (byte) 0xa5;
	public static final byte RESUME = (byte) 0xa6;
	public static final byte RECALL = (byte) 0xa7;
	public static final byte STORE = (byte) 0xa8;
	public static final byte SPEED = (byte) 0xa9;
	public static final byte LET = (byte) 0xaa;
	public static final byte GOTO = (byte) 0xab;
	public static final byte RUN = (byte) 0xac;
	public static final byte IF = (byte) 0xad;
	public static final byte RESTORE = (byte) 0xae;
	public static final byte AMPERSAND = (byte) 0xaf;
	public static final byte GOSUB = (byte) 0xb0;
	public static final byte RETURN = (byte) 0xb1;
	public static final byte REM = (byte) 0xb2; 
	public static final byte STOP = (byte) 0xb3;
	public static final byte ON = (byte) 0xb4;
	public static final byte WAIT = (byte) 0xb5;
	public static final byte LOAD = (byte) 0xb6;
	public static final byte SAVE = (byte) 0xb7;
	public static final byte DEF = (byte) 0xb8;
	public static final byte POKE = (byte) 0xb9;
	public static final byte PRINT = (byte) 0xba;
	public static final byte CONT = (byte) 0xbb;
	public static final byte LIST = (byte) 0xbc;
	public static final byte CLEAR = (byte) 0xbd;
	public static final byte GET = (byte) 0xbe;
	public static final byte NEW = (byte) 0xbf;
	public static final byte TAB = (byte) 0xc0;
	public static final byte TO = (byte) 0xc1;
	public static final byte FN = (byte) 0xc2;
	public static final byte SPC = (byte) 0xc3;
	public static final byte THEN = (byte) 0xc4;
	public static final byte AT = (byte) 0xc5;
	public static final byte NOT = (byte) 0xc6;
	public static final byte STEP = (byte) 0xc7;
	public static final byte PLUS = (byte) 0xc8;
	public static final byte MINUS = (byte) 0xc9;
	public static final byte MULTIPLY = (byte) 0xca;
	public static final byte DIVIDE = (byte) 0xcb;
	public static final byte POWER = (byte) 0xcc;
	public static final byte AND = (byte) 0xcd;
	public static final byte OR = (byte) 0xce;
	public static final byte GREATERTHAN = (byte) 0xcf;
	public static final byte EQUALS = (byte) 0xd0;
	public static final byte LESSTHAN = (byte) 0xd1;
	public static final byte SGN = (byte) 0xd2;
	public static final byte INT = (byte) 0xd3;
	public static final byte ABS = (byte) 0xd4;
	public static final byte USR = (byte) 0xd5;
	public static final byte FRE = (byte) 0xd6;
	public static final byte SCRN = (byte) 0xd7;
	public static final byte PDL = (byte) 0xd8;
	public static final byte POS = (byte) 0xd9;
	public static final byte SQR = (byte) 0xda;
	public static final byte RND = (byte) 0xdb;
	public static final byte LOG = (byte) 0xdc;
	public static final byte EXP = (byte) 0xdd;
	public static final byte COS = (byte) 0xde;
	public static final byte SIN = (byte) 0xdf;
	public static final byte TAN = (byte) 0xe0;
	public static final byte ATN = (byte) 0xe1;
	public static final byte PEEK = (byte) 0xe2;
	public static final byte LEN = (byte) 0xe3;
	public static final byte STR$ = (byte) 0xe4;
	public static final byte VAL = (byte) 0xe5;
	public static final byte ASC = (byte) 0xe6;
	public static final byte CHR$ = (byte) 0xe7;
	public static final byte LEFT$ = (byte) 0xe8;
	public static final byte RIGHT$ = (byte) 0xe9;
	public static final byte MID$ = (byte) 0xea;
}
