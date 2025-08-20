package com.webcodepro.applecommander.storage.os.cpm;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskFactory;
import org.applecommander.util.DataBuffer;

/**
 * Test this disk for a likely CP/M filesystem.
 * @see <a href="https://www.seasip.info/Cpm/format22.html">CP/M 2.2</a>
 * @see <a href="https://www.seasip.info/Cpm/format31.html">CP/M 3.1</a>
 */
public class CpmDiskFactory implements DiskFactory {
    @Override
    public void inspect(Context ctx) {
        ctx.orders.forEach(order -> {
            if (order.isSizeApprox(Disk.APPLE_140KB_DISK) || order.isSizeApprox(Disk.APPLE_140KB_NIBBLE_DISK)) {
                CpmFormatDisk disk = new CpmFormatDisk(order.getName(), order);
                if (check(disk)) {
                    ctx.disks.add(disk);
                }
            }
        });
    }

    public boolean check(CpmFormatDisk disk) {
        DataBuffer entries = DataBuffer.wrap(disk.readCpmFileEntries());
        int offset = 0;
        while (offset < entries.limit()) {
            // Check if this is an empty directory entry (and ignore it)
            int e5count = 0;
            for (int i=0; i<CpmFileEntry.ENTRY_LENGTH; i++) {
                e5count+= entries.getUnsignedByte(offset+i) == 0xe5 ? 1 : 0;
            }
            if (e5count != CpmFileEntry.ENTRY_LENGTH) {	// Not all bytes were 0xE5
                // Check user number. Should be 0-15 or 0xE5
                int userNumber = entries.getUnsignedByte(offset);
                if (userNumber > 15 && userNumber != 0xe5) return false;
                // Validate filename has highbit off
                for (int i=0; i<8; i++) {
                    if (entries.getUnsignedByte(offset+1+i) > 127) return false;
                }
                // Extent should be 0-31 (low = 0-31 and high = 0)
                int exLow = entries.getUnsignedByte(offset+0xc);
                int exHighS2 = entries.getUnsignedByte(offset+0xe);
                if (exLow > 31 || exHighS2 > 0) return false;
                // Number of used records cannot exceed 0x80
                int numberOfRecords = entries.getUnsignedByte(offset+0xf);
                if (numberOfRecords > 0x80) return false;
            }
            // Next entry
            offset+= CpmFileEntry.ENTRY_LENGTH;
        }
        return true;
    }
}
