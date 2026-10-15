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
import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.FileStore;
import org.applecommander.filestore.GenericDirectory;
import org.applecommander.shrinkit.MasterHeaderBlock;
import org.applecommander.shrinkit.NuFileArchive;
import org.applecommander.source.Source;
import org.applecommander.ui.DisplayColumn;
import org.applecommander.ui.DisplayMode;
import org.applecommander.util.Container;
import org.applecommander.util.InformationGroup;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ShrinkitFileStore implements FileStore {
    private final Source source;
    private final NuFileArchive archive;
    private final List<FileEntry> files = new ArrayList<>();

    public ShrinkitFileStore(Source source) {
        Objects.requireNonNull(source);
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
        return new GenericDirectory<>(this, files);
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
            .leftString("Name", ShrinkitFileEntry::getName)
            .leftString("Sys. Id", ShrinkitFileEntry::getFileSysIdText, DisplayMode.DETAIL)
            .rightNumber("File Sys. Info", ShrinkitFileEntry::getFileSysInfo, "$%04X", DisplayMode.DETAIL)
            .centerString("Access", ShrinkitFileEntry::getAccessText, DisplayMode.DETAIL)
            .leftString("Kind", ShrinkitFileEntry::getStorageTypeText)
            .centerString("Type", ShrinkitFileEntry::getFileTypeText)
            .rightNumber("Aux. Type", ShrinkitFileEntry::getExtraType, "$%04X")
            .centerDate("Archived", ShrinkitFileEntry::getArchiveWhen)
            .centerDate("Created", ShrinkitFileEntry::getCreateWhen, DisplayMode.DETAIL)
            .centerDate("Modified", ShrinkitFileEntry::getModWhen, DisplayMode.DETAIL)
            .leftString("Format", ShrinkitFileEntry::getThreadFormatText)
            .rightPercent("Size", ShrinkitFileEntry::getCompressedSize, ShrinkitFileEntry::getSize, "%2.0f%%")
            .rightNumber("Un-Length", ShrinkitFileEntry::getSize, "%,d")
            .rightNumber("Data CRC", ShrinkitFileEntry::getDataForkCrc, "$%04X", DisplayMode.DETAIL)
            .rightNumber("Resource CRC", ShrinkitFileEntry::getResourceForkCrc, "$%04X", DisplayMode.DETAIL)
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

    @Override
    public List<InformationGroup> information() {
        MasterHeaderBlock header = archive.getMasterHeaderBlock();
        return InformationGroup.builder("ShrinkIt Archive")
            .item("CRC").value("$%04X (%s)", header.getMasterCrc(), header.isValidCrc() ? "Valid" : "Invalid")
            .item("Total Records").value("%d", header.getTotalRecords())
            .item("Date Created").value(header.getArchiveCreateWhen())
            .item("Date Modified").value(header.getArchiveModWhen())
            .item("Version").value(header.getMasterVersion())
            .item("EOF").value("%d", header.getMasterEof())
            .get(source);
    }
}
