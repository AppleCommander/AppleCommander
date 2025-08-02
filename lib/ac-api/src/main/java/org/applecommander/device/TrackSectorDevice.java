package org.applecommander.device;

import org.applecommander.capability.CapabilityProvider;
import org.applecommander.util.DataBuffer;

public interface TrackSectorDevice extends CapabilityProvider {
    DataBuffer readSector(int track, int sector);
    void writeSector(int track, int sector, DataBuffer data);
}
