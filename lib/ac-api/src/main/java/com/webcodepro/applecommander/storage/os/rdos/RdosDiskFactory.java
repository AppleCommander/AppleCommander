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
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import org.applecommander.util.DataBuffer;
import static com.webcodepro.applecommander.storage.os.rdos.RdosFormatDisk.*;

import java.util.Set;

public class RdosDiskFactory implements DiskFactory {
    private final static Set<Integer> validFileTypes = Set.of(0xc1 /*A*/, 0xc2 /*B*/, 0xd4 /*T*/, 0xd3 /*S*/);

    @Override
    public void inspect(Context ctx) {
        ctx.orders.forEach(order -> {
            int sectorsPerTrack = check(order);
            if (sectorsPerTrack > 0) {
                ctx.disks.add(new RdosFormatDisk(ctx.source.getName(), order, sectorsPerTrack));
            }
        });
    }

    /**
     * Check for RDOS catalog. Note that it might be DOS ordered -or- physical sector (13-sector disks).
     * Returns sectors per track (13 or 16).
     */
    public int check(ImageOrder order) {
        boolean good = false;
        if (order.isSizeApprox(DiskConstants.APPLE_140KB_DISK) || order.isSizeApprox(DiskConstants.APPLE_140KB_NIBBLE_DISK)) {
            // 16-sector disks are DOS ordered...
            good = true;
            for (int s=0; s<CATALOG_SECTORS; s++) {
                DataBuffer data = DataBuffer.wrap(order.readSector(1, s));
                good = testCatalogSector(data, 560);
                if (!good) break;
            }
            if (good && testForCatalogCode(order, 0, 1)) {            //testForStartupFile(order, 0x60, 15)) {
                return 16;
            }
            else {
                // 13-sector disks are "physical" ordered...
                for (int s=0; s<CATALOG_SECTORS; s++) {
                    DataBuffer data = DataBuffer.wrap(order.readSector(1,sectorSkew[s]));
                    good = testCatalogSector(data, 455);
                    if (!good) break;
                }
                if (good && testForCatalogCode(order, 1, 9)) {    // testForStartupFile(order, 0xfa, 4, 5)) {
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
    public boolean testForCatalogCode(ImageOrder order, int track, int sector) {
        DataBuffer data = DataBuffer.wrap(order.readSector(track,sector));
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
