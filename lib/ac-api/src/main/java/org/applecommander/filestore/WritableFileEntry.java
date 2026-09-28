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
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;

/**
 * A WritableFileEntry represents a single, writable file on disk.
 */
public interface WritableFileEntry extends FileEntry, Container {
    /**
     * Allows the name of this entry to be changed (depending on the {@link Capability}
     * allowed by the underlying {@link FileStore}).
     */
    void setName(String name);
    /**
     * Store the file's data.
     * This does not include any metadata that may be embedded with the file.
     */
    void setDataFork(DataBuffer data);
    /**
     * Store the resource fork data.
     * This may not be supported and may throw an exception.
     * @see Capability
     */
    void setResourceFork(DataBuffer data);
}
