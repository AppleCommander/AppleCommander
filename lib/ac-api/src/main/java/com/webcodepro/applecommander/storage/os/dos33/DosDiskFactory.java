package com.webcodepro.applecommander.storage.os.dos33;

import com.webcodepro.applecommander.storage.DiskFactory;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import org.applecommander.util.DataBuffer;

import java.util.*;

public class DosDiskFactory implements DiskFactory {
    @Override
    public void inspect(Context ctx) {
        // It seems easiest to gather all possibilities first...
        List<FormattedDisk> tests = new ArrayList<>();
        if (ctx.orders.size() == 1) {
            ImageOrder order = ctx.orders.getFirst();
            if (order.isSizeApprox(FormattedDisk.APPLE_800KB_DISK)) {
                tests.add(new UniDosFormatDisk(ctx.source.getName(), order, UniDosFormatDisk.UNIDOS_DISK_1));
                tests.add(new UniDosFormatDisk(ctx.source.getName(), order, UniDosFormatDisk.UNIDOS_DISK_2));
                tests.add(new OzDosFormatDisk(ctx.source.getName(), order, OzDosFormatDisk.OZDOS_DISK_1));
                tests.add(new OzDosFormatDisk(ctx.source.getName(), order, OzDosFormatDisk.OZDOS_DISK_2));
            }
            else {
                tests.add(new DosFormatDisk(ctx.source.getName(), ctx.orders.getFirst()));
            }
        }
        else if (ctx.orders.size() == 2) {
            // Could be either, so count both (should be PO vs DO) and choose the longest catalog
            FormattedDisk fdisk1 = new DosFormatDisk(ctx.source.getName(), ctx.orders.get(0));
            FormattedDisk fdisk2 = new DosFormatDisk(ctx.source.getName(), ctx.orders.get(1));
            int count1 = count(fdisk1, 17);
            int count2 = count(fdisk2, 17);
            // Note this assumes DO was the first ImageOrder in the list to give it an edge
            if (count1 >= count2) tests.add(fdisk1);
            else tests.add(fdisk2);
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
