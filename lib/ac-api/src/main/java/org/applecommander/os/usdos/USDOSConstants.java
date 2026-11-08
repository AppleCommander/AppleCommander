package org.applecommander.os.usdos;

import org.applecommander.capability.Capability;

import java.util.HexFormat;
import java.util.Set;

public interface USDOSConstants {
    /// This is part of the message "INSERT DISK WITH \COMMAND.COM" on sector 1.
    byte[] BOOT_PROMPT = HexFormat.of().parseHex("c9ced3c5d2d4a0c4c9d3cba0d7c9d4c8a0dcc3cfcdcdc1cec4aec3cfcd8d");

    int DIRECTORY_TRACK = 0;
    int DIRECTORY_FIRST_SECTOR = 14;
    int DIRECTORY_TOTAL_SECTORS = 13;

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

    Set<Capability> CAPABILITIES = Set.of(Capability.SUPPORTS_DIRECTORIES, Capability.SUPPORTS_DELETED_FILES);
}
