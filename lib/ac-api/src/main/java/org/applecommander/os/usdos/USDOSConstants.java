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
package org.applecommander.os.usdos;

import org.applecommander.capability.Capability;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.ui.CharacterMapper;

import java.util.HexFormat;
import java.util.Set;

public interface USDOSConstants {
    /// This is part of the message "INSERT DISK WITH \COMMAND.COM" on sector 1.
    byte[] BOOT_PROMPT = HexFormat.of().parseHex("c9ced3c5d2d4a0c4c9d3cba0d7c9d4c8a0dcc3cfcdcdc1cec4aec3cfcd8d");

    int ENTRY_LENGTH = 20;
    int MAX_NAME_LENGTH = 12;   // 8.3 notation
    int ENTRY_BASE_NAME = 0;
    int ENTRY_BASE_NAME_LENGTH = 8;
    int ENTRY_EXT = 8;
    int ENTRY_EXT_LENGTH = 3;
    int ENTRY_DIR_VALUE = 0xdc;
    int ENTRY_SIZE = 11;
    int ENTRY_DATE = 13;
    int ENTRY_DIR_NUM = 15;
    int ENTRY_ADDRESS = 16;
    int ENTRY_MAP_TRACK = 18;
    int ENTRY_MAP_SECTOR = 19;

    int DIRECTORY_TRACK = 0;
    int DIRECTORY_FIRST_SECTOR = 14;
    int DIRECTORY_TOTAL_SECTORS = 13;
    int DIRECTORY_MAX_ENTRIES = (DIRECTORY_TOTAL_SECTORS * TrackSectorDevice.SECTOR_SIZE) / ENTRY_LENGTH;
    int BITMAP_TRACK = 0;
    int BITMAP_SECTOR = 15;
    int BITMAP_OFFSET = 0x0e;

    int INDEX_TRACKS = 0;
    int INDEX_SECTORS = 0xaa;
    int MAX_INDEX_SECTORS = INDEX_SECTORS-1;

    Set<Capability> CAPABILITIES = Set.of(Capability.SUPPORTS_DIRECTORIES, Capability.SUPPORTS_DELETED_FILES,
            Capability.WRITE_FILES, Capability.CREATE_FILES, Capability.DELETE_FILES, Capability.CREATE_DIRECTORIES);

    /// US-DOS is very flexible in what names are allowed. This is more stringent than what US-DOS
    /// will allow (such as a file named `.` or `a....` which has an extension of `...`). However,
    /// you can have `A-B.TXT` but not `A-B`. This limits the name to alphanumeric characters.
    CharacterMapper FILE_NAME_MAPPER = CharacterMapper.builder()
            .mapRange('a', 'z', true)
            .mapRange('A', 'Z', false)
            .mapRange('0', '9', false)
            .map('.', '.')
            .build();

    /// US-DOS is very flexible in what names are allowed. This is more stringent than what US-DOS
    /// will allow (such as a file named `.` or `a....` which has an extension of `...`). However,
    /// you can have `A-B.TXT` but not `A-B`. This limits the name to alphanumeric characters.
    CharacterMapper DIRECTORY_NAME_MAPPER = CharacterMapper.builder()
            .mapRange('a', 'z', true)
            .mapRange('A', 'Z', false)
            .mapRange('0', '9', false)
            .build();
}
