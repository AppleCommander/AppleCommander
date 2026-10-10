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
package org.applecommander.os.gamedos;

import org.applecommander.capability.Capability;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.exception.DirectoryFullException;
import org.applecommander.exception.FileExistsException;
import org.applecommander.filestore.*;
import org.applecommander.transfer.ProdosAttributes;
import org.applecommander.util.DataBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GamedosDirectory implements WritableDirectory, GamedosConstants {
    private final GamedosFileStore fileStore;
    private final TrackSectorDevice device;

    public GamedosDirectory(GamedosFileStore fileStore) {
        this.fileStore = fileStore;
        this.device = fileStore.get(TrackSectorDevice.class).orElseThrow();
    }

    @Override
    public boolean can(Capability capability) {
        return CAPABILITIES.contains(capability);
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
        return fileStore.getLabel();
    }

    @Override
    public List<FileEntry> getFiles() {
        List<FileEntry> files = new ArrayList<>();
        for (int i=0; i<DIRECTORY_SIZE; i++) {
            DataBuffer data = device.readSector(DIRECTORY_TRACK, DIRECTORY_SECTOR+i);
            for (int offset=0; offset<data.limit(); offset+= GamedosFileEntry.ENTRY_SIZE) {
                if (data.getUnsignedByte(offset) != 0) {
                    files.add(new GamedosFileEntry(this, DIRECTORY_SECTOR+i, offset));
                }
            }
        }
        return files;
    }

    /// Create a file. By definition, any file created is a `WritableFileEntry`.
    @Override
    public GamedosFileEntry createFile(String fileName) {
        for (int i=0; i<DIRECTORY_SIZE; i++) {
            DataBuffer data = device.readSector(DIRECTORY_TRACK, DIRECTORY_SECTOR+i);
            for (int offset=0; offset<data.limit(); offset+= GamedosFileEntry.ENTRY_SIZE) {
                GamedosFileEntry fileEntry = new GamedosFileEntry(this, DIRECTORY_SECTOR+i, offset);
                if (data.getUnsignedByte(offset) == 0) {
                    fileEntry.setName(fileName);
                    return fileEntry;
                }
                else if (fileName.equalsIgnoreCase(fileEntry.getName())) {
                    throw new FileExistsException("file '%s' already exists", fileName);
                }
            }
        }
        throw new DirectoryFullException("there are no more directory entries available on this disk");
    }

    /// Create a file given the supplied ProDOS attributes. This is expected to handle
    /// setting all appropriate `FileEntry` attributes that align. File type should be
    /// converted, dates applied, etc.
    @Override
    public WritableFileEntry createFrom(ProdosAttributes prodosAttributes) {
        GamedosFileEntry fileEntry = createFile(prodosAttributes.name());
        fileEntry.setMeta(prodosAttributes.auxType());
        String fileType = switch (prodosAttributes.fileType()) {
            case ProdosAttributes.BAS -> {
                // Meta = end of code addr for Applesoft
                prodosAttributes.dataFork().ifPresent(dataFork -> fileEntry.setMeta(dataFork.limit()));
                yield "A";
            }
            case ProdosAttributes.TXT -> "T";
            case ProdosAttributes.INT -> "I";
            default -> "B";
        };
        fileEntry.setFiletype(fileType);
        prodosAttributes.dataFork().ifPresent(fileEntry::setDataFork);
        prodosAttributes.resourceFork().ifPresent(fileEntry::setResourceFork);
        return fileEntry;
    }

    /// Delete a file.
    @Override
    public void deleteFile(FileEntry fileEntry) {
        GamedosFileEntry file = (GamedosFileEntry) fileEntry;
        // Just need to reset the file type to $00.
        file.modifyEntry(data -> data.putByte(0,0));
    }
}
