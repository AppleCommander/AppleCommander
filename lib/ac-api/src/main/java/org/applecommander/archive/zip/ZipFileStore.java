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
import org.applecommander.filestore.Directory;
import org.applecommander.filestore.DisplayColumn;
import org.applecommander.filestore.DisplayColumn.Mode;
import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.FileStore;
import org.applecommander.source.Source;
import org.applecommander.util.Container;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipFileStore implements FileStore {
    private final Source source;
    private final List<FileEntry> entries = new ArrayList<>();

    public ZipFileStore(Source source) {
        Objects.requireNonNull(source);
        this.source = source;

        try (ZipInputStream inputStream = getZipInputStream()) {
            int entryNumber = 0;
            while (true) {
                ZipEntry entry = inputStream.getNextEntry();
                if (entry == null) break;
                entries.add(new ZipFileEntry(this, entryNumber++, entry));
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public ZipInputStream getZipInputStream() throws IOException {
        // FileSource uses Path (converts from File and/or String as well)
        Optional<Path> opt = source.get(Path.class);
        if (opt.isPresent()) {
            return new ZipInputStream(Files.newInputStream(opt.get()));
        }
        return new ZipInputStream(new ByteArrayInputStream(source.readAllBytes().asBytes()));
    }

    @Override
    public String getLabel() {
        return source.getName();
    }

    @Override
    public ZipDirectory getRootDirectory() {
        return new ZipDirectory(this, entries);
    }

    @Override
    public String getPathSeparator() {
        return "/";
    }

    @Override
    public List<DisplayColumn> getDisplayColumns() {
        return DisplayColumn.builder(ZipFileEntry.class)
            .addIntField("Length", FileEntry::getSize, Mode.NATIVE, Mode.DETAIL)
            .addStringField("Method", ZipFileEntry::getMethodName, Mode.DETAIL)
            .addLongField("Size", ZipFileEntry::getCompressedSize, Mode.DETAIL)
            .addPercentField("Ratio", ZipFileEntry::getCompressedSize, ZipFileEntry::getSize, "%2.0f%%", Mode.DETAIL)
            .addFileTimeField("Last Modified", ZipFileEntry::getLastModifiedTime, Mode.NATIVE, Mode.DETAIL)
            .addLongField("CRC-32", ZipFileEntry::getCrc, "%08x", Mode.DETAIL)
            .addStringField("Name", FileEntry::getName, Mode.NATIVE, Mode.DETAIL)
            .toList();
    }

    @Override
    public boolean can(Capability capability) {
        return false;
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, source);
    }
}
