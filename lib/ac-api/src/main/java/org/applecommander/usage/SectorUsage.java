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
package org.applecommander.usage;

import org.applecommander.device.TrackSectorDevice;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

/// Provides sector level usage information. Sectors are always the same size in all contexts,
/// so it is hard-coded.
public class SectorUsage extends DiskUsage {
    private final BiFunction<Integer,Integer,UsageType> usageFn;
    private final TrackSectorDevice.Geometry geometry;

    public SectorUsage(TrackSectorDevice.Geometry geometry,
                       BiFunction<Integer,Integer,UsageType> usageFn) {
        super(TrackSectorDevice.SECTOR_SIZE);
        Objects.requireNonNull(geometry);
        Objects.requireNonNull(usageFn);
        this.usageFn = usageFn;
        this.geometry = geometry;
    }

    @Override
    public Map<UsageType, Integer> getUsageCounts() {
        Map<UsageType,Integer> counts = new HashMap<>();
        for (int t=0; t<getTracksOnDisk(); t++) {
            for (int s=0; s<getSectorsPerTrack(); s++) {
                counts.compute(getUsage(t,s), (u, v) -> v == null ? 1 : v + 1);
            }
        }
        return counts;
    }
    public UsageType getUsage(int track, int sector) {
        return usageFn.apply(track, sector);
    }
    @Override
    public int getTotal() {
        return geometry.sectorsPerDisk();
    }
    public int getTracksOnDisk() {
        return geometry.tracksOnDisk();
    }
    public int getSectorsPerTrack() {
        return geometry.sectorsPerTrack();
    }
}
