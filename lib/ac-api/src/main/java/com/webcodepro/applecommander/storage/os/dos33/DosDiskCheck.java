package com.webcodepro.applecommander.storage.os.dos33;

import com.webcodepro.applecommander.storage.DiskConstants;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.util.AppleUtil;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.os.DiskCheck;
import org.applecommander.util.DataBuffer;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;

public class DosDiskCheck implements DiskCheck {
    private final DosFormatDisk disk;
    private final TrackSectorDevice device;

    private final List<Finding> findings = new ArrayList<>();
    private final UsedSectors usedSectors = new UsedSectors();

    public DosDiskCheck(DosFormatDisk disk, TrackSectorDevice device) {
        this.disk = disk;
        this.device = device;
    }

    @Override
    public List<Finding> scan() throws Exception {
        initUsedSectors();
        checkVtoc();
        for (FileEntry entry : disk.getFiles()) {
            if (entry instanceof DosFileEntry file) {
                switch (file.getTrack()) {
                    case 0x00 -> { /* Unused. */ }
                    case 0xff -> { /* Deleted. */ }
                    default -> checkFile(file);
                }
                byte filetype = file.readFileEntry()[0x02];
                filetype &= 0x7f;
                if (AppleUtil.getBitCount(filetype) > 1) {
                    // Noting odd file types. Really can't fix it.
                    DosSectorAddress ts = file.getFileFileAddress();
                    addFinding("file", ts.track, ts.sector,Optional.empty(),
                            "Unexpected DOS file type of %02X.", filetype);
                }
            }
            else {
                throw new RuntimeException("Unexpected DOS file type: " + entry.getClass().getName());
            }
        }
        checkVtocBitMap();
        return findings;
    }

    private void initUsedSectors() {
        int dosTracks = 3;
        // Looks like UniDOS/OzDOS just use track 0 for DOS. That's 64 sectors, so larger than a usual 140K disk!
        int deviceSize = device.getGeometry().deviceSize();
        if (deviceSize >= DiskConstants.APPLE_800KB_DISK && deviceSize <= DiskConstants.APPLE_800KB_2IMG_DISK) {
            dosTracks = 1;
        }
        // Mark DOS sectors as used.
        for (int track = 0; track < dosTracks; track++) {
            for (int sector = 0; sector < device.getGeometry().sectorsPerTrack(); sector++) {
                usedSectors.set(track, sector);
            }
        }
        // Work through VTOC and catalog sectors.
        int track = 17;
        int sector = 0;
        while (track > 0) {
            usedSectors.set(track, sector);
            DataBuffer data = device.readSector(track, sector);
            track = data.getUnsignedByte(0x01);
            sector = data.getUnsignedByte(0x02);
        }
    }

    private void checkFile(DosFileEntry file) {
        int sectorsRemaining = file.getSectorsUsed();
        int track = file.getTrack();
        int sector = file.getSector();
        while (track > 0) {
            usedSectors.set(track,sector);
            DataBuffer data = device.readSector(track, sector);
            track = data.getUnsignedByte(0x01);
            sector = data.getUnsignedByte(0x02);
            int offset = 0x0c;
            while (sectorsRemaining > 0 && offset < 0x100) {
                int t = data.getUnsignedByte(offset++);
                int s = data.getUnsignedByte(offset++);
                usedSectors.set(t,s);
                sectorsRemaining--;
            }
        }
        if (sectorsRemaining > 0) {
            // This seems fishy. More of a warning, so fix to apply.
            addFinding("file", file.getTrack(), file.getSector(), Optional.empty(),
                    "Sectors used appears to be too high (file entry says %d but we have %d left over)",
                    file.getSectorsUsed(), sectorsRemaining);
        }
    }

    private void checkVtoc() {
        DataBuffer data = DataBuffer.wrap(disk.readVtoc());
        if (data.getUnsignedByte(0x27) != 122) {
            addFinding("vtoc", 17, 0, Optional.of(() -> {
                data.putByte(0x27, 122);
                disk.writeVtoc(data.asBytes());
            }), "VTOC T/S pairs is expected to be 122 but is %d.", data.getUnsignedByte(0x27));
        }
        // Note: These next two just reuse the VTOC bytes instead of the #getTracks or #getSectors on DosFormatDisk.
        if (data.getUnsignedByte(0x34) != device.getGeometry().tracksOnDisk()) {
            addFinding("vtoc", 17, 0, Optional.of(() -> {
                data.putByte(0x34, device.getGeometry().tracksOnDisk());
                disk.writeVtoc(data.asBytes());
            }), "VTOC tracks on disk (%d) do not match device tracks on disk (%d).",
                    data.getUnsignedByte(0x34), device.getGeometry().tracksOnDisk());
        }
        if (data.getUnsignedByte(0x35) != device.getGeometry().sectorsPerTrack()) {
            addFinding("vtoc", 17, 0, Optional.of(() -> {
                data.putByte(0x35, device.getGeometry().sectorsPerTrack());
                disk.writeVtoc(data.asBytes());
            }), "VTOC sectors per track (%d) does not match device sectors per track (%d).",
                    data.getUnsignedByte(0x35), device.getGeometry().sectorsPerTrack());
        }
    }

    private void checkVtocBitMap() {
        List<DosSectorAddress> allocationErrors = new ArrayList<>();
        byte[] vtoc = disk.readVtoc();
        for (int track = 0; track < disk.getTracks(); track++) {
            for (int sector = 0; sector < disk.getSectors(); sector++) {
                if (usedSectors.get(track, sector) != disk.isSectorUsed(track, sector, vtoc)) {
                    allocationErrors.add(new DosSectorAddress(track, sector));
                }
            }
        }

        if (!allocationErrors.isEmpty()) {
            String description = String.format("The VTOC free sector bitmap has %d allocation errors.", allocationErrors.size());
            Optional<Runnable> action = Optional.empty();
            if (allocationErrors.size() < 10) {
                // Note that we offer to fix the allocation error if the count is rather minimal.
                description = String.format("The VTOC free sector bitmap is incorrect for the following: %s.", allocationErrors);
                action = Optional.of(() -> {
                    for (int track = 0; track < disk.getTracks(); track++) {
                        for (int sector = 0; sector < disk.getSectors(); sector++) {
                            if (usedSectors.get(track, sector)) {
                                disk.setSectorUsed(track, sector, vtoc);
                            }
                            else {
                                disk.setSectorFree(track, sector, vtoc);
                            }
                        }
                    }
                    disk.writeVtoc(vtoc);
                });
            }
            addFinding("vtoc", 17, 0, action, description);
        }
    }

    // TODO should List<Finding> become Findings which has helper methods like this and/or a builder mechanism?
    private void addFinding(String classification, int track, int sector, Optional<Runnable> action,
                            String fmt, Object... args) {
        String description = String.format(fmt, args);
        Finding finding = new Finding(description, action, classification, new Coordinate(track, sector));
        findings.add(finding);
    }

    /**
     * Centralized placement of track/sectors and the bit position calculation.
     */
    public static class UsedSectors {
        private final BitSet usedSectors = new BitSet();

        public void set(int track, int sector) {
            usedSectors.set(computeBitPosition(track, sector));
        }
        public boolean get(int track, int sector) {
            return usedSectors.get(computeBitPosition(track, sector));
        }

        private int computeBitPosition(int track, int sector) {
            return track*32 + sector;
        }
    }
}
