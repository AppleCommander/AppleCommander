package org.applecommander.device;

import org.applecommander.capability.Capability;
import org.applecommander.util.DataBuffer;

/**
 * Adapts a <code>BlockDevice</code> to a <code>TrackSectorDevice</code>.
 */
public class BlockToTrackSectorAdapter implements TrackSectorDevice {
    private final BlockDevice device;
    private final TrackSectorToBlockStrategy strategy;
    private final Geometry geometry;

    public BlockToTrackSectorAdapter(BlockDevice device, TrackSectorToBlockStrategy strategy) {
        this.device = device;
        this.strategy = strategy;
        this.geometry = new Geometry(strategy.getTotalTracks(), strategy.getSectorsPerTrack());
    }

    @Override
    public boolean can(Capability capability) {
        return device.can(capability);
    }

    @Override
    public Geometry getGeometry() {
        return geometry;
    }

    @Override
    public DataBuffer readSector(int track, int sector) {
        int block = strategy.computeBlock(track, sector);
        DataBuffer blockData = device.readBlock(block);
        int offset = strategy.computeOffset(track, sector);
        return blockData.slice(offset, SECTOR_SIZE);
    }

    @Override
    public void writeSector(int track, int sector, DataBuffer data) {
        assert(data.limit() == SECTOR_SIZE);
        int block = strategy.computeBlock(track, sector);
        DataBuffer blockData = device.readBlock(block);
        int offset = strategy.computeOffset(track, sector);
        blockData.put(offset, data);
        device.writeBlock(block, blockData);
    }
}
