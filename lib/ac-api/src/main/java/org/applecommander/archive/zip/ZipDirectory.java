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
package org.applecommander.archive.zip;

import org.applecommander.filestore.Directory;
import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.FileStore;
import org.applecommander.source.Source;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ZipDirectory implements Directory {
    private final FileStore fileStore;
    private final List<FileEntry> entries;

    public ZipDirectory(FileStore fileStore, List<FileEntry> entries) {
        Objects.requireNonNull(fileStore);
        Objects.requireNonNull(entries);
        this.fileStore = fileStore;
        this.entries = entries;
    }

    @Override
    public Optional<Directory> getParent() {
        return Optional.empty();
    }

    @Override
    public FileStore getFileStore() {
        return fileStore;
    }

    @Override
    public String getName() {
        return fileStore.get(Source.class).orElseThrow().getName();
    }

    @Override
    public List<FileEntry> getFiles() {
        return entries;
    }
}
