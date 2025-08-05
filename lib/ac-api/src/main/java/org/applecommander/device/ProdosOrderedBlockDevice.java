package org.applecommander.device;

import org.applecommander.capability.Capability;
import org.applecommander.source.Source;
import org.applecommander.util.DataBuffer;

public class ProdosOrderedBlockDevice implements BlockDevice {
    private final Source source;
    private final Geometry geometry;

    public ProdosOrderedBlockDevice(Source source, int blockSize, int blocksOnDevice) {
        this.source = source;
        this.geometry = new Geometry(blockSize, blocksOnDevice);
    }

    @Override
    public boolean can(Capability capability) {
        return capability == Capability.WRITE_BLOCK;
    }

    @Override
    public Geometry getGeometry() {
        return geometry;
    }

    @Override
    public DataBuffer readBlock(int block) {
        assert(block < geometry.blocksOnDevice());
        return source.readBytes(block* geometry.blockSize(), geometry.blockSize());
    }

    @Override
    public void writeBlock(int block, DataBuffer blockData) {
        assert(block < geometry.blocksOnDevice());
        assert(blockData.limit() == geometry.blockSize());
        source.writeBytes(block*geometry.blockSize(), blockData);
    }
}
