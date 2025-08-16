package com.webcodepro.applecommander.storage.os.prodos;

import com.webcodepro.applecommander.storage.DiskFactory;
import com.webcodepro.applecommander.storage.FormattedDisk;
import org.applecommander.util.DataBuffer;

import java.util.ArrayList;
import java.util.List;

public class ProdosDiskFactory implements DiskFactory {
    @Override
    public void inspect(Context ctx) {
        // It seems easiest to gather all possibilities first...
        List<FormattedDisk> tests = new ArrayList<>();
        ctx.orders.forEach(order -> tests.add(new ProdosFormatDisk(ctx.source.getName(), order)));
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
        // Now follow the directory blocks -- but only forward; it seems some images have "bad" backward links!
        while (good) {
            int nextBlock = volumeDirectory.getUnsignedShort(0x02);
            if (nextBlock == 0) break;
            volumeDirectory = DataBuffer.wrap(fdisk.readBlock(nextBlock));
            // Ensure that the prior link points to the block we just read
            priorBlock = volumeDirectory.getUnsignedShort(0x00);
            good = (priorBlock != 0);
        }
        return good;
    }
}
