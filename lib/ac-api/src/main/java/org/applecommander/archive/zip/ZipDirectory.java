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

import org.applecommander.capability.Capability;
import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.FileStore;
import org.applecommander.filestore.WritableDirectory;
import org.applecommander.source.Source;
import org.applecommander.transfer.ProdosAttributes;

import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Objects;

public class ZipDirectory implements WritableDirectory {
    private final ZipFileStore fileStore;
    private final List<FileEntry> entries;

    public ZipDirectory(ZipFileStore fileStore, List<FileEntry> entries) {
        Objects.requireNonNull(fileStore);
        Objects.requireNonNull(entries);
        this.fileStore = fileStore;
        this.entries = entries;
    }

    @Override
    public boolean can(Capability capability) {
        return false;
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

    @Override
    public ZipFileEntry createFrom(ProdosAttributes prodosAttributes) {
        ZipFileEntry fileEntry = createFile(prodosAttributes.name());
        prodosAttributes.dataFork().ifPresent(fileEntry::setDataFork);
        if (prodosAttributes.creation() != null) {
            fileEntry.setCreationTime(FileTime.from(prodosAttributes.creation().toInstant()));
        }
        if (prodosAttributes.modified() != null) {
            fileEntry.setLastModifiedTime(FileTime.from(prodosAttributes.modified().toInstant()));
        }
        return fileEntry;
    }

    @Override
    public ZipFileEntry createFile(String fileName) {
        ZipFileEntry fileEntry = new ZipFileEntry(fileStore, fileName);
        entries.add(fileEntry);
        fileStore.get(Source.class).ifPresent(source -> source.hasChanged(true));
        return fileEntry;

    }

    @Override
    public void deleteFile(FileEntry fileEntry) {
        entries.remove(fileEntry);
        fileStore.get(Source.class).ifPresent(source -> source.hasChanged(true));
    }
}
