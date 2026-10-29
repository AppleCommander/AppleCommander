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
package org.applecommander.os.pascal;

public interface PascalConstants {
    int DIRECTORY_FIRST_BLOCK = 2;
    int DIRECTORY_LAST_BLOCK = 5;
    int DIRECTORY_BLOCKS = 4;

    int ENTRY_LENGTH = 26;

    /// The block number of the first block. For the volume header, this is the first directory block
    /// and is always 0.
    int ENTRY_FIRST_BLOCK = 0;
    /// The block number of the last block. For the volume header, this is the last directory block
    /// and is always 6. For a file, it is the last block+1.
    int ENTRY_LAST_BLOCK = 2;
    /// File type. Bits 0-3 below. Bit 15 is used by the filer. Bits 4-14 reserved.
    /// | Value | Description |
    /// | :---: | :--- |
    /// | 0     | Volume Header |
    /// | 1     | `xdskfile` (bad blocks) |
    /// | 2     | `codefile` |
    /// | 3     | `textfile` |
    /// | 4     | `infofile` |
    /// | 5     | `datafile` |
    /// | 6     | `graffile` |
    /// | 7     | `fotofile` |
    /// | 8     | `securedir` |
    int ENTRY_TYPE = 4;
    /// Name of entry. Note that volume and file have different max lengths.
    int ENTRY_NAME = 6;
    /// Maximum length of a file name.
    int FILE_NAME_LENGTH = 15;
    /// Maximum length of the volume name.
    int VOLUME_NAME_LENGTH = 7;
    /// Number of blocks on disk.
    int VOLUME_BLOCKS_ON_DISK = 14;
    /// Number of files on disk.
    int VOLUME_FILES_ON_DISK = 16;
    /// Most recent date setting.
    int VOLUME_RECENT_DATE = 20;
    /// Number of bytes used in the last block. Range is 1..512.
    int FILE_BYTES_USED_IN_LAST_BLOCK = 22;
    /// Modification date for file.
    int FILE_MODIFICATION_DATE = 24;
}
