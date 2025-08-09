package org.applecommander.device;

import org.applecommander.capability.CapabilityProvider;
import org.applecommander.util.DataBuffer;

public interface TrackSectorDevice extends CapabilityProvider {
    int SECTOR_SIZE = 256;

    Geometry getGeometry();
    DataBuffer readSector(int track, int sector);
    void writeSector(int track, int sector, DataBuffer data);
    record Geometry(int tracksOnDisk, int sectorsPerTrack) {
        int getSectorsPerDisk() {
            return tracksOnDisk*sectorsPerTrack;
        }
    }
}
