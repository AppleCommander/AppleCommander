package org.applecommander.device;

import org.applecommander.capability.CapabilityProvider;
import org.applecommander.util.DataBuffer;

public interface BlockDevice extends CapabilityProvider {
    int BLOCK_SIZE = 512;

    Geometry getGeometry();
    DataBuffer readBlock(int block);
    void writeBlock(int block, DataBuffer blockData);

    record Geometry(int blockSize, int blocksOnDevice) {}
}
