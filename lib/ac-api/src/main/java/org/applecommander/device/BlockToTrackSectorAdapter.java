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
 * Adapts a <code>BlockDevice</code> to a <code>TrackSectorDevice</code>.
 */
public class BlockToTrackSectorAdapter implements TrackSectorDevice {
    private final BlockDevice device;
    private final TrackSectorToBlockStrategy strategy;
    private final Geometry geometry;

    public BlockToTrackSectorAdapter(BlockDevice device, TrackSectorToBlockStrategy strategy) {
        this.device = device;
        this.strategy = strategy;
        this.geometry = new Geometry(strategy.getTotalTracks(), strategy.getSectorsPerTrack());
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, device);
    }

    @Override
    public boolean is(Hint hint) {
        return hint == Hint.PRODOS_BLOCK_ORDER;
    }

    @Override
    public boolean can(Capability capability) {
        return device.can(capability);
    }

    @Override
    public Geometry getGeometry() {
        return geometry;
    }

    @Override
    public DataBuffer readSector(int track, int sector) {
        int block = strategy.computeBlock(track, sector);
        DataBuffer blockData = device.readBlock(block);
        int offset = strategy.computeOffset(track, sector);
        return blockData.slice(offset, SECTOR_SIZE);
    }

    @Override
    public void writeSector(int track, int sector, DataBuffer data) {
        assert(data.limit() == SECTOR_SIZE);
        int block = strategy.computeBlock(track, sector);
        DataBuffer blockData = device.readBlock(block);
        int offset = strategy.computeOffset(track, sector);
        blockData.put(offset, data);
        device.writeBlock(block, blockData);
    }
}
