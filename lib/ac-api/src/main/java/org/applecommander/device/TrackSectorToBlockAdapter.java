package org.applecommander.device;

import org.applecommander.capability.Capability;
import org.applecommander.util.DataBuffer;

import java.util.function.BiConsumer;

public class TrackSectorToBlockAdapter implements BlockDevice {
    private final TrackSectorDevice device;
    private final Geometry geometry;

    public TrackSectorToBlockAdapter(TrackSectorDevice device) {
        this.device = device;
        this.geometry = new Geometry(BLOCK_SIZE, device.getGeometry().getSectorsPerDisk() / 2);
    }

    @Override
    public boolean can(Capability capability) {
        return capability == Capability.WRITE_BLOCK && device.can(Capability.WRITE_SECTOR);
    }

    @Override
    public Geometry getGeometry() {
        return geometry;
    }

    @Override
    public DataBuffer readBlock(int block) {
        DataBuffer data = DataBuffer.create(BLOCK_SIZE);
        operate(block,
                (t,s) -> data.put(0, device.readSector(t,s)),
                (t,s) -> data.put(256, device.readSector(t,s)));
        return data;
    }

    @Override
    public void writeBlock(int block, DataBuffer blockData) {
        operate(block,
                (t,s) -> device.writeSector(t,s,blockData.slice(0,256)),
                (t,s) -> device.writeSector(t,s,blockData.slice(256,512)));
    }

    public void operate(int block, BiConsumer<Integer,Integer> operation1, BiConsumer<Integer,Integer> operation2) {
        int track = block / 8;
        int sectorIndex = block % 8;
        int[] sectorMapping1 = { 0, 13, 11, 9, 7, 5, 3, 1 };
        int[] sectorMapping2 = { 14, 12, 10, 8, 6, 4, 2, 15 };
        int sector1 = sectorMapping1[sectorIndex];
        int sector2 = sectorMapping2[sectorIndex];
        operation1.accept(track, sector1);
        operation2.accept(track, sector2);
    }
}
