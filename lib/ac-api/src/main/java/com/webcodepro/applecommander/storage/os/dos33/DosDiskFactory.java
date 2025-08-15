package com.webcodepro.applecommander.storage.os.dos33;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskFactory;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.physical.DosOrder;
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import com.webcodepro.applecommander.storage.physical.NibbleOrder;
import com.webcodepro.applecommander.storage.physical.ProdosOrder;
import org.applecommander.hint.Hint;
import org.applecommander.util.DataBuffer;

import java.util.ArrayList;
import java.util.List;

public class DosDiskFactory implements DiskFactory {
    @Override
    public void inspect(Context ctx) {
        // A Source should be removing any headers in the file, so we test against actual sizing:
        boolean is140K = ctx.source.getSize() == Disk.APPLE_140KB_DISK;
        boolean isNibble = ctx.source.getSize() == Disk.APPLE_140KB_NIBBLE_DISK;
        boolean is400KOrLess = ctx.source.getSize() <= 50*16*256;     // Max DOS size
        boolean is800K = ctx.source.getSize() == Disk.APPLE_800KB_DISK;
        // It seems easiest to gather all possibilities first...
        List<FormattedDisk> tests = new ArrayList<>();
        if (ctx.source.is(Hint.NIBBLE_SECTOR_ORDER) || isNibble) {
            tests.add(new DosFormatDisk(ctx.source.getName(), new NibbleOrder(ctx.source)));
        }
        else if (is800K) {
            ImageOrder order = new ProdosOrder(ctx.source);
            tests.add(new UniDosFormatDisk(ctx.source.getName(), order, UniDosFormatDisk.UNIDOS_DISK_1));
            tests.add(new UniDosFormatDisk(ctx.source.getName(), order, UniDosFormatDisk.UNIDOS_DISK_2));
            tests.add(new OzDosFormatDisk(ctx.source.getName(), order, OzDosFormatDisk.OZDOS_DISK_1));
            tests.add(new OzDosFormatDisk(ctx.source.getName(), order, OzDosFormatDisk.OZDOS_DISK_2));
        }
        else if (ctx.source.is(Hint.PRODOS_BLOCK_ORDER) && is400KOrLess) {
            tests.add(new DosFormatDisk(ctx.source.getName(), new ProdosOrder(ctx.source)));
        }
        else if (ctx.source.is(Hint.DOS_SECTOR_ORDER)) {
            tests.add(new DosFormatDisk(ctx.source.getName(), new DosOrder(ctx.source)));
        }
        else if (is140K) {
            // Could be either, so try both PO and DO
            tests.add(new DosFormatDisk(ctx.source.getName(), new ProdosOrder(ctx.source)));
            tests.add(new DosFormatDisk(ctx.source.getName(), new DosOrder(ctx.source)));
        }
        // ... and then test for DOS VTOC etc. Passing track number along to hopefully handle it later!
        for (FormattedDisk fdisk : tests) {
            if (check(fdisk, 17)) {
                ctx.disks.add(fdisk);
            }
        }
    }

    /**
     * Test this image order by looking for a likely DOS VTOC and set of catalog sectors.
     */
    public boolean check(FormattedDisk disk, final int track) {
        DataBuffer vtoc = DataBuffer.wrap(disk.readSector(track, 0));
        int nextTrack = vtoc.getUnsignedByte(0x01);
        int nextSector = vtoc.getUnsignedByte(0x02);
        int tsPairs = vtoc.getUnsignedByte(0x27);
        int tracksPerDisk = vtoc.getUnsignedByte(0x34);
        int sectorsPerTrack = vtoc.getUnsignedByte(0x35);
        int bytesPerSector = vtoc.getUnsignedShort(0x36);
        // Start with VTOC test
        boolean good = nextTrack == track       // expect catalog to match our track
                    && nextSector > 0           // expect catalog to be...
                    && nextSector < 32          // ... a legitimate sector
                    && tsPairs == 122           // expect 122 track/sectors pairs per sector
                    && tracksPerDisk >= track   // expect sensible...
                    && tracksPerDisk <= 50      // ... tracks per disk
                    && sectorsPerTrack > 10     // expect sensible...
                    && sectorsPerTrack <= 32    // ... sectors per disk
                    && bytesPerSector == 0x100; // expect 256 bytes per sector
        // Now chase the directory links (note we assume catalog is all on same track).
        int sector = nextSector;
        int nextSectorPointers = 0;
        while (good) {
            nextSectorPointers++;
            DataBuffer cat = DataBuffer.wrap(disk.readSector(track,sector));
            nextTrack = cat.getUnsignedByte(0x01);
            nextSector = cat.getUnsignedByte(0x02);
            if (nextTrack == 0) break;  // at end
            good = false;
            if (track == nextTrack && sector == nextSector+1) {
                sector = nextSector;
                good = true;
            }
        }
        // Finally, due to PO and DO mapping, make certain we traversed as many sectors as expected
        // Note that we are NOT following the linkages, just reading all sectors.
        if (good) {
            int actualSectorPointers = 0;
            for (sector=0; sector<sectorsPerTrack; sector++) {
                DataBuffer cat = DataBuffer.wrap(disk.readSector(track,sector));
                nextTrack = cat.getUnsignedByte(0x01);
                nextSector = cat.getUnsignedByte(0x02);
                if (track == nextTrack && nextSector != 0) {
                    actualSectorPointers++;
                }
            }
            good = (nextSectorPointers == actualSectorPointers);
        }
        return good;
    }
}
