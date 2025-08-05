package org.applecommander.device;

import org.applecommander.capability.Capability;
import org.applecommander.util.DataBuffer;

public class SkewedTrackSectorDevice implements TrackSectorDevice {
    public static TrackSectorDevice dosToPhysicalSkew(TrackSectorDevice device) {
        return new SkewedTrackSectorDevice(device,
                0x0, 0xd, 0xb, 0x9, 0x7, 0x5, 0x3, 0x1,
                0xe, 0xc, 0xa, 0x8, 0x6, 0x4, 0x2, 0xf);
    }

    private final TrackSectorDevice device;
    private final int[] sectorSkew;

    private SkewedTrackSectorDevice(TrackSectorDevice device, int... sectorSkew) {
        assert(sectorSkew.length == 16);
        this.device = device;
        this.sectorSkew = sectorSkew;
    }

    @Override
    public boolean can(Capability capability) {
        if (capability == Capability.WRITE_SECTOR) {
            return device.can(Capability.WRITE_SECTOR);
        }
        return false;
    }

    @Override
    public Geometry getGeometry() {
        return device.getGeometry();
    }

    @Override
    public DataBuffer readSector(int track, int sector) {
        assert(sector >= 0 && sector < sectorSkew.length);
        return device.readSector(track, sectorSkew[sector]);
    }

    @Override
    public void writeSector(int track, int sector, DataBuffer data) {
        assert(sector >= 0 && sector < sectorSkew.length);
        device.writeSector(track, sectorSkew[sector], data);
    }
}
