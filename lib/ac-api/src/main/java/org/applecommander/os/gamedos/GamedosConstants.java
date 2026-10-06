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
package org.applecommander.os.gamedos;

import org.applecommander.capability.Capability;

import java.nio.charset.StandardCharsets;
import java.util.Set;

public interface GamedosConstants {
    byte[] MARKER = "GAMEDOS ".getBytes(StandardCharsets.UTF_8);

    int DIRECTORY_TRACK = 0;
    int DIRECTORY_SECTOR = 14;
    int DIRECTORY_SIZE = 2;

    int ENTRY_SIZE = 32;
    int ENTRY_TYPE_OFFSET = 0;
    int ENTRY_TRACK_OFFSET = 1;
    int ENTRY_SECTOR_OFFSET = 2;
    int ENTRY_META_OFFSET = 3;
    int ENTRY_NAME_OFFSET = 5;
    int ENTRY_NAME_LENGTH = 26;
    int ENTRY_SECTORS_OFFSET = 31;

    Set<Capability> CAPABILITIES = Set.of(Capability.WRITE_FILES, Capability.CREATE_FILES, Capability.DELETE_FILES);

}
