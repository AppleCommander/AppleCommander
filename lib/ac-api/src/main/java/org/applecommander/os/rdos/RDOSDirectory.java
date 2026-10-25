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
package org.applecommander.os.rdos;

import org.applecommander.capability.Capability;
import org.applecommander.device.BlockDevice;
import org.applecommander.filestore.Directory;
import org.applecommander.filestore.FileEntry;
import org.applecommander.util.DataBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class RDOSDirectory implements Directory, RDOSConstants {
    private final RDOSFileStore fileStore;
    private final BlockDevice device;

    RDOSDirectory(RDOSFileStore fileStore) {
        Objects.requireNonNull(fileStore);
        this.fileStore = fileStore;
        this.device = fileStore.get(BlockDevice.class).orElseThrow();
    }

    /// Get the parent directory. If this is the root directory, it returns empty.
    @Override
    public Optional<Directory> getParent() {
        return Optional.empty();
    }

    /// Returns the FileStore that this Directory belongs to.
    @Override
    public RDOSFileStore getFileStore() {
        return fileStore;
    }

    /// Returns the name of this directory.
    @Override
    public String getName() {
        // Just using the RDOS file entry and not the Krakowicz entry
        int block = fileStore.getAdapter().firstDirectoryBlock;
        return device.readBlock(block).getFixedLengthString(NAME_OFFSET, NAME_LENGTH);
    }

    /// Return all entries that are stored in this directory.
    @Override
    public List<FileEntry> getFiles() {
        List<FileEntry> entries = new ArrayList<>();
        int firstBlock = fileStore.getAdapter().firstDirectoryBlock;
        int lastBlock = fileStore.getAdapter().lastDirectoryBlock;
        for (int block=firstBlock; block<=lastBlock; block++) {
            DataBuffer data = device.readBlock(block);
            for (int offset=0; offset<data.limit(); offset+= ENTRY_LENGTH) {
                if (data.getUnsignedByte(offset) >= 0x80) {
                    entries.add(new RDOSFileEntry(this, block, offset));
                }
            }
        }
        return entries;
    }

    /// Helps identify capabilities of the existing component.
    @Override
    public boolean can(Capability capability) {
        return false;
    }
}
