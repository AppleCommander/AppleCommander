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
package org.applecommander.os.gutenberg;

import org.applecommander.device.TrackSectorDevice;
import org.applecommander.filestore.Directory;
import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.FileStore;
import org.applecommander.util.DataBuffer;

import java.util.ArrayList;
import java.util.List;

public class GutenbergDirectory implements Directory, GutenbergConstants{
    private final GutenbergFileStore fileStore;
    private final TrackSectorDevice device;
    private final GutenbergFileHelper helper;

    GutenbergDirectory(GutenbergFileStore fileStore) {
        this.fileStore = fileStore;
        this.device = fileStore.get(TrackSectorDevice.class).orElseThrow();
        this.helper = fileStore.get(GutenbergFileHelper.class).orElseThrow();
    }

    /// Returns the FileStore that this Directory belongs to.
    @Override
    public FileStore getFileStore() {
        return fileStore;
    }

    /// Returns the name of this directory.
    @Override
    public String getName() {
        DataBuffer data = device.readSector(FIRST_CATALOG_TRACK, FIRST_CATALOG_SECTOR);
        return data.getFixedLengthString(DISK_LABEL_OFFSET, DISK_LABEL_LENGTH).trim();
    }

    /// Return all entries that are stored in this directory.
    @Override
    public List<FileEntry> getFiles() {
        List<FileEntry> files = new ArrayList<>();
        helper.follow(FIRST_CATALOG_TRACK, FIRST_CATALOG_SECTOR, (currentTrack,currentSector,data) -> {
           for (int i=FIRST_ENTRY; i<data.limit(); i+= ENTRY_LENGTH) {
               if (data.getUnsignedByte(i+ENTRY_NAME_OFFSET) == 0xa0) {
                   // empty file entry
                   continue;
               }
               GutenbergFileEntry fileEntry = new GutenbergFileEntry(this, currentTrack, currentSector, i);
               if (fileEntry.getFirstTrack() == GutenbergConstants.FIRST_CATALOG_TRACK
                       && fileEntry.getFirstSector() == GutenbergConstants.FIRST_CATALOG_SECTOR) {
                   // prevent the directory itself as being listed as a file
                   continue;
               }
               files.add(fileEntry);
           }
        });
        return files;
    }
}
