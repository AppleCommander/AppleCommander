package com.webcodepro.applecommander.storage.os.prodos;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskFactory;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.physical.DosOrder;
import com.webcodepro.applecommander.storage.physical.NibbleOrder;
import com.webcodepro.applecommander.storage.physical.ProdosOrder;
import org.applecommander.hint.Hint;
import org.applecommander.util.DataBuffer;

import java.util.ArrayList;
import java.util.List;

public class ProdosDiskFactory implements DiskFactory {
    @Override
    public void inspect(Context ctx) {
        // A Source should be removing any headers in the file, so we test against actual sizing:
        boolean is140K = ctx.source.getSize() == Disk.APPLE_140KB_DISK;
        boolean isNibble = ctx.source.getSize() == Disk.APPLE_140KB_NIBBLE_DISK;
        boolean is400KPlus = ctx.source.getSize() > 50*16*256;     // Max DOS size
        // It seems easiest to gather all possibilities first...
        List<FormattedDisk> tests = new ArrayList<>();
        if (ctx.source.is(Hint.NIBBLE_SECTOR_ORDER) || isNibble) {
            tests.add(new ProdosFormatDisk(ctx.source.getName(), new NibbleOrder(ctx.source)));
        }
        else if (ctx.source.is(Hint.PRODOS_BLOCK_ORDER) || is400KPlus) {
            tests.add(new ProdosFormatDisk(ctx.source.getName(), new ProdosOrder(ctx.source)));
        }
        else if (ctx.source.is(Hint.DOS_SECTOR_ORDER)) {
            tests.add(new ProdosFormatDisk(ctx.source.getName(), new DosOrder(ctx.source)));
        }
        else if (is140K) {
            // Could be either, so try both PO and DO
            tests.add(new ProdosFormatDisk(ctx.source.getName(), new ProdosOrder(ctx.source)));
            tests.add(new ProdosFormatDisk(ctx.source.getName(), new DosOrder(ctx.source)));
        }
        // ... and then test for ProDOS details:
        for (FormattedDisk fdisk : tests) {
            if (check(fdisk)) {
                ctx.disks.add(fdisk);
            }
        }
    }

    public boolean check(FormattedDisk fdisk) {
        DataBuffer volumeDirectory = DataBuffer.wrap(fdisk.readBlock(2));
        int priorBlock = volumeDirectory.getUnsignedShort(0x00);
        int storageType = volumeDirectory.getUnsignedByte(0x04) >> 4;
        int entryLength = volumeDirectory.getUnsignedByte(0x23);
        int entriesPerBlock = volumeDirectory.getUnsignedByte(0x24);
        // Check primary block for values
        boolean good = priorBlock == 0
                    && storageType == 0xf
                    && entryLength == 0x27
                    && entriesPerBlock == 0x0d;
        // Now follow the directory blocks
        int currentBlock = 2;
        while (good) {
            int nextBlock = volumeDirectory.getUnsignedShort(0x02);
            if (nextBlock == 0) break;
            volumeDirectory = DataBuffer.wrap(fdisk.readBlock(nextBlock));
            // Ensure that the prior link points to the block we just read
            priorBlock = volumeDirectory.getUnsignedShort(0x00);
            good = (priorBlock == currentBlock);
            currentBlock = nextBlock;
        }
        return good;
    }
}
