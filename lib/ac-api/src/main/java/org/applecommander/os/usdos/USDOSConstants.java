package org.applecommander.os.usdos;

import org.applecommander.capability.Capability;
import org.applecommander.ui.CharacterMapper;

import java.util.HexFormat;
import java.util.Set;

public interface USDOSConstants {
    /// This is part of the message "INSERT DISK WITH \COMMAND.COM" on sector 1.
    byte[] BOOT_PROMPT = HexFormat.of().parseHex("c9ced3c5d2d4a0c4c9d3cba0d7c9d4c8a0dcc3cfcdcdc1cec4aec3cfcd8d");

    int DIRECTORY_TRACK = 0;
    int DIRECTORY_FIRST_SECTOR = 14;
    int DIRECTORY_TOTAL_SECTORS = 13;
    int BITMAP_TRACK = 0;
    int BITMAP_SECTOR = 15;
    int BITMAP_OFFSET = 0x0e;

    int ENTRY_LENGTH = 20;
    int ENTRY_NAME = 0;
    int ENTRY_NAME_LENGTH = 8;
    int ENTRY_EXT = 8;
    int ENTRY_EXT_LENGTH = 3;
    int ENTRY_DIR_VALUE = 0xdc;
    int ENTRY_SIZE = 11;
    int ENTRY_DATE = 13;
    int ENTRY_DIR_NUM = 15;
    int ENTRY_ADDRESS = 16;
    int ENTRY_MAP_TRACK = 18;
    int ENTRY_MAP_SECTOR = 19;

    int INDEX_TRACKS = 0;
    int INDEX_SECTORS = 0xaa;

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
