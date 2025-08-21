package com.webcodepro.applecommander.storage.os.gutenberg;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskFactory;
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import org.applecommander.util.DataBuffer;

import static com.webcodepro.applecommander.storage.os.gutenberg.GutenbergFormatDisk.*;

public class GutenbergDiskFactory implements DiskFactory {
    @Override
    public void inspect(Context ctx) {
        ctx.orders.forEach(order -> {
            if (check(order)) {
                ctx.disks.add(new GutenbergFormatDisk(ctx.source.getName(), order));
            }
        });
    }

    public boolean check(ImageOrder order) {
        boolean good = false;
        if (order.isSizeApprox(Disk.APPLE_140KB_DISK) || order.isSizeApprox(Disk.APPLE_140KB_NIBBLE_DISK)) {
            final int tracksPerDisk = 35;
            final int sectorsPerTrack = 16;
            // Everything starts at T17,S7
            DataBuffer data = DataBuffer.wrap(order.readSector(CATALOG_TRACK, VTOC_SECTOR));
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
