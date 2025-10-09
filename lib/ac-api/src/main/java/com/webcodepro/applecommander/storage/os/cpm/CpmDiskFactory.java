/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2025 by Robert Greene and others
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
package com.webcodepro.applecommander.storage.os.cpm;

import com.webcodepro.applecommander.storage.DiskFactory;
import org.applecommander.device.*;
import org.applecommander.hint.Hint;
import org.applecommander.util.DataBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Test this disk for a likely CP/M filesystem.
 * @see <a href="https://www.seasip.info/Cpm/format22.html">CP/M 2.2</a>
 * @see <a href="https://www.seasip.info/Cpm/format31.html">CP/M 3.1</a>
 */
public class CpmDiskFactory implements DiskFactory {
    @Override
    public void inspect(Context ctx) {
        List<CpmFormatDisk> candidates = new ArrayList<>();
        ctx.trackSectorDevice()
                .include16Sector(Hint.DOS_SECTOR_ORDER)
                .get()
                .forEach(device -> {
                    BlockDevice blockDevice = new TrackSectorToBlockAdapter(
                            SkewedTrackSectorDevice.dosToCpmSkew(device),
                            TrackSectorToBlockAdapter.BlockStyle.CPM);
                    CpmFormatDisk disk = new CpmFormatDisk(ctx.source.getName(), blockDevice);
                    candidates.add(disk);
                });
        // Look for the one with the largest directory entry
        int maxFileCount = -1;
        CpmFormatDisk selectedDisk = null;
        for (CpmFormatDisk candidate : candidates) {
            int count = check(candidate);
            if (count != -1 && count > maxFileCount) {
                selectedDisk = candidate;
                maxFileCount = count;
            }
        }
        if (selectedDisk != null) {
            ctx.disks.add(selectedDisk);
        }
    }

    public int check(CpmFormatDisk disk) {
        DataBuffer entries = DataBuffer.wrap(disk.readCpmFileEntries());
        int offset = 0;
        int count = 0;
        while (offset < entries.limit()) {
            // Check if this is an empty directory entry (and ignore it)
            int e5count = 0;
            for (int i=0; i<CpmFileEntry.ENTRY_LENGTH; i++) {
                e5count+= entries.getUnsignedByte(offset+i) == 0xe5 ? 1 : 0;
            }
            if (e5count == CpmFileEntry.ENTRY_LENGTH) {
                // If we find a fully blank entry, assume we are done. Should assist with sector order determination.
                return count;
            }
            // Not all bytes were 0xE5
            // Check user number. Should be 0-15 or 0xE5
            int userNumber = entries.getUnsignedByte(offset);
            if (userNumber > 0x1f && userNumber != 0xe5) return -1;
            // Validate filename has highbit off and is a character
            for (int i=0; i<8; i++) {
                int ch = entries.getUnsignedByte(offset+1+i);
                if (ch < 0x20 || ch > 127) return -1;
            }
            // Extent should be 0-31 (low = 0-31 and high = 0)
            int exLow = entries.getUnsignedByte(offset+0xc);
            int exHighS2 = entries.getUnsignedByte(offset+0xe);
            if (exLow > 31 || exHighS2 > 0) return -1;
            // Number of used records cannot exceed 0x80
            int numberOfRecords = entries.getUnsignedByte(offset+0xf);
            if (numberOfRecords > 0x80) return -1;
            count++;
            // Next entry
            offset+= CpmFileEntry.ENTRY_LENGTH;
        }
        return count;
    }
}
