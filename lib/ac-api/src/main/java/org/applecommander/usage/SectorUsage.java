package org.applecommander.usage;

import org.applecommander.device.TrackSectorDevice;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.IntSupplier;

/// Provides sector level usage information. Sectors are always the same size in all contexts,
/// so it is hard-coded.
public class SectorUsage extends DiskUsage {
    private final BiFunction<Integer,Integer,Boolean> usedFn;
    private final int totalTracks;
    private final int totalSectors;

    public SectorUsage(IntSupplier used, IntSupplier free, BiFunction<Integer,Integer,Boolean> usedFn,
                       int totalTracks, int totalSectors) {
        super(TrackSectorDevice.SECTOR_SIZE, used, free);
        Objects.requireNonNull(usedFn);
        this.usedFn = usedFn;
        this.totalTracks = totalTracks;
        this.totalSectors = totalSectors;
    }

    public boolean isUsed(int track, int sector) {
        return usedFn.apply(track, sector);
    }
    public int getTotalTracks() {
        return totalTracks;
    }
    public int getTotalSectors() {
        return totalSectors;
    }
}
