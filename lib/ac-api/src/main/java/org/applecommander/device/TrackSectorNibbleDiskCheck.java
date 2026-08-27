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
package org.applecommander.device;

import com.webcodepro.applecommander.util.Range;
import org.applecommander.capability.Capability;
import org.applecommander.os.Coordinate;
import org.applecommander.os.DiskCheck;
import org.applecommander.util.DataBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.applecommander.device.TrackSectorDevice.SECTOR_SIZE;

/**
 * Provides a "low-level" scan of a nibble device. If any sectors cannot be found, adds a Finding
 * which allows the user to over-write the track (first, reading sectors from the track).
 */
public class TrackSectorNibbleDiskCheck implements DiskCheck {
    private final TrackSectorNibbleDevice device;

    public TrackSectorNibbleDiskCheck(TrackSectorNibbleDevice device) {
        this.device = device;
    }

    @Override
    public List<Finding> scan() {
        List<Finding> findings = new ArrayList<>();
        boolean canFix = device.can(Capability.WRITE_SECTOR) && device.can(Capability.FORMAT_TRACK);
        TrackSectorDevice.Geometry geometry = device.getGeometry();
        for (int track = 0; track < geometry.tracksOnDisk(); track++) {
            List<Integer> badSectors = new ArrayList<>();
            int trackSize = geometry.sectorsPerTrack() * SECTOR_SIZE;
            DataBuffer trackData = DataBuffer.create(trackSize);
            for (int sector = 0; sector < geometry.sectorsPerTrack(); sector++) {
                try {
                    DataBuffer sectorData = device.readSector(track, sector);
                    trackData.put(sector * SECTOR_SIZE, sectorData);
                } catch (Throwable t) {
                    badSectors.add(sector);
                }
            }
            if (!badSectors.isEmpty()) {
                final int problemTrack = track;
                Optional<Runnable> action = Optional.empty();
                if (canFix) {
                    action = Optional.of(() -> rewriteTrack(problemTrack, trackData));
                }
                List<Range> badSectorRanges = Range.from(badSectors);
                Finding finding = new Finding(
                        String.format("Unable to read sectors %s on track %d.", badSectorRanges, problemTrack),
                        action, "nibble", Coordinate.trackOnly(problemTrack));
                findings.add(finding);
            }
        }
        return findings;
    }

    private void rewriteTrack(int track, DataBuffer trackData) {
        device.formatTrack(track);
        for (int sector = 0; sector < device.getGeometry().sectorsPerTrack(); sector++) {
            device.writeSector(track, sector, trackData.slice(sector*SECTOR_SIZE, SECTOR_SIZE));
        }
    }
}
