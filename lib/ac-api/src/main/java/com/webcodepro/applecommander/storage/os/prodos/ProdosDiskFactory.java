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
package com.webcodepro.applecommander.storage.os.prodos;

import com.webcodepro.applecommander.storage.DiskFactory;
import org.applecommander.device.BlockDevice;
import org.applecommander.hint.Hint;
import org.applecommander.util.DataBuffer;
import static com.webcodepro.applecommander.storage.DiskConstants.*;

public class ProdosDiskFactory implements DiskFactory {
    @Override
    public void inspect(Context ctx) {
        ctx.blockDevice()
                .include16Sector(Hint.PRODOS_BLOCK_ORDER)
                .include800K()
                .includeHDV()
                .get()
                .forEach(device -> {
                    if (check(device)) {
                        ctx.disks.add(new ProdosFormatDisk(ctx.source.getName(), device));
                    }
                });
    }

    public boolean check(BlockDevice device) {
        int nextBlock = 2;
        DataBuffer volumeDirectory = device.readBlock(nextBlock);
        int priorBlock = volumeDirectory.getUnsignedShort(0x00);
        int storageType = volumeDirectory.getUnsignedByte(0x04) >> 4;
        int entryLength = volumeDirectory.getUnsignedByte(0x23);
        int bitmapPointer = volumeDirectory.getUnsignedShort(0x27);
        int totalBlocks = volumeDirectory.getUnsignedShort(0x29);
        // Note entriesPerBlock is documented as $D, but other values exist as well ($C, for instance)
        int entriesPerBlock = volumeDirectory.getUnsignedByte(0x24);
        // Check primary block for values
        boolean good = priorBlock == 0
                    && storageType == 0xf
                    && entryLength == 0x27
                    && (entryLength * entriesPerBlock) < BLOCK_SIZE
                    && bitmapPointer < totalBlocks;
        // Now follow the directory blocks -- but only forward; it seems some images have "bad" backward links!
        while (good) {
            // Verify the entries a bit
            for (int i=0x04; i<256; i+=0x27) {
                // skip the volume directory header
                if (nextBlock == 2 && i == 0x04) continue;
                // Skip deleted files
                storageType = volumeDirectory.getUnsignedByte(i) >> 4;
                if (storageType == 0) continue;

                int keyPointer = volumeDirectory.getUnsignedShort(i+0x11);
                int blocksUsed = volumeDirectory.getUnsignedShort(i+0x13);
                int headerPointer = volumeDirectory.getUnsignedShort(i+0x25);
                // Skip empty files (ala Beagle Bros)
                if (keyPointer == 0 && blocksUsed == 0) continue;
                // Test file
                good = keyPointer != 0
                        && keyPointer < totalBlocks
                        && blocksUsed < totalBlocks
                        && headerPointer < totalBlocks;
                if (!good) return false;
            }
            nextBlock = volumeDirectory.getUnsignedShort(0x02);
            if (nextBlock == 0) break;
            if (nextBlock >= totalBlocks) return false;
            volumeDirectory = device.readBlock(nextBlock);
        }
        return good;
    }
}
