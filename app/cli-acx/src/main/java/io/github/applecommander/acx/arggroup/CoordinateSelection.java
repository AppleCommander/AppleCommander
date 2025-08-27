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

    public byte[] read(FormattedDisk disk) {
        if (sectorCoordinate != null) {
            return sectorCoordinate.read(disk);
        }
        else if (blockCoordinate != null) {
            return blockCoordinate.read(disk);
        }
        TrackSectorDevice device = TrackSectorDeviceAdapter.from(disk);
        return device.readSector(0, 0).asBytes();
    }

    public void write(FormattedDisk disk, byte[] data) {
        if (sectorCoordinate != null) {
            sectorCoordinate.write(disk, data);
        }
        else if (blockCoordinate != null) {
            blockCoordinate.write(disk, data);
        }
        TrackSectorDevice device = TrackSectorDeviceAdapter.from(disk);
        device.writeSector(0, 0, DataBuffer.wrap(data));
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

        public byte[] read(FormattedDisk disk) {
            TrackSectorDevice device = TrackSectorDeviceAdapter.from(disk);
            validateTrackAndSector(device);
            return device.readSector(track, sector).asBytes();
        }

        public void write(FormattedDisk disk, byte[] data) {
            TrackSectorDevice device = TrackSectorDeviceAdapter.from(disk);
            validateTrackAndSector(device);
            device.writeSector(track, sector, DataBuffer.wrap(data));
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

        public byte[] read(FormattedDisk disk) {
            BlockDevice device = BlockDeviceAdapter.from(disk);
            validateBlockNum(device);
            return device.readBlock(block).asBytes();
        }

        public void write(FormattedDisk disk, byte[] data) {
            BlockDevice device = BlockDeviceAdapter.from(disk);
            validateBlockNum(device);
            device.writeBlock(block, DataBuffer.wrap(data));
        }
    }
}
