package org.applecommander.device;

public class ProdosBlockToTrackSectorAdapterStrategy implements TrackSectorToBlockStrategy {
    /**
     * This table contains the block offset for a particular DOS sector.
     */
    public static final int[] blockInterleave = {
        0, 7, 6, 6, 5, 5, 4, 4, 3, 3, 2, 2, 1, 1, 0, 7
    };
    /**
     * Defines the location within a block in which the DOS sector resides.
     * (0 = 0-255 and 1 = 256-511.)
     */
    public static final int[] blockOffsets = {
        0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1
    };

    @Override
    public int computeBlock(int track, int sector) {
        return track * 8 + blockInterleave[sector];
    }

    @Override
    public int computeOffset(int track, int sector) {
        return blockOffsets[sector] * TrackSectorDevice.SECTOR_SIZE;
    }

    @Override
    public int getTotalTracks() {
        return 35;
    }

    @Override
    public int getSectorsPerTrack() {
        return 16;
    }

    @Override
    public String getName() {
        return "Prodos block adapter";
    }
}
