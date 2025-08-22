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
package com.webcodepro.applecommander.storage.os.nakedos;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskFactory;
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import org.applecommander.util.DataBuffer;

public class NakedosDiskFactory implements DiskFactory {
    @Override
    public void inspect(Context ctx) {
        ctx.orders.forEach(order -> {
            if (check(order)) {
                ctx.disks.add(new NakedosFormatDisk(ctx.source.getName(), order));
            }
        });
    }

    public boolean check(ImageOrder image) {
        boolean good = false;
        if (image.isSizeApprox(Disk.APPLE_140KB_DISK) || image.isSizeApprox(Disk.APPLE_140KB_NIBBLE_DISK)) {
            final int catalogSize = 560;      // 35 tracks, 16 bytes per track
            // Capture entire catalog
            DataBuffer cat = DataBuffer.create(catalogSize);
            for (int i=0; i<3; i++) {
                // NOTE: Documented as sectors A..C but is really 9..B (https://bitbucket.org/martin.haye/super-mon/src/master/)
                int sector = NakedosFormatDisk.sectorTranslate[9+i];
                DataBuffer data = DataBuffer.wrap(image.readSector(0, sector));
                if (i == 0) {
                    cat.put(0, data.slice(0xd0, 0x30));
                }
                else {
                    cat.put(0x30+(i-1)*256, data);
                }
            }
            // Scan for validity
            for (int i=0; i<catalogSize; i++) {
                int fileId = cat.getUnsignedByte(i);
                boolean validNakedosReserved = i <= 0x0b && fileId == 0xfe;
                boolean othersNotReserved = i > 0x0b && fileId != 0xfe;
                boolean hex00invalid = fileId != 0x00;
                good = (validNakedosReserved || othersNotReserved) && hex00invalid;
                if (!good) break;
            }
        }
        return good;
    }
}
