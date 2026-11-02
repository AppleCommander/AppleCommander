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
package org.applecommander.archive.binaryii;

import org.applecommander.capability.Capability;
import org.applecommander.filestore.Directory;
import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.FileStore;
import org.applecommander.filestore.GenericDirectory;
import org.applecommander.source.Source;
import org.applecommander.ui.DisplayColumn;
import org.applecommander.ui.DisplayMode;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.InformationGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BinaryIIFileStore implements FileStore, BinaryIIConstants {
    private final Source source;
    private final List<FileEntry> files;

    BinaryIIFileStore(Source source) {
        this.source = source;
        this.files = List.copyOf(readContents());
    }

    private List<FileEntry> readContents() {
        List<FileEntry> files = new ArrayList<>();
        int offset = 0;
        while (offset < source.getSize()) {
            DataBuffer header = source.readBytes(offset, BLOCK_SIZE);
            int eof = header.getUnsignedByte(GSOS_EOF) << 24 | header.getUnsignedShort3(EOF);
            if (eof == 0) continue;
            int phantomFlag = header.getUnsignedByte(PHANTOM_FILE_FLAG);
            if (phantomFlag > 0) continue;
            int dataChunks = (eof + BLOCK_SIZE - 1) / BLOCK_SIZE;
            DataBuffer data = source.readBytes(offset+BLOCK_SIZE, dataChunks*BLOCK_SIZE);
            files.add(new BinaryIIFileEntry(this, header, data.slice(0, eof)));
            offset += (dataChunks + 1) * BLOCK_SIZE;
        }
        return files;
    }

    /// Get the type of file store. This is a generic name, such as "RDOS 2.1" or "ShrinkIt".
    @Override
    public String getType() {
        return "Binary II";
    }

    /// The label is a mechanism to distinguish multiple file stores in a single file.
    /// For instance, UniDOS has two file stores in the file, so it would have two
    /// different labels such as "Disk 1" and "Disk 2".
    @Override
    public String getLabel() {
        return source.getName();
    }

    /// All FileStores support a "root" directory that references all files.
    @Override
    public Directory getRootDirectory() {
        return new GenericDirectory<>(this, files);
    }

    /// This is the path separator character.
    @Override
    public String getPathSeparator() {
        return "/";
    }

    /// Yields a list of columns to be used in this display mode.
    @Override
    public List<DisplayColumn> getDisplayColumns() {
        return DisplayColumn.builder(BinaryIIFileEntry.class)
            .left("Filename").string(BinaryIIFileEntry::getName).done()
            .center("Type").string(BinaryIIFileEntry::getFileTypeText).done()
            .right("Blocks").integer(BinaryIIFileEntry::getBlocksUsed, "%d").done()
            .left("Storage Type").string(BinaryIIFileEntry::getStorageTypeText).mode(DisplayMode.DETAIL).done()
            .center("Access").string(BinaryIIFileEntry::getAccessText).mode(DisplayMode.DETAIL).done()
            .left("OS Type").string(BinaryIIFileEntry::getOSTypeText).mode(DisplayMode.DETAIL).done()
            .center("Modified").dateAndTime(BinaryIIFileEntry::getModificationDate).done()
            .center("Created").dateAndTime(BinaryIIFileEntry::getCreateDate).done()
            .right("Length").integer(BinaryIIFileEntry::getSize, "$%04X").done()
            .right("Subtype").integer(BinaryIIFileEntry::getAuxType, "$%04X").done()
            .left("Data Flags").string(BinaryIIFileEntry::getDataFlagsText).mode(DisplayMode.DETAIL).done()
            .center("Version").integer(BinaryIIFileEntry::getVersion, "%d").mode(DisplayMode.DETAIL).done()
            .toList();
    }

    /// Helps identify capabilities of the existing component.
    @Override
    public boolean can(Capability capability) {
        return false;
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, source);
    }

    @Override
    public List<InformationGroup> information() {
        return InformationGroup.builder("Binary II Archive")
            .item("Files").value(files.size())
            .get(source);
    }
}
