/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2026 by Robert Greene and others
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
package org.applecommander.archive.binscii;

public interface BinsciiConstants {
    String SENTINAL = "FiLeStArTfIlEsTaRt";

    int OFFSET_FILESIZE = 0;
    int OFFSET_SEGSTART = OFFSET_FILESIZE + 3;
    int OFFSET_ACCESS = OFFSET_SEGSTART + 3;
    int OFFSET_FILETYPE = OFFSET_ACCESS + 1;
    int OFFSET_AUXTYPE = OFFSET_FILETYPE + 1;
    int OFFSET_STORETYPE = OFFSET_AUXTYPE + 2;
    int OFFSET_BLKSIZE = OFFSET_STORETYPE + 1;
    int OFFSET_CREDATE = OFFSET_BLKSIZE + 2;
    int OFFSET_CRETIME = OFFSET_CREDATE + 2;
    int OFFSET_MODDATE = OFFSET_CRETIME + 2;
    int OFFSET_MODTIME = OFFSET_MODDATE + 2;
    int OFFSET_SEGLEN = OFFSET_MODTIME + 2;
    int OFFSET_CRC = OFFSET_SEGLEN + 3;
}
