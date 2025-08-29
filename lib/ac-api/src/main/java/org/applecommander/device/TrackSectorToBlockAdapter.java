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
import java.util.function.BiConsumer;

public class TrackSectorToBlockAdapter implements BlockDevice {
    private final TrackSectorDevice device;
    private final Geometry geometry;

    public TrackSectorToBlockAdapter(TrackSectorDevice device) {
        this.device = device;
        this.geometry = new Geometry(STANDARD_BLOCK_SIZE, device.getGeometry().sectorsPerDisk() / 2);
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
        return capability == Capability.WRITE_BLOCK && device.can(Capability.WRITE_SECTOR);
    }

    @Override
    public Geometry getGeometry() {
        return geometry;
    }

    @Override
    public DataBuffer readBlock(int block) {
        DataBuffer data = DataBuffer.create(STANDARD_BLOCK_SIZE);
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
