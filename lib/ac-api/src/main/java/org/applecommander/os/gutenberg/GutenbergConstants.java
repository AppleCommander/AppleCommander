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
package org.applecommander.os.gutenberg;

import org.applecommander.capability.Capability;

import java.util.Set;

public interface GutenbergConstants {
    int FIRST_CATALOG_TRACK = 17;
    int FIRST_CATALOG_SECTOR = 7;

    int PRIOR_TRACK = 0;
    int PRIOR_SECTOR = 1;
    int CURRENT_TRACK = 2;
    int CURRENT_SECTOR = 3;
    int NEXT_TRACK = 4;
    int NEXT_SECTOR = 5;
    int HEADER_BYTES = 6;

    int DISK_LABEL_OFFSET = 6;
    int DISK_LABEL_LENGTH = 9;

    int ENTRY_LENGTH = 0x10;
    int FIRST_ENTRY = 0x10;
    int ENTRY_NAME_OFFSET = 0;
    int ENTRY_NAME_LENGTH = 12;
    int ENTRY_FIRST_TRACK = 12;
    int ENTRY_FIRST_SECTOR = 13;
    int ENTRY_TYPE = 14;

    int DELETED_FLAG = 0x40;

    Set<Capability> CAPABILITIES = Set.of(Capability.SUPPORTS_DELETED_FILES);
}
