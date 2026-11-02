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
package org.applecommander.archive.binscii;

import org.applecommander.capability.Capability;
import org.applecommander.filestore.Directory;
import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.FileStore;
import org.applecommander.filestore.GenericDirectory;
import org.applecommander.shrinkit.CRC16;
import org.applecommander.source.Source;
import org.applecommander.ui.DisplayColumn;
import org.applecommander.ui.DisplayMode;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.InformationGroup;

import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BinsciiFileStore implements FileStore, BinsciiConstants {
    private final Source source;
    private final List<FileEntry> files;

    public BinsciiFileStore(Source source) {
        Objects.requireNonNull(source);
        this.source = source;

        BinsciiFileEntry fileEntry = readContents();
        this.files = List.of(fileEntry);
    }

    private BinsciiFileEntry readContents() {
        try (
            ByteArrayInputStream is = new ByteArrayInputStream(source.readAllBytes().asBytes());
            InputStreamReader isreader = new InputStreamReader(is);
            BufferedReader reader = new BufferedReader(isreader);
        ) {
            FileState state = FileState.SEEKING;
            BinsciiEncoderDecoder binscii = null;
            String name = null;
            DataBuffer header = null;
            DataBuffer filedata = null;
            int offset = -1;
            int bytesToRead = -1;
            final CRC16 crc = new CRC16();
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                line = line.trim();
                switch (state) {
                    case SEEKING -> {
                        if (SENTINAL.equals(line)) {
                            state = FileState.ALPHABET;
                        }
                    }
                    case ALPHABET -> {
                        binscii = new BinsciiEncoderDecoder(line);
                        state = FileState.HEADER;
                    }
                    case HEADER -> {
                        if (name == null) {
                            int length = line.charAt(0) - '@';
                            name = line.substring(1, length + 1);
                        }
                        header = binscii.decode(line.substring(16));
                        offset = header.getUnsignedShort3(OFFSET_SEGSTART);
                        if (bytesToRead == -1) {
                            int filesize = header.getUnsignedShort3(OFFSET_FILESIZE);
                            filedata = DataBuffer.create(filesize);
                            bytesToRead = filesize;
                        }
                        crc.reset();
                        state = FileState.DATA;
                    }
                    case DATA -> {
                        if (line.length() == 64) {
                            DataBuffer data = binscii.decode(line);
                            int bytecount = Math.min(data.limit(), filedata.limit() - offset);
                            filedata.put(offset, data.slice(0, bytecount));
                            offset += bytecount;
                            bytesToRead -= bytecount;
                            crc.update(data.asBytes());
                        } else if (line.length() == 4) {
                            // segment checksum
                            DataBuffer data = binscii.decode(line);
                            int expectedCRC = data.getUnsignedShort(0);
                            if (crc.getValue() != expectedCRC) {
                                throw new IOException("CRC mismatch");
                            }
                            if (bytesToRead == 0) {
                                return new BinsciiFileEntry(this, name, header, filedata);
                            }
                        } else {
                            state = FileState.SEEKING;
                            if (SENTINAL.equals(line)) {
                                state = FileState.ALPHABET;
                            }
                        }
                    }
                }
            }
            throw new IOException("unable to read BinSCII file");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    /// Get the type of file store. This is a generic name, such as "RDOS 2.1" or "ShrinkIt".
    @Override
    public String getType() {
        return "BinSCII";
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
        return "";
    }

    /// Yields a list of columns to be used in this display mode.
    @Override
    public List<DisplayColumn> getDisplayColumns() {
        return DisplayColumn.builder(BinsciiFileEntry.class)
                .left("Name").string(BinsciiFileEntry::getName).done()
                .right("Size").integer(BinsciiFileEntry::getSize, "%,d").done()
                .center("Access").string(BinsciiFileEntry::getAccessText).done()
                .center("File Type").string(BinsciiFileEntry::getFileTypeText).done()
                .right("Aux. Type").integer(BinsciiFileEntry::getAuxType, "$%04X").done()
                .right("Blocks").integer(BinsciiFileEntry::getTotalBlocks, "%,d").done()
                .center("Created").dateAndTime(BinsciiFileEntry::getCreated).done()
                .center("Modified").dateAndTime(BinsciiFileEntry::getModified).done()
                .left("Storage Type").string(BinsciiFileEntry::getStorageTypeText).mode(DisplayMode.DETAIL).done()
                .right("CRC").integer(BinsciiFileEntry::getChecksum, "$%04X").mode(DisplayMode.DETAIL).done()
                .center("CRC Valid?").yesNo(BinsciiFileEntry::checksumMatches).mode(DisplayMode.DETAIL).done()
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
        return InformationGroup.builder("BinSCII Archive")
                .item("Files").value(files.size())      // Expected to be 1
                .get(source);
    }

    private enum FileState {
        SEEKING, ALPHABET, HEADER, DATA
    }
}
