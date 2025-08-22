/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2025 by Robert Greene and others
 * robgreene at users.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
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
