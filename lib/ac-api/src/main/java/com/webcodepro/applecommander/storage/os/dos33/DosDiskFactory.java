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

import java.util.*;

public class DosDiskFactory implements DiskFactory {
    @Override
    public void inspect(Context ctx) {
        // A Source should be removing any headers in the file, so we test against actual sizing:
        boolean is140K = ctx.source.isApproxEQ(Disk.APPLE_140KB_DISK);
        boolean isNibble = ctx.source.isApproxEQ(Disk.APPLE_140KB_NIBBLE_DISK);
        boolean is400KOrLess = ctx.source.isApproxLE(50*16*256);     // Max DOS size
        boolean is800K = ctx.source.isApproxEQ(Disk.APPLE_800KB_DISK);
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
            // Could be either, so count both PO and DO and choose the longest catalog
            FormattedDisk poDisk = new DosFormatDisk(ctx.source.getName(), new ProdosOrder(ctx.source));
            FormattedDisk doDisk = new DosFormatDisk(ctx.source.getName(), new DosOrder(ctx.source));
            int poCount = count(poDisk, 17);
            int doCount = count(doDisk, 17);
            if (poCount > doCount) tests.add(poDisk);
            else tests.add(doDisk); // note the slight edge for DO disks
        }
        // ... and then test for DOS VTOC etc. Passing track number along to hopefully handle it later!
        for (FormattedDisk fdisk : tests) {
            try {
                if (check(fdisk, 17)) {
                    ctx.disks.add(fdisk);
                }
            } catch (Throwable t) {
                // obviously wrong configuration
            }
        }
    }

    /**
     * Test this image order by looking for a likely DOS VTOC and set of catalog sectors.
     */
    public boolean check(FormattedDisk disk, final int vtocTrack) {
        DataBuffer vtoc = DataBuffer.wrap(disk.readSector(vtocTrack, 0));
        int nextTrack = vtoc.getUnsignedByte(0x01);
        int nextSector = vtoc.getUnsignedByte(0x02);
        int tsPairs = vtoc.getUnsignedByte(0x27);
        int tracksPerDisk = vtoc.getUnsignedByte(0x34);
        int sectorsPerTrack = vtoc.getUnsignedByte(0x35);
        if (nextSector == 0 && nextTrack != 0) {
            // Some folks "hid" the catalog by setting the pointer to T17,S0 - try and adjust
            nextSector = sectorsPerTrack-1;
        }
        // Start with VTOC test
        boolean good = nextTrack == vtocTrack           // expect catalog to match our track
                    && nextSector > 0                   // expect catalog to be...
                    && nextSector < sectorsPerTrack     // ... a legitimate sector
                    && tsPairs == 122                   // expect 122 track/sectors pairs per sector
                    && tracksPerDisk >= vtocTrack       // expect sensible...
                    && tracksPerDisk <= 50              // ... tracks per disk
                    && sectorsPerTrack > 10             // expect sensible...
                    && sectorsPerTrack <= 32;           // ... sectors per disk
        // Now chase the directory links (note we assume catalog is all on same track).
        Set<Integer> visited = new HashSet<>();
        while (good) {
            int mark = nextTrack * 100 + nextSector;
            if (visited.contains(mark)) break;
            visited.add(mark);
            DataBuffer cat = DataBuffer.wrap(disk.readSector(nextTrack,nextSector));
            nextTrack = cat.getUnsignedByte(0x01);
            nextSector = cat.getUnsignedByte(0x02);
            if (nextTrack == 0) break;  // at end
            good = checkCatalogValidity(cat, tracksPerDisk, sectorsPerTrack);
        }
        return good;
    }

    public int count(FormattedDisk disk, final int vtocTrack) {
        DataBuffer vtoc = DataBuffer.wrap(disk.readSector(vtocTrack, 0));
        int nextTrack = vtoc.getUnsignedByte(0x01);
        int nextSector = vtoc.getUnsignedByte(0x02);
        int tracksPerDisk = vtoc.getUnsignedByte(0x34);
        int sectorsPerTrack = vtoc.getUnsignedByte(0x35);
        if (tracksPerDisk > 50 || sectorsPerTrack > 32) {
            return 0;
        }
        if (nextSector == 0 && nextTrack != 0) {
            // Some folks "hid" the catalog by setting the pointer to T17,S0 - try and adjust
            nextSector = sectorsPerTrack-1;
        }
        int count = 0;
        Set<Integer> visited = new HashSet<>();
        while (nextTrack > 0 && nextTrack <= tracksPerDisk && nextSector > 0 && nextSector < sectorsPerTrack) {
            int mark = nextTrack * 100 + nextSector;
            if (visited.contains(mark)) break;
            visited.add(mark);
            count++;
            DataBuffer data = DataBuffer.wrap(disk.readSector(nextTrack, nextSector));
            if (!checkCatalogValidity(data, tracksPerDisk, sectorsPerTrack)) break;
            nextTrack = data.getUnsignedByte(0x01);
            nextSector = data.getUnsignedByte(0x02);
        }
        return count;
    }

    // Notes (all of this makes it more difficult to test!):
    // 1. File type isn't always as designated by Apple DOS.
    // 2. Sector size is frequently bunk (as in > 560).
    // 3. T/S pair can be bunk - trying to do the test but exclude "bad" components
    public boolean checkCatalogValidity(DataBuffer data, int tracksPerDisk, int sectorsPerTrack) {
        int nextTrack = data.getUnsignedByte(0x01);
        int nextSector = data.getUnsignedByte(0x02);
        if (nextTrack > tracksPerDisk || nextSector > sectorsPerTrack) return false;
        for (int offset=0x0b; offset<0xff; offset+=0x23) {
            int track = data.getUnsignedByte(offset);
            if (track == 0) break;
            if (track == 0xff) continue;    // just skip deleted files
            int sector = data.getUnsignedByte(offset+0x01);
            int sectorSize = data.getUnsignedShort(offset+0x21);
            // Allow potentially bad T/S if the file size is 0.
            if (sectorSize == 0) continue;
            // Otherwise expect things to be legit.
            if (track > tracksPerDisk || sector > sectorsPerTrack) {
                return false;
            }
        }
        return true;
    }
}
