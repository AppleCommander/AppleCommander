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
package org.applecommander.archive.aaf;

import org.applecommander.filestore.Directory;
import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.FileStore;
import org.applecommander.transfer.ProdosAttributes;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;

import java.util.Objects;
import java.util.Optional;

public class AppleArchiveFormatFileEntry implements FileEntry {
    private final AppleArchiveFormatFileStore fileStore;
    private final String name;
    private String text;

    public AppleArchiveFormatFileEntry(AppleArchiveFormatFileStore fileStore, String name) {
        Objects.requireNonNull(fileStore);
        Objects.requireNonNull(name);
        this.fileStore = fileStore;
        this.name = name;
        this.text = "";
    }

    void appendText(String text) {
        Objects.requireNonNull(text);
        if (!text.endsWith("\n")) {
            text += "\n";
        }
        this.text += text;
    }

    public String getText() {
        return text;
    }

    /// The `Directory` that holds this file.
    @Override
    public Directory getParent() {
        return fileStore.getRootDirectory();
    }

    /// Indicates if this `FileEntry` is deleted.
    @Override
    public boolean isDeleted() {
        return false;
    }

    /// Returns the `FileStore` that this `Entry` originates from.
    @Override
    public FileStore getFileStore() {
        return fileStore;
    }

    /// Returns the name of this entry: either a directory name or a file name.
    /// This does not contain any directory components -- strictly the name of this entry.
    /// Note that depending on context, this may be a computed field.
    @Override
    public String getName() {
        return name;
    }

    /// Returns the size, in bytes, of this item.
    /// It may be approximate (based off a sector count, for instance).
    @Override
    public long getSize() {
        return text.length();
    }

    /// Return the file's data.
    /// This does not include any metadata that may be embedded with the file.
    @Override
    public DataBuffer getDataFork() {
        return DataBuffer.wrap(text.getBytes());
    }

    /// Interpret the `FileEntry` as if it were on a ProDOS volume.
    /// The intention is that ProDOS ended up being the common denominator in many tools and formats,
    /// so this allows each file store to manage the interpretation itself.
    @Override
    public ProdosAttributes getProdosAttributes() {
        return ProdosAttributes.builder()
                .TXT()
                .unlocked()
                .dataFork(text.getBytes())
                .get();
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, fileStore);
    }
}
