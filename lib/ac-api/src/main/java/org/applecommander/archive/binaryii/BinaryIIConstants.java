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
package org.applecommander.archive.binaryii;

public interface BinaryIIConstants {
    /// Binary II block size.
    int BLOCK_SIZE = 128;

    /// First set of identification bytes.
    byte[] ID_BYTES1 = { 0x0a, 0x47, 0x4c };
    /// Fourth identification byte.
    byte[] ID_BYTES2 = { 0x02 };

    /// Offset to the first set of identification bytes.
    int ID_BYTES1_OFFSET = 0;
    /// ProDOS 8 access byte.
    int ACCESS_CODE = 3;
    /// ProDOS 8 file type.
    int FILE_TYPE = 4;
    /// ProDOS 8 auxiliary type.
    int AUX_TYPE = 5;
    /// ProDOS 8 storage type.
    int STORAGE_TYPE = 7;
    /// The size of the file in 512-byte blocks.
    int FILE_SIZE = 8;
    /// Date of modification (ProDOS 8 format).
    int MODIFICATION_DATE = 10;
    /// Time of modification (ProDOS 8 format).
    int MODIFICATION_TIME = 12;
    /// Date of creation (ProDOS 8 format).
    int CREATION_DATE = 14;
    /// Time of creation (ProDOS 8 format).
    int CREATION_TIME = 16;
    /// Fourth identification byte.
    int ID_BYTES2_OFFSET = 18;
    /// The end-of-file value for the file (low byte first).
    int EOF = 20;
    /// Pascal string containing the ASCII filename or partial
    /// pathname of this file in ProDOS 8 format. The string
    /// cannot be longer than 64 characters.
    int FILE_NAME = 23;
    /// Pascal string containing the ASCII value of the native
    /// filename. This string may not be longer than 48 characters,
    /// and will not be present if the length byte of file name
    /// is larger than 15.
    int NATIVE_NAME = 39;
    /// The high word of the file's GS/OS auxiliary type.
    int GSOS_AUX_TYPE = 109;
    /// The high byte of the file's GS/OS access word.
    int GSOS_ACCESS = 111;
    /// The high byte of the file's GS/OS file type.
    int GSOS_FILE_TYPE = 112;
    /// The high byte of the file's GS/OS storage type.
    int GSOS_STORAGE = 113;
    /// The high word of the file's GS/OS file size.
    int GSOS_FILE_SIZE = 114;
    /// The high byte of the file's GS/OS EOF value.
    int GSOS_EOF = 116;
    /// The native operating system of the file.
    int OS_TYPE = 121;
    /// The native file type code assigned to the file. Only valid if OS_TYPE is non-zero (not ProDOS).
    int NATIVE_FILE_TYPE = 122;
    /// Indicates if the file should be ignored. It was present to allow file communication programs
    /// to have custom data sent in the file. Non-zero indicates a regular file.
    int PHANTOM_FILE_FLAG = 124;
    /// Metadata on the contents.
    ///
    /// | Bit | Description |
    /// | :---: | :--- |
    /// | 7 | 1 = file is compressed |
    /// | 6 | 1 = file is encrypted |
    /// | 5..1 | reserved |
    /// | 0 | 1 = file is sparse |
    int DATA_FLAGS = 125;
    /// Version of the Binary II format. Only $01 is defined.
    int VERSION = 126;
    /// The number of files in this Binary II file that remain.
    int REMAINING_FILES = 127;
}
