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
import org.applecommander.source.Source;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;

import java.util.Optional;

public class DosOrderedTrackSectorDevice implements TrackSectorDevice {
    private final Source source;
    private final Geometry geometry;

    public DosOrderedTrackSectorDevice(Source source) {
        this.source = source;
        this.geometry = new Geometry(35, 16);   // assumed for now?
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, source);
    }

    @Override
    public boolean is(Hint hint) {
        return hint == Hint.DOS_SECTOR_ORDER;
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
        assert(track < geometry.tracksOnDisk());
        assert(sector < geometry.sectorsPerTrack());
        return source.readBytes((track*16+sector)*SECTOR_SIZE, SECTOR_SIZE);
    }

    @Override
    public void writeSector(int track, int sector, DataBuffer data) {
        assert(track < geometry.tracksOnDisk());
        assert(sector < geometry.sectorsPerTrack());
        assert(data.limit() == SECTOR_SIZE);
        source.writeBytes((track*16+sector)*SECTOR_SIZE, data);
    }
}
