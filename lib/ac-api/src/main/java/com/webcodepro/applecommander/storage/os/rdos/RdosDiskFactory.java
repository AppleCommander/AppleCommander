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
package com.webcodepro.applecommander.storage.os.rdos;

import com.webcodepro.applecommander.storage.DiskConstants;
import com.webcodepro.applecommander.storage.DiskFactory;
import org.applecommander.device.*;
import org.applecommander.hint.Hint;
import org.applecommander.source.Source;
import org.applecommander.util.DataBuffer;

import static com.webcodepro.applecommander.storage.DiskConstants.DOS32_SECTORS_ON_115KB_DISK;
import static com.webcodepro.applecommander.storage.os.rdos.RdosFormatDisk.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RdosDiskFactory implements DiskFactory {
    private final static Set<Integer> validFileTypes = Set.of(0xc1 /*A*/, 0xc2 /*B*/, 0xd4 /*T*/, 0xd3 /*S*/);

    @Override
    public void inspect(Context ctx) {
        List<TrackSectorDevice> devices = new ArrayList<>();
        if (ctx.sectorDevice != null) {
            if (ctx.sectorDevice.is(Hint.NIBBLE_SECTOR_ORDER)) {
                if (ctx.sectorDevice.getGeometry().sectorsPerTrack() == 13) {
                    devices.add(ctx.sectorDevice);
                }
                else {
                    devices.add(SkewedTrackSectorDevice.physicalToDosSkew(ctx.sectorDevice));
                }
            }
            else if (ctx.sectorDevice.is(Hint.DOS_SECTOR_ORDER)) {
                devices.add(ctx.sectorDevice);
            }
            else if (ctx.sectorDevice.is(Hint.PRODOS_BLOCK_ORDER)) {
                // cheating a bit here
                TrackSectorDevice physicalSkew = SkewedTrackSectorDevice.pascalToPhysicalSkew(ctx.sectorDevice);
                devices.add(SkewedTrackSectorDevice.physicalToDosSkew(physicalSkew));
            }
            else {
                // DSK image. Could be DO or PO.
                devices.add(ctx.sectorDevice);
                // cheating a bit here for PO
                TrackSectorDevice physicalSkew = SkewedTrackSectorDevice.pascalToPhysicalSkew(ctx.sectorDevice);
                devices.add(SkewedTrackSectorDevice.physicalToDosSkew(physicalSkew));
            }
        }
        if (ctx.blockDevice != null) {
            devices.add(new BlockToTrackSectorAdapter(ctx.blockDevice, new ProdosBlockToTrackSectorAdapterStrategy()));
        }

        devices.forEach(device -> {
            int sectorsPerTrack = check(device);
            if (sectorsPerTrack > 0) {
                // Detect if we're a 16 sector disk but RDOS expects only 13 sectors:
                if (sectorsPerTrack != device.getGeometry().sectorsPerTrack()) {
                    // 13-sector disks are in physical order, so fix it:
                    device = SkewedTrackSectorDevice.dosToPhysicalSkew(device);
                    // And make the 16-sector disk a fake 13-sector disk:
                    device = SkewedTrackSectorDevice.truncate16sectorTo13(device);
                }
                BlockDevice blockDevice = new TrackSectorToBlockAdapter(device, TrackSectorToBlockAdapter.BlockStyle.RDOS);
                ctx.disks.add(new RdosFormatDisk(ctx.source.getName(), blockDevice));
            }
        });
    }

    /**
     * Check for RDOS catalog. Note that it might be DOS ordered -or- physical sector (13-sector disks).
     * The {@code testForCatalogCode()} method essentially validates sector ordering is correct.
     * Returns sectors per track (13 or 16).
     */
    public int check(TrackSectorDevice device) {
        boolean good = false;
        Source source = device.get(Source.class).orElseThrow();
        if (source.isApproxEQ(DiskConstants.APPLE_140KB_DISK) || source.isApproxEQ(DiskConstants.APPLE_140KB_NIBBLE_DISK)) {
            // 16-sector disks are DOS ordered...
            good = true;
            for (int s=0; s<CATALOG_SECTORS; s++) {
                DataBuffer data = device.readSector(1, s);
                good = testCatalogSector(data, 560);
                if (!good) break;
            }
            if (good && testForCatalogCode(device, 0, 1)) {
                return 16;
            }
            else {
                // 13-sector disks are "physical" ordered...
                device = SkewedTrackSectorDevice.dosToPhysicalSkew(device);
                for (int s=0; s<CATALOG_SECTORS; s++) {
                    DataBuffer data = device.readSector(1,s);
                    good = testCatalogSector(data, DOS32_SECTORS_ON_115KB_DISK);
                    if (!good) break;
                }
                if (good && testForCatalogCode(device, 1, 12)) {
                    return 13;
                }
            }
        }
        return -1;
    }

    /*
     * Test a single catalog sector.
     * <br/>
     * Notes from RdosFileEntry...
     * $00-$17  File name; space-filled.  If the first byte is $00, that is the end of the<br>
     *          directory.  If the first byte is $80, the file is deleted.<br>
     * $18      File type. Appears to be actual letter ('A'=Applesoft, etc)<br>
     * $19      File length in blocks (block = sector = 256 bytes)<br>
     * $1A-$1B  Address of application.  For Applesoft and binary; others may vary.<br>
     * $1C-$1D  Length in bytes of file.<br>
     * $1E-$1F  Starting block of application.<br>
     */
    public boolean testCatalogSector(DataBuffer data, final int maxBlocks) {
        boolean good = true;
        boolean atEnd = false;
        for (int i = 0; i< DiskConstants.SECTOR_SIZE; i+= 0x20) {
            int check = data.getUnsignedByte(i);
            // Once we reach the last file, ensure the rest of the directory is all 0's
            if (atEnd && check != 0) return false;
            atEnd = check == 0;             // at end of directory
            if (atEnd) continue;
            if (check == 0x80) continue;    // deleted
            // Check this is all valid high-bit ASCII
            for (int j=0; j<0x18; j++) {
                good = data.getUnsignedByte(i) >= 0xA0;
                if (!good) return good;
            }
            int fileType = data.getUnsignedByte(i+0x18);
            int lengthInBlocks = data.getUnsignedByte(i+0x19);
            int lengthInBytes = data.getUnsignedShort(i+0x1c);
            int firstBlock = data.getUnsignedShort(i+0x1e);
            good = validFileTypes.contains(fileType)
                    && lengthInBytes <= lengthInBlocks* DiskConstants.SECTOR_SIZE
                    && firstBlock < maxBlocks;
            if (!good) return good;
        }
        return good;
    }

    /**
     * This is (hopefully) the determinant of correct sector ordering. Sector 1 (16-sector image) and
     * 9 (13-sector image) get mapped differently between DO and PO disks.
     */
    public boolean testForCatalogCode(TrackSectorDevice device, int track, int sector) {
        DataBuffer data = device.readSector(track,sector);
        final String header = "  LEN         -<NAME>-       LENGTH BLK";
        final String notInUse = "<NOT IN USE>";
        return locate(data,header) && locate(data,notInUse);
    }
    public boolean locate(DataBuffer buffer, String search) {
        // Convert to a string type thing
        StringBuilder sb = new StringBuilder();
        buffer.position(0);
        while (buffer.hasRemaining()) {
            int ch = buffer.readUnsignedByte() & 0x7f;
            if (ch < 0x20) ch = 0x2e;   // "."
            sb.appendCodePoint(ch);
        }
        // And just use a String function
        return sb.toString().contains(search);
    }
}
