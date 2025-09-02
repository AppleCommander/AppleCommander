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
package com.webcodepro.applecommander.storage.os.gutenberg;

import com.webcodepro.applecommander.storage.DiskConstants;
import com.webcodepro.applecommander.storage.DiskFactory;
import org.applecommander.device.BlockToTrackSectorAdapter;
import org.applecommander.device.ProdosBlockToTrackSectorAdapterStrategy;
import org.applecommander.device.SkewedTrackSectorDevice;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.hint.Hint;
import org.applecommander.util.DataBuffer;

import java.util.ArrayList;
import java.util.List;

import static com.webcodepro.applecommander.storage.os.gutenberg.GutenbergFormatDisk.*;

public class GutenbergDiskFactory implements DiskFactory {
    @Override
    public void inspect(Context ctx) {
        List<TrackSectorDevice> devices = new ArrayList<>();
        // We need DOS ordered...
        if (ctx.sectorDevice != null) {
            if (ctx.sectorDevice.is(Hint.NIBBLE_SECTOR_ORDER)) {
                devices.add(SkewedTrackSectorDevice.physicalToDosSkew(ctx.sectorDevice));
            }
            else if (ctx.sectorDevice.is(Hint.DOS_SECTOR_ORDER)) {
                devices.add(ctx.sectorDevice);
            }
            else if (ctx.sectorDevice.is(Hint.PRODOS_BLOCK_ORDER)) {
                // Cheating a bit...
                TrackSectorDevice tmp = SkewedTrackSectorDevice.pascalToPhysicalSkew(ctx.sectorDevice);
                devices.add(SkewedTrackSectorDevice.physicalToDosSkew(tmp));
            }
            else {
                // Likely a DSK image
                devices.add(ctx.sectorDevice);
                // Cheating a bit...
                TrackSectorDevice tmp = SkewedTrackSectorDevice.pascalToPhysicalSkew(ctx.sectorDevice);
                devices.add(SkewedTrackSectorDevice.physicalToDosSkew(tmp));
            }
        }
        else if (ctx.blockDevice != null) {
            TrackSectorDevice po = new BlockToTrackSectorAdapter(ctx.blockDevice, new ProdosBlockToTrackSectorAdapterStrategy());
            TrackSectorDevice tmp = SkewedTrackSectorDevice.pascalToPhysicalSkew(po);
            devices.add(SkewedTrackSectorDevice.physicalToDosSkew(tmp));
        }
        devices.forEach(device -> {
            if (check(device)) {
                ctx.disks.add(new GutenbergFormatDisk(ctx.source.getName(), device));
            }
        });
    }

    public boolean check(TrackSectorDevice order) {
        boolean good = false;
        if (order.getGeometry().sectorsPerDisk() == DiskConstants.DOS33_SECTORS_ON_140KB_DISK) {
            final int tracksPerDisk = 35;
            final int sectorsPerTrack = 16;
            // Everything starts at T17,S7
            DataBuffer data = order.readSector(CATALOG_TRACK, VTOC_SECTOR);
            for (int i=0x0f; i<data.limit(); i+= 0x10) {
                // Check for the CR at every 16th byte.
                if (data.getUnsignedByte(i) != 0x8d) return false;
            }
            // Verify T/S links:
            int priorTrack = data.getUnsignedByte(0x00);
            int priorSector = data.getUnsignedByte(0x01);
            int currentTrack = data.getUnsignedByte(0x02);
            int currentSector = data.getUnsignedByte(0x03);     // high bit set if first DIR sector
            int nextTrack = data.getUnsignedByte(0x04);         // high bit set if last DIR sector
            int nextSector = data.getUnsignedByte(0x05);
            good = priorTrack < tracksPerDisk && priorSector < sectorsPerTrack
                && currentTrack < tracksPerDisk && (currentSector & 0x7f) < sectorsPerTrack
                && (nextTrack & 0x7f) < tracksPerDisk && nextSector < sectorsPerTrack
                && isHighAscii(data, 6, 9);
            if (!good) return false;
            // Check that the file entries are as expected
            for (int i=0x10; i<data.limit(); i+= 0x10) {
                int firstTrack = data.getUnsignedByte(i+0xc);
                int firstSector = data.getUnsignedByte(i+0xd);  // if == 0x40, file is deleted
                good = isHighAscii(data, i, 12)
                    && firstTrack < tracksPerDisk
                    && (firstSector < sectorsPerTrack || firstSector == 0x40)
                    && isHighAscii(data, i+0xe, 1);
                if (!good) return false;
            }
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
