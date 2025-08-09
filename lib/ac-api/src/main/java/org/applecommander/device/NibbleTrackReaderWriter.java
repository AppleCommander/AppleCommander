package org.applecommander.device;

import org.applecommander.capability.CapabilityProvider;
import org.applecommander.util.DataBuffer;

public interface NibbleTrackReaderWriter extends CapabilityProvider {
    /**
     * The number of physical tracks on this device.
     */
    int getTracksOnDevice();
    /**
     * Read nibbilized track data.
     */
    DataBuffer readTrackData(int track);
    /**
     * Write nibbilized track data.
     */
    void writeTrackData(int track, DataBuffer data);
}
