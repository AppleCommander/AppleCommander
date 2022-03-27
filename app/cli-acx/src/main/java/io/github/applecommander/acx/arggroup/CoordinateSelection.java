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

import com.webcodepro.applecommander.storage.Disk;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class CoordinateSelection {
    @ArgGroup(exclusive = false)
    private CoordinateSelection.SectorCoordinateSelection sectorCoordinate;
    @ArgGroup(exclusive = false)
    private CoordinateSelection.BlockCoordinateSelection blockCoordinate;
    
    public byte[] read(Disk disk) {
        if (sectorCoordinate != null) {
            return sectorCoordinate.read(disk);
        }
        else if (blockCoordinate != null) {
            return blockCoordinate.read(disk);
        }
        return disk.readSector(0, 0);
    }
    
    public void write(Disk disk, byte[] data) {
        if (sectorCoordinate != null) {
            sectorCoordinate.write(disk, data);
        }
        else if (blockCoordinate != null) {
            blockCoordinate.write(disk, data);
        }
        disk.writeSector(0, 0, data);
    }
    
    public static class SectorCoordinateSelection {
        @Option(names = { "-t", "--track" }, required = true, description = "Track number.")
        private Integer track;
        @Option(names = { "-s", "--sector" }, required = true, description = "Sector number.")
        private Integer sector;
        
        public byte[] read(Disk disk) {
            return disk.readSector(track, sector);
        }
        public void write(Disk disk, byte[] data) {
            disk.writeSector(track, sector, data);
        }
    }
    public static class BlockCoordinateSelection {
        @Option(names = { "-b", "--block" }, description = "Block number.")
        private Integer block;
        
        public byte[] read(Disk disk) {
            return disk.readBlock(block);
        }
        public void write(Disk disk, byte[] data) {
            disk.writeBlock(block, data);
        }
    }
}