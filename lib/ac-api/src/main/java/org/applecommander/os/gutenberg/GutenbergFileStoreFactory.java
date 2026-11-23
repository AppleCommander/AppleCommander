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

import org.applecommander.device.SectorOrder;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.filestore.FileStoreFactory;
import org.applecommander.util.DataBuffer;

public class GutenbergFileStoreFactory implements FileStoreFactory, GutenbergConstants {
    /// The name of this factory. Usually the name and file extensions.
    @Override
    public String getName() {
        return "Gutenberg";
    }

    /// Inspect the given `Source`, storing any results or messages in the context.
    @Override
    public void inspect(Context ctx) {
        ctx.trackSectorDevice()
                .check16Sector(device -> {
                    device = device.request(SectorOrder.DOS);
                    if (check(device)) {
                        ctx.fileStores.add(new GutenbergFileStore(device));
                    }
                });
    }

    public boolean check(TrackSectorDevice device) {
        boolean good;
        DataBuffer data = device.readSector(FIRST_CATALOG_TRACK, FIRST_CATALOG_SECTOR);
        final int tracksOnDisk = device.getGeometry().tracksOnDisk();
        final int sectorsPerTrack = device.getGeometry().sectorsPerTrack();
        for (int i=ENTRY_LENGTH-1; i<data.limit(); i+= ENTRY_LENGTH) {
            // Check for the CR at every 16th byte.
            if (data.getUnsignedByte(i) != 0x8d) return false;
        }
        // Verify T/S links:
        int priorTrack = data.getUnsignedByte(PRIOR_TRACK);
        int priorSector = data.getUnsignedByte(PRIOR_SECTOR);
        int currentTrack = data.getUnsignedByte(CURRENT_TRACK);
        int currentSector = data.getUnsignedByte(CURRENT_SECTOR);   // high bit set if first DIR sector
        int nextTrack = data.getUnsignedByte(NEXT_TRACK);           // high bit set if last DIR sector
        int nextSector = data.getUnsignedByte(NEXT_SECTOR);
        good = priorTrack < tracksOnDisk && priorSector < sectorsPerTrack
                && currentTrack < tracksOnDisk && (currentSector & 0x7f) < sectorsPerTrack
                && (nextTrack & 0x7f) < tracksOnDisk && nextSector < sectorsPerTrack
                && isHighAscii(data, DISK_LABEL_OFFSET, DISK_LABEL_LENGTH);
        if (!good) return false;
        // Since we are only checking the first catalog sector, verify the current links are what we expect
        good = currentTrack == FIRST_CATALOG_TRACK
                && (currentSector & 0x7f) == FIRST_CATALOG_SECTOR;
        if (!good) return false;
        // Check that the file entries are as expected
        for (int i=FIRST_ENTRY; i<data.limit(); i+= ENTRY_LENGTH) {
            if (data.getUnsignedByte(i) == 0xa0) {
                // Unused file entry
                continue;
            }
            int firstTrack = data.getUnsignedByte(i+ ENTRY_FIRST_TRACK);    // sometimes if == 0x40, file is deleted
            int firstSector = data.getUnsignedByte(i+ ENTRY_FIRST_SECTOR);  // if == 0x40, file is deleted
            good = isHighAscii(data, i+ENTRY_NAME_OFFSET, ENTRY_NAME_LENGTH)
                    && (firstTrack < tracksOnDisk || firstTrack == DELETED_FLAG)
                    && (firstSector < sectorsPerTrack || firstSector == DELETED_FLAG)
                    && isHighAscii(data, i+ENTRY_TYPE, 1);
            if (!good) return false;
        }
        return good;
    }

    public boolean isHighAscii(DataBuffer data, int start, int length) {
        for (int i=start; i<start+length; i++) {
            if (data.getUnsignedByte(i) < 0xa0) return false;
        }
        return true;
    }
}
