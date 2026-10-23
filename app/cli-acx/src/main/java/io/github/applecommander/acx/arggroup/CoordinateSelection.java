/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2019-2022 by Robert Greene and others
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
package io.github.applecommander.acx.arggroup;

import com.webcodepro.applecommander.storage.BlockDeviceAdapter;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.TrackSectorDeviceAdapter;
import io.github.applecommander.acx.converter.IntegerTypeConverter;
import org.applecommander.device.BlockDevice;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.util.DataBuffer;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class CoordinateSelection {
    @ArgGroup(exclusive = false)
    private CoordinateSelection.SectorCoordinateSelection sectorCoordinate;
    @ArgGroup(exclusive = false)
    private CoordinateSelection.BlockCoordinateSelection blockCoordinate;
    @Option(names = { "-n", "--count" }, defaultValue = "1", description = "Number of sectors/blocks to read/write.")
    private int count = 1;

    public byte[] read(FormattedDisk disk) {
        if (sectorCoordinate != null) {
            return sectorCoordinate.read(disk, count);
        }
        else if (blockCoordinate != null) {
            return blockCoordinate.read(disk, count);
        }
        TrackSectorDevice device = TrackSectorDeviceAdapter.from(disk);
        return device.readSector(0, 0).asBytes();
    }

    public void write(FormattedDisk disk, byte[] data) {
        if (sectorCoordinate != null) {
            sectorCoordinate.write(disk, count, data);
        }
        else if (blockCoordinate != null) {
            blockCoordinate.write(disk, count, data);
        }
        else {
            TrackSectorDevice device = TrackSectorDeviceAdapter.from(disk);
            device.writeSector(0, 0, DataBuffer.wrap(data));
        }
    }

    public static class SectorCoordinateSelection {
        @Option(names = { "-t", "--track" }, required = true, description = "Track number.",
                converter = IntegerTypeConverter.class)
        private Integer track;
        @Option(names = { "-s", "--sector" }, required = true, description = "Sector number.",
                converter = IntegerTypeConverter.class)
        private Integer sector;

        public void validateTrackAndSector(TrackSectorDevice device) throws IllegalArgumentException  {
            final int tracksPerDisk = device.getGeometry().tracksOnDisk();
            final int sectorsPerTrack = device.getGeometry().sectorsPerTrack();

            if (track < 0 || track >= tracksPerDisk) {
                String errormsg = String.format("The track number(%d) is out of range(0-%d) on this image.", track, tracksPerDisk-1);
                throw new IllegalArgumentException(errormsg);
            }

            if (sector < 0 || sector >= sectorsPerTrack) {
                String errormsg = String.format("The sector number(%d) is out of range(0-%d) on this image.", sector, sectorsPerTrack-1);
                throw new IllegalArgumentException(errormsg);
            }
        }

        public byte[] read(FormattedDisk disk, int count) {
            TrackSectorDevice device = TrackSectorDeviceAdapter.from(disk);
            validateTrackAndSector(device);
            return device.readRange(track, sector, count).asBytes();
        }

        public void write(FormattedDisk disk, int count, byte[] data) {
            TrackSectorDevice device = TrackSectorDeviceAdapter.from(disk);
            validateTrackAndSector(device);
            if (data.length < TrackSectorDevice.SECTOR_SIZE) {
                throw new IllegalArgumentException("The data length smaller than a sector in size.");
            }
            else if (data.length % TrackSectorDevice.SECTOR_SIZE != 0) {
                throw new IllegalArgumentException("The data length is not a multiple of a sector in size.");
            }
            DataBuffer dataBuffer = DataBuffer.wrap(data, 0, count * TrackSectorDevice.SECTOR_SIZE);
            device.writeRange(track, sector, dataBuffer);
        }
    }
    public static class BlockCoordinateSelection {
        @Option(names = { "-b", "--block" }, description = "Block number.", converter = IntegerTypeConverter.class)
        private Integer block;

        public void validateBlockNum(BlockDevice device) throws IllegalArgumentException {
            final int blocksOnDevice = device.getGeometry().blocksOnDevice();

            if (block < 0 || block >= blocksOnDevice) {
                String errormsg = String.format("The block number(%d) is out of range(0-%d) on this image.", block, blocksOnDevice-1);
                throw new IllegalArgumentException(errormsg);
            }
        }

        public byte[] read(FormattedDisk disk, int count) {
            BlockDevice device = BlockDeviceAdapter.from(disk);
            validateBlockNum(device);
            return device.readRange(block, count).asBytes();
        }

        public void write(FormattedDisk disk, int count, byte[] data) {
            BlockDevice device = BlockDeviceAdapter.from(disk);
            validateBlockNum(device);
            if (data.length < device.getGeometry().blockSize()) {
                throw new IllegalArgumentException("The data length smaller than a block in size.");
            }
            else if (data.length % device.getGeometry().blockSize() != 0) {
                throw new IllegalArgumentException("The data length is not a multiple of a block in size.");
            }
            DataBuffer dataBuffer = DataBuffer.wrap(data, 0, count * device.getGeometry().blockSize());
            device.writeRange(block, dataBuffer);
        }
    }
}
