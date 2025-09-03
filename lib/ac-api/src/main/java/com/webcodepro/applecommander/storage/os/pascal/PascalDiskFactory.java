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
package com.webcodepro.applecommander.storage.os.pascal;

import com.webcodepro.applecommander.storage.DiskConstants;
import com.webcodepro.applecommander.storage.DiskFactory;
import org.applecommander.device.BlockDevice;
import org.applecommander.device.SkewedTrackSectorDevice;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.device.TrackSectorToBlockAdapter;
import org.applecommander.hint.Hint;
import org.applecommander.util.DataBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Automatic discovery of Pascal volumes.
 */
public class PascalDiskFactory implements DiskFactory {
    @Override
    public void inspect(Context ctx) {
        List<BlockDevice> devices = new ArrayList<>();
        if (ctx.blockDevice != null) {
            devices.add(ctx.blockDevice);
        }
        if (ctx.sectorDevice != null && ctx.sectorDevice.getGeometry().sectorsPerDisk() <= 1600) {
            if (ctx.sectorDevice.is(Hint.NIBBLE_SECTOR_ORDER)) {
                TrackSectorDevice skewed = SkewedTrackSectorDevice.physicalToPascalSkew(ctx.sectorDevice);
                devices.add(new TrackSectorToBlockAdapter(skewed, TrackSectorToBlockAdapter.BlockStyle.PASCAL));
            }
            else if (ctx.sectorDevice.is(Hint.DOS_SECTOR_ORDER)) {
                TrackSectorDevice skewed = SkewedTrackSectorDevice.dosToPascalSkew(ctx.sectorDevice);
                devices.add(new TrackSectorToBlockAdapter(skewed, TrackSectorToBlockAdapter.BlockStyle.PASCAL));
            }
            else {
                // Likely a DSK image, need to pick between DO and PO...
                // Try DO
                TrackSectorDevice device1 = SkewedTrackSectorDevice.dosToPascalSkew(ctx.sectorDevice);
                devices.add(new TrackSectorToBlockAdapter(device1, TrackSectorToBlockAdapter.BlockStyle.PRODOS));
                // Try PO
                TrackSectorDevice device2 = ctx.sectorDevice;
                devices.add(new TrackSectorToBlockAdapter(device2, TrackSectorToBlockAdapter.BlockStyle.PRODOS));
            }
        }

        devices.forEach(device -> {
            if (check(device)) {
                ctx.disks.add(new PascalFormatDisk(ctx.source.getName(), device));
            }
        });
    }

    /** Check for a likely directory structure. Note that we scan all sizes, even though that is overkill. */
    public boolean check(BlockDevice device) {
        boolean good = false;
        if (device.getGeometry().blockSize() >= DiskConstants.PRODOS_BLOCKS_ON_140KB_DISK) {
            // Read entire directory for analysis
            DataBuffer dir = DataBuffer.create(2048);
            for (int block=2; block<6; block++) {
                DataBuffer data = device.readBlock(block);
                dir.put((block-2)*DiskConstants.BLOCK_SIZE, data);
            }
            // Check volume entry
            int dFirstBlock = dir.getUnsignedShort(0);
            int dLastBlock = dir.getUnsignedShort(2);
            int dEntryType = dir.getUnsignedShort(4);
            int dNameLength = dir.getUnsignedByte(6);
            int dBlocksOnDisk = dir.getUnsignedShort(14);
            int dFilesOnDisk = dir.getUnsignedShort(16);
            int dZeroBlock = dir.getUnsignedShort(18);
            if (dBlocksOnDisk == 0) dBlocksOnDisk = 280;    // patch for some Pascal disks found
            good = dFirstBlock == 0 && dLastBlock == 6 && dEntryType == 0
                && dNameLength < 8
                && dFilesOnDisk < 78
                && dBlocksOnDisk >= 280 && dZeroBlock == 0;
            // Check (any) existing file entries
            int offset = 26;
            while (good && dFilesOnDisk > 0 && offset < dir.limit()) {
                int fFirstBlock = dir.getUnsignedShort(offset);
                int fLastBlock = dir.getUnsignedShort(offset+2);
                int fEntryType = dir.getUnsignedShort(offset+4);
                int fNameLength = dir.getUnsignedByte(offset+6);
                int fBytesLastBlock = dir.getUnsignedShort(offset+22);
                if (fNameLength == 0) break;    // last entry?
                good = fFirstBlock < fLastBlock
                    && fLastBlock <= dBlocksOnDisk
                    && (fEntryType & 0x7fff) < 9
                    && fNameLength < 16
                    && fBytesLastBlock <= 512;
                offset += 26;
                dFilesOnDisk--;
            }
        }
        return good;
    }
}
