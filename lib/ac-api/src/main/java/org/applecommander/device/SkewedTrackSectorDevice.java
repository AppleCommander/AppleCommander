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
import org.applecommander.hint.Hint;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;

import java.util.Optional;

/**
 * This is an overlay on a TrackSectorDevice to give the proper sector skew to the device.
 * <p/>
 * <pre>
 * From Beneath Apple DOS (2020).
 * -------- SECTOR ORDERING --------
 * Physical*  DOS 3.3  Pascal*  CP/M
 *     0         0        0       0
 *     1         D        2       3
 *     2         B        4       6
 *     3         9        6       9
 *     4         7        8       C
 *     5         5        A       F
 *     6         3        C       2
 *     7         1        E       5
 *     8         E        1       8
 *     9         C        3       B
 *     A         A        5       E
 *     B         8        7       1
 *     C         6        9       4
 *     D         4        B       7
 *     E         2        D       A
 *     F         F        F       D
 * </pre>
 * <ul>Notes:
 * <li>Physical = RDOS and DOS 3.2 sector ordering</li>
 * <li>Pascal = ProDOS sector ordering</li>
 * </ul>
 */
public class SkewedTrackSectorDevice implements TrackSectorDevice {
    public static TrackSectorDevice physicalToDosSkew(TrackSectorDevice device) {
        return new SkewedTrackSectorDevice(device,
                0x0, 0xd, 0xb, 0x9, 0x7, 0x5, 0x3, 0x1,
                0xe, 0xc, 0xa, 0x8, 0x6, 0x4, 0x2, 0xf);
    }
    public static TrackSectorDevice physicalToPascalSkew(TrackSectorDevice device) {
        return new SkewedTrackSectorDevice(device,
                0x0, 0x2, 0x4, 0x6, 0x8, 0xa, 0xc, 0xe,
                0x1, 0x3, 0x5, 0x7, 0x9, 0xb, 0xd, 0xf);
    }
    public static TrackSectorDevice pascalToPhysicalSkew(TrackSectorDevice device) {
        return new SkewedTrackSectorDevice(device,
                0x0, 0x8, 0x1, 0x9, 0x2, 0xa, 0x3, 0xb,
                0x4, 0xc, 0x5, 0xd, 0x6, 0xe, 0x7, 0xf);
    }
    public static TrackSectorDevice dosToPhysicalSkew(TrackSectorDevice device) {
        return new SkewedTrackSectorDevice(device,
                0x0, 0x7, 0xe, 0x6, 0xd, 0x5, 0xc, 0x4,
                0xb, 0x3, 0xa, 0x2, 0x9, 0x1, 0x8, 0xf);
    }
    public static TrackSectorDevice dosToPascalSkew(TrackSectorDevice device) {
        return new SkewedTrackSectorDevice(device,
                0x0, 0xe, 0xd, 0xc, 0xb, 0xa, 0x9, 0x8,
                0x7, 0x6, 0x5, 0x4, 0x3, 0x2, 0x1, 0xf);
    }
    // CP/M skews are from 'cpmtools'
    public static TrackSectorDevice dosToCpmSkew(TrackSectorDevice device) {
        return new SkewedTrackSectorDevice(device, 0,6,12,3,9,15,14,5,11,2,8,7,13,4,10,1);
    }
    public static TrackSectorDevice pascalToCpmSkew(TrackSectorDevice device) {
        return new SkewedTrackSectorDevice(device, 0,9,3,12,6,15,1,10,4,13,7,8,2,11,5,14);
    }
    // Special RDOS "skew" for truncation (from 16 sector to 13 sector)
    public static TrackSectorDevice truncate16sectorTo13(TrackSectorDevice device) {
        return new SkewedTrackSectorDevice(device,
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    }

    private final TrackSectorDevice device;
    private final int[] sectorSkew;
    private final Geometry geometry;

    private SkewedTrackSectorDevice(TrackSectorDevice device, int... sectorSkew) {
        assert(sectorSkew.length == 16 || sectorSkew.length == 13);
        this.device = device;
        this.sectorSkew = sectorSkew;
        this.geometry = new Geometry(device.getGeometry().tracksOnDisk(), sectorSkew.length);
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, device);
    }

    @Override
    public boolean is(Hint hint) {
        return device.is(hint);
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
        return geometry;
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

    @Override
    public void format() {
        device.format();
    }
}
