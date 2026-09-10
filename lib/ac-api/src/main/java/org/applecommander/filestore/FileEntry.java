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
package org.applecommander.filestore;

import org.applecommander.capability.Capability;

/**
 * A FileEntry represents a single file on disk.
 *
 * @see Entry
 * @see DirectoryEntry
 */
public interface FileEntry extends Entry {
    /**
     * Return the file's data.
     * This does not include any metadata that may be embedded with the file.
     */
    byte[] getDataFork();
    /**
     * Return the resource fork data.
     * This may not be supported and may throw an exception.
     * @see Capability
     */
    byte[] getResourceFork();
    /**
     * Store the file's data.
     * This does not include any metadata that may be embedded with the file.
     */
    void setDataFork(byte[] data);
    /**
     * Store the resource fork data.
     * This may not be supported and may throw an exception.
     * @see Capability
     */
    void setResourceFork(byte[] data);
}
