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
package org.applecommander.archive.shrinkit;

import org.applecommander.capability.Capability;
import org.applecommander.filestore.Directory;
import org.applecommander.filestore.DisplayColumn;
import org.applecommander.filestore.DisplayColumn.Alignment;
import org.applecommander.filestore.DisplayColumn.Mode;
import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.FileStore;
import org.applecommander.shrinkit.NuFileArchive;
import org.applecommander.source.Source;
import org.applecommander.util.Container;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShrinkitFileStore implements FileStore {
    private final Source source;
    private final NuFileArchive archive;
    private final List<FileEntry> files = new ArrayList<>();

    public ShrinkitFileStore(Source source) {
        this.source = source;
        try {
            this.archive = new NuFileArchive(new ByteArrayInputStream(source.readAllBytes().asBytes()));

            archive.getHeaderBlocks().forEach(header -> {
                files.add(new ShrinkitFileEntry(this, header));
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    @Override
    public String getLabel() {
        return source.getName();
    }

    @Override
    public Directory getRootDirectory() {
        return new ShrinkitDirectory(this, files);
    }

    @Override
    public String getPathSeparator() {
        // Default to the ProDOS separator
        if (archive.getHeaderBlocks().isEmpty()) {
            return "/";
        }
        // Otherwise, the first file defines the separator
        return archive.getHeaderBlocks().getFirst().getFileSystemSeparator();
    }

    @Override
    public List<DisplayColumn> getDisplayColumns() {
        return DisplayColumn.builder(ShrinkitFileEntry.class)
            .addStringField("Name", ShrinkitFileEntry::getName)
            .addStringField("Sys. Id", ShrinkitFileEntry::getFileSysIdText, Mode.DETAIL)
            .addIntField("File Sys. Info", ShrinkitFileEntry::getFileSysInfo, "$%04X", Mode.DETAIL)
            .addStringField("Access", Alignment.CENTER, ShrinkitFileEntry::getAccessString, Mode.DETAIL)
            .addStringField("Kind", ShrinkitFileEntry::getStorageTypeString)
            .addStringField("Type", Alignment.CENTER, ShrinkitFileEntry::getFiletype)
            .addLongField("Aux. Type", ShrinkitFileEntry::getExtraType, "$%04X")
            .addDateField("Archived", ShrinkitFileEntry::getArchiveWhen)
            .addDateField("Created", ShrinkitFileEntry::getCreateWhen, Mode.DETAIL)
            .addDateField("Modified", ShrinkitFileEntry::getModWhen, Mode.DETAIL)
            .addStringField("Format", ShrinkitFileEntry::getThreadFormat)
            .addPercentField("Size", ShrinkitFileEntry::getCompressedSize, ShrinkitFileEntry::getSize, "%2.0f%%")
            .addLongField("Un-Length", ShrinkitFileEntry::getSize)
            .addIntField("Data CRC", ShrinkitFileEntry::getDataForkCrc, "$%04X", Mode.DETAIL)
            .addIntField("Resource CRC", ShrinkitFileEntry::getResourceForkCrc, "$%04X", Mode.DETAIL)
            .toList();
    }

    @Override
    public boolean can(Capability capability) {
        return capability == Capability.SUPPORTS_RESOURCE_FORKS;
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, source, archive);
    }
}
