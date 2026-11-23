/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2026 by Robert Greene and others
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
package org.applecommander.os.gutenberg;

import org.applecommander.device.Coordinate;
import org.applecommander.device.Coordinate.TrackAndSectorCoordinate;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.util.DataBuffer;

import java.util.HashSet;
import java.util.Set;

public class GutenbergFileHelper implements GutenbergConstants {
    private final TrackSectorDevice device;

    GutenbergFileHelper(TrackSectorDevice device) {
        this.device = device;
    }

    public void follow(int track, int sector, SectorHandler handler) {
        Set<TrackAndSectorCoordinate> visited = new HashSet<>();
        boolean atEnd = false;
        while (!atEnd) {
            atEnd = track >= 0x80 || sector >= 0x80;
            track &= 0x7f;
            sector &= 0x7f;
            if (track == 0x40) track = 0;
            if (sector == 0x40) sector = 0;

            // Break on invalid coordinates
            if (!device.getGeometry().isValid(track, sector)) {
                break;
            }

            // Prevent circular references
            TrackAndSectorCoordinate coordinate = Coordinate.trackAndSector(track, sector);
            if (visited.contains(coordinate)) {
                break;
            }
            visited.add(coordinate);

            DataBuffer sectorData = device.readSector(track, sector);
            handler.process(track, sector, sectorData);
            track = sectorData.getUnsignedByte(NEXT_TRACK);
            sector = sectorData.getUnsignedByte(NEXT_SECTOR);
        }
    }

    public interface SectorHandler {
        void process(int currentTrack, int currentSector, DataBuffer sectorData);
    }
}
