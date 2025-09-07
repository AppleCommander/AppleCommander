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
     * <p/>
     * NOTE: Adapter type devices have to be cautious about what device is responsible
     * about formatting. For example, a UniDOS disk is 2x400K volumes on an 800K
     * block device -- if they defer formatting to the 800K block device, a format on
     * one volume also wipes out the other (in this case, do not defer to the "parent").
     * However, if the block adapter contains a nibble-based TrackSectorDevice, the
     * actual formatting needs to get to the nibble device so it can lay down sector
     * markers and the rest of the track structure.
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
        public int deviceSize() {
            return sectorsPerDisk() * SECTOR_SIZE;
        }
    }
}
