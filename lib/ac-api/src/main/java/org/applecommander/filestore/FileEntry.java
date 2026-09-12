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

/**
 * A FileEntry represents a single file on disk.
 */
public interface FileEntry extends Container {
    /**
     * The parent <code>DirectoryEntry</code>, if applicable. Can return <code>null</code>.
     */
    Directory getParent();
    /**
     * Indicates if this <code>Entry</code> is deleted.
     */
    boolean isDeleted();
    /**
     * Returns the <code>FileStore</code> that this <code>Entry</code> originates from.
     */
    FileStore getFileStore();
    /**
     * Returns the name of this entry: either a directory name or a file name.
     * This does not contain any directory components -- strictly the name of this entry.
     * Note that depending on context, this may be a computed field.
     */
    String getName();
    /**
     * Allows the name of this entry to be changed (depending on the {@link Capability}
     * allowed by the underlying {@link FileStore}).
     */
    void setName(String name);
    /**
     * Returns the size, in bytes, of this item.
     * It may be approximate (based off a sector count, for instance).
     */
    int getSize();
    /**
     * Return the textual representation of the file type, such as "BAS" or "A" for Applesoft.
     */
    String getFiletype();
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
