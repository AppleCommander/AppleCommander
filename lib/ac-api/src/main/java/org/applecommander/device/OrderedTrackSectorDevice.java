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
import org.applecommander.source.Source;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.InformationGroup;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OrderedTrackSectorDevice implements TrackSectorDevice {
    private final Source source;
    private final SectorOrder sectorOrder;
    private final Geometry geometry;

    public OrderedTrackSectorDevice(Source source, SectorOrder sectorOrder) {
        this.source = source;
        this.sectorOrder = sectorOrder;
        this.geometry = calculateGeometry(source);
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
    public boolean can(Capability capability) {
        return capability == Capability.WRITE_SECTOR;
    }

    @Override
    public Geometry getGeometry() {
        return geometry;
    }

    /// Get the sector order.
    @Override
    public SectorOrder getSectorOrder() {
        return sectorOrder ;
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
        assert track < geometry.tracksOnDisk();
        assert sector < geometry.sectorsPerTrack();
        return (track * geometry.sectorsPerTrack() + sector) * SECTOR_SIZE;
    }

    @Override
    public List<InformationGroup> information() {
        return InformationGroup.builder("Ordered Track/Sector Device")
            .item("Sector Order").value(sectorOrder.name())
            .item("Sector Organization").value(Arrays.stream(sectorOrder.physicalSectorOrder)
                        .mapToObj(String::valueOf)
                        .collect(Collectors.joining(",")))
            .merge(geometry)
            .get(source);
    }
}
