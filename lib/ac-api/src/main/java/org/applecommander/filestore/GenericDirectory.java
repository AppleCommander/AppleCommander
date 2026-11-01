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

import java.util.List;

/// Provides a generic `Directory` structure for simple `FileStore`s.
public class GenericDirectory<FS extends FileStore> implements Directory {
    private final FS fileStore;
    private final List<FileEntry> files;

    public GenericDirectory(FS fileStore, List<FileEntry> files) {
        this.fileStore = fileStore;
        this.files = files;
    }

    /// Returns the FileStore that this Directory belongs to.
    @Override
    public FS getFileStore() {
        return fileStore;
    }

    /// Returns the name of this directory.
    @Override
    public String getName() {
        return fileStore.getLabel();
    }

    /// Return all entries that are stored in this directory.
    @Override
    public List<FileEntry> getFiles() {
        return files;
    }
}
