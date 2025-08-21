package com.webcodepro.applecommander.storage.os.pascal;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskFactory;
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import org.applecommander.util.DataBuffer;

/**
 * Automatic discovery of Pascal volumes.
 */
public class PascalDiskFactory implements DiskFactory {
    @Override
    public void inspect(Context ctx) {
        ctx.orders.forEach(order -> {
            if (check(order)) {
                ctx.disks.add(new PascalFormatDisk(ctx.source.getName(), order));
            }
        });
    }

    /** Check for a likely directory structure. Note that we scan all sizes, even though that is overkill. */
    public boolean check(ImageOrder order) {
        boolean good = false;
        if (order.getPhysicalSize() >= Disk.APPLE_140KB_DISK) {
            // Read entire directory for analysis
            DataBuffer dir = DataBuffer.create(2048);
            for (int block=2; block<6; block++) {
                byte[] data = order.readBlock(block);
                dir.put((block-2)*Disk.BLOCK_SIZE, DataBuffer.wrap(data));
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
