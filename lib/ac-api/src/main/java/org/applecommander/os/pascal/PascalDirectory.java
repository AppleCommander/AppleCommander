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
package org.applecommander.os.pascal;

import org.applecommander.capability.Capability;
import org.applecommander.device.BlockDevice;
import org.applecommander.filestore.Directory;
import org.applecommander.filestore.FileEntry;
import org.applecommander.util.DataBuffer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class PascalDirectory implements Directory, PascalConstants {
    private final PascalFileStore fileStore;
    private final BlockDevice device;

    public PascalDirectory(PascalFileStore fileStore) {
        this.fileStore = fileStore;
        this.device = fileStore.get(BlockDevice.class).orElseThrow();
    }

    public DataBuffer readEntry() {
        return device.readBlock(DIRECTORY_FIRST_BLOCK).slice(0, ENTRY_LENGTH);
    }
    public void modifyEntry(Consumer<DataBuffer> consumer) {
        DataBuffer blockData = device.readBlock(DIRECTORY_FIRST_BLOCK);
        DataBuffer entryData = blockData.slice(0, ENTRY_LENGTH);
        consumer.accept(entryData);
        device.writeBlock(DIRECTORY_FIRST_BLOCK, blockData);
    }

    /// Returns the FileStore that this Directory belongs to.
    @Override
    public PascalFileStore getFileStore() {
        return fileStore;
    }

    /// Returns the name of this directory.
    @Override
    public String getName() {
        return readEntry().getPascalString(ENTRY_NAME);
    }
    /// Sets the name of this directory. Per the Apple Pascal documentation:
    ///
    /// > A diskette's volume name must be seven or fewer characters long and may not
    /// > contain an equals sign (`=`), dollar sign (`$`), question mark (`?`) or
    /// > comma(`,`).
    ///
    /// This method only errors if, after applying the rules, the string is empty.
    public void setName(String name) {
        StringBuffer sb = new StringBuffer();
        for (char ch : name.toUpperCase().toCharArray()) {
            if (!Character.isLetter(ch) || ch == '$' || ch == ',' || ch == '=' || ch == '?') {
                continue;
            }
            sb.append(ch);
        }
        if (sb.isEmpty()) {
            throw new IllegalArgumentException("Illegal name '" + name + "'");
        }
        modifyEntry(entry -> entry.putPascalString(ENTRY_NAME, VOLUME_NAME_LENGTH, sb.toString(), false));
    }

    public int getBlocksOnDisk() {
        return readEntry().getUnsignedShort(VOLUME_BLOCKS_ON_DISK);
    }

    public int getFilesOnDisk() {
        return readEntry().getUnsignedShort(VOLUME_FILES_ON_DISK);
    }
    public void updateFilesOnDisk() {
        modifyEntry(entry -> entry.putUnsignedShort(VOLUME_FILES_ON_DISK, getFiles().size()));
    }

    public Date getMostRecentDate() {
        return readEntry().getPascalDate(VOLUME_RECENT_DATE);
    }
    public void setMostRecentDate(Date date) {
        modifyEntry(entry -> entry.putPascalDate(VOLUME_RECENT_DATE, date));
    }

    /// Return all entries that are stored in this directory.
    @Override
    public List<FileEntry> getFiles() {
        List<FileEntry> entries = new ArrayList<>();
        DataBuffer dir = device.readRange(DIRECTORY_FIRST_BLOCK, DIRECTORY_BLOCKS);
        for (int offset = ENTRY_LENGTH; offset < dir.limit(); offset += ENTRY_LENGTH) {
            if (dir.getUnsignedByte(offset+ENTRY_NAME) == 0) {
                // End of directory
                break;
            }
            entries.add(new PascalFileEntry(this, offset));
        }
        return entries;
    }

    /// Helps identify capabilities of the existing component.
    @Override
    public boolean can(Capability capability) {
        return false;
    }
}
