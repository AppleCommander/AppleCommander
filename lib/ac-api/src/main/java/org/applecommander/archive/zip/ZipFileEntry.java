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

import org.applecommander.filestore.ContentType;
import org.applecommander.filestore.Directory;
import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.FileStore;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.FileMagic;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipFileEntry implements FileEntry {
    private final ZipFileStore fileStore;
    private final int entryNumber;
    private final ZipEntry entry;

    public ZipFileEntry(ZipFileStore fileStore, int entryNumber, ZipEntry entry) {
        Objects.requireNonNull(fileStore);
        Objects.requireNonNull(entry);
        this.fileStore = fileStore;
        this.entryNumber = entryNumber;
        this.entry = entry;
    }

    @Override
    public Directory getParent() {
        return null;
    }

    @Override
    public boolean isDeleted() {
        return false;
    }

    @Override
    public FileStore getFileStore() {
        return fileStore;
    }

    @Override
    public String getName() {
        return entry.getName();
    }

    @Override
    public long getSize() {
        return entry.getSize();
    }

    @Override
    public String getFiletype() {
        int pos = entry.getName().lastIndexOf('.');
        if (pos > 0) {
            return entry.getName().substring(pos + 1);
        }
        return "?";
    }

    public FileTime getLastModifiedTime() {
        return entry.getLastModifiedTime();
    }

    public long getCrc() {
        return entry.getCrc();
    }

    public long getCompressedSize() {
        return entry.getCompressedSize();
    }

    public int getMethod() {
        return entry.getMethod();
    }
    public String getMethodName() {
        return switch (getMethod()) {
            case ZipEntry.STORED -> "Stored";
            case ZipEntry.DEFLATED -> "Deflated";
            default -> String.format("Method %d", entry.getMethod());
        };
    }

    @Override
    public DataBuffer getDataFork() {
        try (ZipInputStream inputStream = fileStore.getZipInputStream()) {
            int n = 0;
            while (true) {
                ZipEntry temp = inputStream.getNextEntry();
                if (temp == null) break;
                // Since the ZipEntry doesn't have an equals method, and we can't trust name (can have multiple
                // entries with the same name), we track the entry number that we read from originally.
                if (n == this.entryNumber) {
                    return DataBuffer.wrap(inputStream.readAllBytes());
                }
                n++;
            }
            throw new IOException("Unable to locate zip entry for " + entry.getName());
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public ContentType getContentType() {
        return FileMagic.identifyContentType(getName());
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, fileStore, entry);
    }
}
