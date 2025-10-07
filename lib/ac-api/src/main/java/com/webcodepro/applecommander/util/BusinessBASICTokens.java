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

/**
 * This class contains all Applesoft tokens.
 * Note that invalid source characters (&amp;, &lt;, etc)
 * are spelled out.
 * @author Rob
 */
public interface BusinessBASICTokens {
	byte END = (byte) 0x80;
	byte FOR = (byte) 0x81;
	byte NEXT = (byte) 0x82;
	byte DATA = (byte) 0x83;
	byte INPUT = (byte) 0x84;
	byte DEL = (byte) 0x85;
	byte DIM = (byte) 0x86;
	byte READ = (byte) 0x87;
	byte GR = (byte) 0x88;
	byte TEXT = (byte) 0x89;
	byte PRnbr = (byte) 0x8a;
	byte INnbr = (byte) 0x8b;
	byte CALL = (byte) 0x8c;
	byte PLOT = (byte) 0x8d;
	byte HLIN = (byte) 0x8e;
	byte VLIN = (byte) 0x8f;
	byte HGR2 = (byte) 0x90;
	byte HGR = (byte) 0x91;
	byte HCOLOR = (byte) 0x92;
	byte HPLOT = (byte) 0x93;
	byte DRAW = (byte) 0x94;
	byte XDRAW = (byte) 0x95;
	byte HTAB = (byte) 0x96;
	byte HOME = (byte) 0x97;
	byte ROT = (byte) 0x98;
	byte SCALE = (byte) 0x99;
	byte SHLOAD = (byte) 0x9a;
	byte TRACE = (byte) 0x9b;
	byte NOTRACE = (byte) 0x9c;
	byte NORMAL = (byte) 0x9d;
	byte INVERSE = (byte) 0x9e;
	byte FLASH = (byte) 0x9f;
	byte COLOR = (byte) 0xa0;
	byte POP = (byte) 0xa1;
	byte VTAB = (byte) 0xa2;
	byte HIMEM = (byte) 0xa3;
	byte LOMEM = (byte) 0xa4;
	byte ONERR = (byte) 0xa5;
	byte RESUME = (byte) 0xa6;
	byte RECALL = (byte) 0xa7;
	byte STORE = (byte) 0xa8;
	byte SPEED = (byte) 0xa9;
	byte LET = (byte) 0xaa;
	byte GOTO = (byte) 0xab;
	byte RUN = (byte) 0xac;
	byte IF = (byte) 0xad;
	byte RESTORE = (byte) 0xae;
	byte AMPERSAND = (byte) 0xaf;
	byte GOSUB = (byte) 0xb0;
	byte RETURN = (byte) 0xb1;
	byte REM = (byte) 0xb2;
	byte STOP = (byte) 0xb3;
	byte ON = (byte) 0xb4;
	byte WAIT = (byte) 0xb5;
	byte LOAD = (byte) 0xb6;
	byte SAVE = (byte) 0xb7;
	byte DEF = (byte) 0xb8;
	byte POKE = (byte) 0xb9;
	byte PRINT = (byte) 0xba;
	byte CONT = (byte) 0xbb;
	byte LIST = (byte) 0xbc;
	byte CLEAR = (byte) 0xbd;
	byte GET = (byte) 0xbe;
	byte NEW = (byte) 0xbf;
	byte TAB = (byte) 0xc0;
	byte TO = (byte) 0xc1;
	byte FN = (byte) 0xc2;
	byte SPC = (byte) 0xc3;
	byte THEN = (byte) 0xc4;
	byte AT = (byte) 0xc5;
	byte NOT = (byte) 0xc6;
	byte STEP = (byte) 0xc7;
	byte PLUS = (byte) 0xc8;
	byte MINUS = (byte) 0xc9;
	byte MULTIPLY = (byte) 0xca;
	byte DIVIDE = (byte) 0xcb;
	byte POWER = (byte) 0xcc;
	byte AND = (byte) 0xcd;
	byte OR = (byte) 0xce;
	byte GREATERTHAN = (byte) 0xcf;
	byte EQUALS = (byte) 0xd0;
	byte LESSTHAN = (byte) 0xd1;
	byte SGN = (byte) 0xd2;
	byte INT = (byte) 0xd3;
	byte ABS = (byte) 0xd4;
	byte USR = (byte) 0xd5;
	byte FRE = (byte) 0xd6;
	byte SCRN = (byte) 0xd7;
	byte PDL = (byte) 0xd8;
	byte POS = (byte) 0xd9;
	byte SQR = (byte) 0xda;
	byte RND = (byte) 0xdb;
	byte LOG = (byte) 0xdc;
	byte EXP = (byte) 0xdd;
	byte COS = (byte) 0xde;
	byte SIN = (byte) 0xdf;
	byte TAN = (byte) 0xe0;
	byte ATN = (byte) 0xe1;
	byte PEEK = (byte) 0xe2;
	byte LEN = (byte) 0xe3;
	byte STR$ = (byte) 0xe4;
	byte VAL = (byte) 0xe5;
	byte ASC = (byte) 0xe6;
	byte CHR$ = (byte) 0xe7;
	byte LEFT$ = (byte) 0xe8;
	byte RIGHT$ = (byte) 0xe9;
	byte MID$ = (byte) 0xea;
}
