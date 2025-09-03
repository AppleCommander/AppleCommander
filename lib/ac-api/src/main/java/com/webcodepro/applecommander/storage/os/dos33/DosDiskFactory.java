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
package com.webcodepro.applecommander.storage.os.dos33;

import com.webcodepro.applecommander.storage.DiskFactory;
import org.applecommander.device.BlockToTrackSectorAdapter;
import org.applecommander.device.ProdosBlockToTrackSectorAdapterStrategy;
import org.applecommander.device.SkewedTrackSectorDevice;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.hint.Hint;
import org.applecommander.os.dos.OzdosAdapterStrategy;
import org.applecommander.os.dos.UnidosAdapterStrategy;
import org.applecommander.util.DataBuffer;
import static com.webcodepro.applecommander.storage.DiskConstants.*;
import static com.webcodepro.applecommander.storage.os.dos33.DosFormatDisk.*;

import java.util.*;

public class DosDiskFactory implements DiskFactory {
    @Override
    public void inspect(Context ctx) {
        // It seems easiest to gather all possibilities first...
        List<TrackSectorDevice> devices = new ArrayList<>();
        // We need DOS ordered...
        if (ctx.sectorDevice != null) {
            if (ctx.sectorDevice.is(Hint.NIBBLE_SECTOR_ORDER)) {
                if (ctx.sectorDevice.getGeometry().sectorsPerTrack() == 13) {
                    // 13-sector = DOS 3.2 = physical sector order
                    devices.add(ctx.sectorDevice);
                }
                else {
                    // 16-sector = DOS 3.3 = DOS sector order
                    devices.add(SkewedTrackSectorDevice.physicalToDosSkew(ctx.sectorDevice));
                }
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
                // Likely a DSK image, need to pick between DO and PO...
                TrackSectorDevice device1 = ctx.sectorDevice;
                // Cheating a bit...
                TrackSectorDevice tmp = SkewedTrackSectorDevice.pascalToPhysicalSkew(ctx.sectorDevice);
                TrackSectorDevice device2 = SkewedTrackSectorDevice.physicalToDosSkew(tmp);

                int count1 = count(device1);
                int count2 = count(device2);
                // Note this assumes DO was the first device in the list to give it an edge
                if (count1 >= count2) devices.add(device1);
                else devices.add(device2);
            }
        }
        else if (ctx.blockDevice != null) {
            if (ctx.blockDevice.getGeometry().blocksOnDevice() == PRODOS_BLOCKS_ON_140KB_DISK) {
                devices.add(new BlockToTrackSectorAdapter(ctx.blockDevice, new ProdosBlockToTrackSectorAdapterStrategy()));
            }
            else if (ctx.blockDevice.getGeometry().blocksOnDevice() == PRODOS_BLOCKS_ON_800KB_DISK) {
                devices.add(new BlockToTrackSectorAdapter(ctx.blockDevice, UnidosAdapterStrategy.UNIDOS_DISK_1));
                devices.add(new BlockToTrackSectorAdapter(ctx.blockDevice, UnidosAdapterStrategy.UNIDOS_DISK_2));
                devices.add(new BlockToTrackSectorAdapter(ctx.blockDevice, OzdosAdapterStrategy.OZDOS_DISK_1));
                devices.add(new BlockToTrackSectorAdapter(ctx.blockDevice, OzdosAdapterStrategy.OZDOS_DISK_2));
            }
        }

        // ... and then test for DOS VTOC etc. Passing track number along to hopefully handle it later!
        for (TrackSectorDevice device : devices) {
            if (check(device)) {
                ctx.disks.add(new DosFormatDisk(ctx.source.getName(), device));
            }
        }
    }

    /**
     * Test this image order by looking for a likely DOS VTOC and set of catalog sectors.
     */
    public boolean check(TrackSectorDevice device) {
        DataBuffer vtoc = device.readSector(CATALOG_TRACK, VTOC_SECTOR);
        int nextTrack = vtoc.getUnsignedByte(0x01);
        int nextSector = vtoc.getUnsignedByte(0x02);
        int tracksPerDisk = vtoc.getUnsignedByte(0x34);
        int sectorsPerTrack = vtoc.getUnsignedByte(0x35);
        if (nextSector == 0 && nextTrack != 0) {
            // Some folks "hid" the catalog by setting the pointer to T17,S0 - try and adjust
            nextSector = sectorsPerTrack-1;
        }
        if (nextSector != 0 && nextTrack == 0) {
            // Some folks zeroed out the next track field, so try the same as VTOC (T17)
            nextTrack = DosFormatDisk.CATALOG_TRACK;
        }
        // Start with VTOC test
        boolean good = nextTrack <= tracksPerDisk       // expect catalog to be sensible
                    && nextSector > 0                   // expect catalog to be...
                    && nextSector < sectorsPerTrack     // ... a legitimate sector
                    && tracksPerDisk >= CATALOG_TRACK   // expect sensible...
                    && tracksPerDisk <= 50              // ... tracks per disk
                    && sectorsPerTrack > 10             // expect sensible...
                    && sectorsPerTrack <= 32;           // ... sectors per disk
        // Check that the free sectors are sensible (really only valid for 13 or 16 sector disks)
        if (sectorsPerTrack == 13 || sectorsPerTrack == 16) {
            // We only check that which is in common (bytes 3+4 of 1-4).
            // Some DOS 3.2 cracks are on DOS 3.3 disks but report as 13 sector.
            int unexpectedValue = 0;
            for (int i=0; i<tracksPerDisk; i++) {
                int offset = 0x38 + (i * 4);
                int t3 = vtoc.getUnsignedByte(offset+2);
                int t4 = vtoc.getUnsignedByte(offset+3);
                // Found a free sector that should not exist
                if (t3 != 0 || t4 != 0) unexpectedValue++;
            }
            // Totally arbitrary. Allow some errors but not a lot.
            if (unexpectedValue > 3) {
                return false;
            }
        }
        // Now chase the directory links (note we assume catalog is all on same track).
        Set<Integer> visited = new HashSet<>();
        while (good) {
            int mark = nextTrack * 100 + nextSector;
            if (visited.contains(mark)) break;
            visited.add(mark);
            DataBuffer cat = device.readSector(nextTrack,nextSector);
            nextTrack = cat.getUnsignedByte(0x01);
            nextSector = cat.getUnsignedByte(0x02);
            if (nextTrack == 0) break;  // at end
            good = checkCatalogValidity(cat, tracksPerDisk, sectorsPerTrack);
        }
        return good;
    }

    public int count(TrackSectorDevice disk) {
        DataBuffer vtoc = disk.readSector(CATALOG_TRACK, VTOC_SECTOR);
        int nextTrack = vtoc.getUnsignedByte(0x01);
        int nextSector = vtoc.getUnsignedByte(0x02);
        int tracksPerDisk = vtoc.getUnsignedByte(0x34);
        int sectorsPerTrack = vtoc.getUnsignedByte(0x35);
        if (tracksPerDisk > 50 || sectorsPerTrack > 32) {
            return 0;
        }
        if (nextSector == 0 && nextTrack != 0) {
            // Some folks "hid" the catalog by setting the pointer to T17,S0 - try and adjust
            nextSector = sectorsPerTrack-1;
        }
        int count = 0;
        Set<Integer> visited = new HashSet<>();
        while (nextTrack > 0 && nextTrack <= tracksPerDisk && nextSector > 0 && nextSector < sectorsPerTrack) {
            int mark = nextTrack * 100 + nextSector;
            if (visited.contains(mark)) break;
            visited.add(mark);
            count++;
            DataBuffer data = disk.readSector(nextTrack, nextSector);
            if (!checkCatalogValidity(data, tracksPerDisk, sectorsPerTrack)) break;
            nextTrack = data.getUnsignedByte(0x01);
            nextSector = data.getUnsignedByte(0x02);
        }
        return count;
    }

    // Notes (all of this makes it more difficult to test!):
    // 1. File type isn't always as designated by Apple DOS.
    // 2. Sector size in the file entry is frequently bunk (as in > 560).
    // 3. T/S pair can be bunk - trying to do the test and yet exclude "bad" components
    public boolean checkCatalogValidity(DataBuffer data, int tracksPerDisk, int sectorsPerTrack) {
        int nextTrack = data.getUnsignedByte(0x01);
        int nextSector = data.getUnsignedByte(0x02);
        if (nextTrack > tracksPerDisk || nextSector > sectorsPerTrack) return false;
        for (int offset=0x0b; offset<0xff; offset+=0x23) {
            int track = data.getUnsignedByte(offset);
            if (track == 0) break;
            if (track == 0xff) continue;    // just skip deleted files
            int sector = data.getUnsignedByte(offset+0x01);
            int sectorSize = data.getUnsignedShort(offset+0x21);
            // Allow potentially bad T/S if the file size is 0.
            if (sectorSize == 0) continue;
            // Otherwise expect things to be legit.
            if (track > tracksPerDisk || sector > sectorsPerTrack) {
                return false;
            }
        }
        return true;
    }
}
