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

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.IntSupplier;

/// Provides sector level usage information. Sectors are always the same size in all contexts,
/// so it is hard-coded.
public class SectorUsage extends DiskUsage {
    private final BiFunction<Integer,Integer,Boolean> usedFn;
    private final int totalTracks;
    private final int totalSectors;

    public SectorUsage(IntSupplier used, IntSupplier free, BiFunction<Integer,Integer,Boolean> usedFn,
                       int totalTracks, int totalSectors) {
        super(TrackSectorDevice.SECTOR_SIZE, used, free);
        Objects.requireNonNull(usedFn);
        this.usedFn = usedFn;
        this.totalTracks = totalTracks;
        this.totalSectors = totalSectors;
    }

    public boolean isUsed(int track, int sector) {
        return usedFn.apply(track, sector);
    }
    public int getTotalTracks() {
        return totalTracks;
    }
    public int getTotalSectors() {
        return totalSectors;
    }
}
