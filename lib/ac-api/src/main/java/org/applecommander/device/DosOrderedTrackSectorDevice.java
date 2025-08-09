package org.applecommander.device;

import org.applecommander.capability.Capability;
import org.applecommander.source.Source;
import org.applecommander.util.DataBuffer;

public class DosOrderedTrackSectorDevice implements TrackSectorDevice {
    private final Source source;
    private final Geometry geometry;

    public DosOrderedTrackSectorDevice(Source source) {
        this.source = source;
        this.geometry = new Geometry(35, 16);   // assumed for now?
    }

    @Override
    public boolean can(Capability capability) {
        return capability == Capability.WRITE_SECTOR;
    }

    @Override
    public Geometry getGeometry() {
        return geometry;
    }

    @Override
    public DataBuffer readSector(int track, int sector) {
        assert(track < geometry.tracksOnDisk());
        assert(sector < geometry.sectorsPerTrack());
        return source.readBytes((track*16+sector)*SECTOR_SIZE, SECTOR_SIZE);
    }

    @Override
    public void writeSector(int track, int sector, DataBuffer data) {
        assert(track < geometry.tracksOnDisk());
        assert(sector < geometry.sectorsPerTrack());
        assert(data.limit() == SECTOR_SIZE);
        source.writeBytes((track*16+sector)*SECTOR_SIZE, data);
    }
}
