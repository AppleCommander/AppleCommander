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

import com.webcodepro.applecommander.storage.DiskConstants;
import org.applecommander.capability.Capability;
import org.applecommander.hint.Hint;
import org.applecommander.source.Source;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;

import java.util.Optional;

public class DosOrderedTrackSectorDevice implements TrackSectorDevice {
    private final Source source;
    private final Geometry geometry;
    private final Hint orderHint;

    // TODO FIXME do we still need an "unknown" ordered disk? If so, name kinda suchs. :-)
    public DosOrderedTrackSectorDevice(Source source) {
        this.source = source;
        this.geometry = calculateGeometry(source);
        this.orderHint = null;
    }
    public DosOrderedTrackSectorDevice(Source source, Hint orderHint) {
        this.source = source;
        this.geometry = calculateGeometry(source);
        this.orderHint = orderHint;
    }
    private static Geometry calculateGeometry(Source source) {
        if (source.isApproxEQ(DiskConstants.APPLE_13SECTOR_DISK)) {
            int tracksOnDisk = source.getSize() / (13 * SECTOR_SIZE);
            return new Geometry(tracksOnDisk, 13);
        }
        else {
            int tracksOnDisk = source.getSize() / (16 * SECTOR_SIZE);
            return new Geometry(tracksOnDisk, 16);
        }
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, source);
    }

    @Override
    public boolean is(Hint hint) {
        return hint == orderHint;
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
        return source.readBytes(calculateOffset(track,sector), SECTOR_SIZE);
    }

    @Override
    public void writeSector(int track, int sector, DataBuffer data) {
        assert(data.limit() == SECTOR_SIZE);
        source.writeBytes(calculateOffset(track,sector), data);
    }

    public int calculateOffset(int track, int sector) {
        assert(track < geometry.tracksOnDisk());
        assert(sector < geometry.sectorsPerTrack());
        return (track * geometry.sectorsPerTrack() + sector) * SECTOR_SIZE;
    }
}
