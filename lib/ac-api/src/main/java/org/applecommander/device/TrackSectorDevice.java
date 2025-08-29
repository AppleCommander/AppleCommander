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

import org.applecommander.util.DataBuffer;

public interface TrackSectorDevice extends Device {
    int SECTOR_SIZE = 256;

    Geometry getGeometry();
    DataBuffer readSector(int track, int sector);
    void writeSector(int track, int sector, DataBuffer data);
    /**
     * Format a disk. For most disks, this is simply a wipe to all zeros. If this
     * disk has extended format (such as nibble formats), this is the opportunity
     * to write out that format.
     */
    default void format() {
        DataBuffer sectorData = DataBuffer.create(SECTOR_SIZE);
        for (int track = 0; track < getGeometry().tracksOnDisk(); track++) {
            for (int sector = 0; sector < getGeometry().sectorsPerTrack(); sector++) {
                writeSector(track, sector, sectorData);
            }
        }
    }

    record Geometry(int tracksOnDisk, int sectorsPerTrack) {
        public int sectorsPerDisk() {
            return tracksOnDisk*sectorsPerTrack;
        }
    }
}
