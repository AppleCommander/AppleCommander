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
import org.applecommander.util.InformationGroup;

import java.util.List;
import java.util.Optional;

/**
 * Provides a _very simple_ mapping from Track/Sectors to blocks. All sector skewing is expected
 * to be handled in the TrackSectorDevice. As such, this device expects to map sectors in the given
 * ("natural") order. (That is, ProDOS blocks are sectors 0,1 and 2,3 and 3,4 etc. RDOS is in physical
 * order. CP/M is in CP/M's expected order.)
 */
public class TrackSectorToBlockAdapter implements BlockDevice {
    private final TrackSectorDevice device;
    private final BlockStyle style;
    private final Geometry geometry;

    public TrackSectorToBlockAdapter(TrackSectorDevice device, BlockStyle style) {
        this.device = device;
        this.style = style;
        this.geometry = new Geometry(style.blockSize, device.getGeometry().deviceSize() / style.blockSize);
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
        return capability == Capability.WRITE_BLOCK && device.can(Capability.WRITE_SECTOR);
    }

    @Override
    public Geometry getGeometry() {
        return geometry;
    }

    @Override
    public DataBuffer readBlock(int block) {
        Coordinate.TrackAndSectorCoordinate coordinate = calculateTrackSectorCoordinate(block);
        return device.readRange(coordinate.track(), coordinate.sector(), style.sectorsPerBlock);
    }

    /// Read a range of blocks.
    @Override
    public DataBuffer readRange(int block, int totalBlocks) {
        Coordinate.TrackAndSectorCoordinate coordinate = calculateTrackSectorCoordinate(block);
        return device.readRange(coordinate.track(), coordinate.sector(), style.sectorsPerBlock * totalBlocks);
    }

    @Override
    public void writeBlock(int block, DataBuffer blockData) {
        assert blockData.limit() == style.blockSize;
        Coordinate.TrackAndSectorCoordinate coordinate = calculateTrackSectorCoordinate(block);
        device.writeRange(coordinate.track(), coordinate.sector(), sizeBuffer(blockData));
    }

    /// Write a range of blocks.
    @Override
    public void writeRange(int block, DataBuffer rangeData) {
        Coordinate.TrackAndSectorCoordinate coordinate = calculateTrackSectorCoordinate(block);
        device.writeRange(coordinate.track(), coordinate.sector(), sizeBuffer(rangeData));
    }

    @Override
    public List<InformationGroup> information() {
        return InformationGroup.builder("Track/Sector to Block Adapter")
            .item("Block Style").value(style.name())
            .item("Block Size (Bytes)").value(style.blockSize)
            .item("Sectors per Block").value(style.sectorsPerBlock)
            .merge(geometry)
            .get(device);
    }

    @Override
    public void format() {
        device.format();
    }

    public Coordinate.TrackAndSectorCoordinate calculateTrackSectorCoordinate(int block) {
        int physicalSector = block * style.sectorsPerBlock;
        int track = physicalSector / device.getGeometry().sectorsPerTrack();
        int sector = physicalSector % device.getGeometry().sectorsPerTrack();
        return Coordinate.trackAndSector(track, sector);
    }

    public enum BlockStyle {
        RDOS(256),
        PASCAL(512),
        PRODOS(512),
        CPM(1024);

        public final int blockSize;
        public final int sectorsPerBlock;

        BlockStyle(int blockSize) {
            assert blockSize % TrackSectorDevice.SECTOR_SIZE == 0;
            this.blockSize = blockSize;
            this.sectorsPerBlock = blockSize / TrackSectorDevice.SECTOR_SIZE;
        }
    }
}
