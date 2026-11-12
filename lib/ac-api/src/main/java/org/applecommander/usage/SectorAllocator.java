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

import org.applecommander.device.Coordinate;
import org.applecommander.device.Coordinate.TrackAndSectorCoordinate;
import org.applecommander.device.TrackSectorDevice.Geometry;
import org.applecommander.exception.DiskFullException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// The `SectorAllocator` is a general usage sector allocation strategy.
/// This differs from `DiskUsage` in that when allocating sectors, we only care if a sector is free or used,
/// and it allows write operations.
/// The current strategy is to re-use existing sectors (when given) and then look for free sectors
/// by starting at track 0, sector 0.
/// There is a mix of two integers to represent track and sector or a `TrackAndSectorCoordinate`. Depending
/// on usage, either is applicable.
public class SectorAllocator {
    private final SectorBitmapReader reader;
    private final SectorBitmapWriter writer;
    private final Geometry geometry;

    public SectorAllocator(SectorBitmapReader reader, SectorBitmapWriter writer, Geometry geometry) {
        Objects.requireNonNull(reader);
        Objects.requireNonNull(writer);
        Objects.requireNonNull(geometry);
        this.reader = reader;
        this.writer = writer;
        this.geometry = geometry;
    }

    /// Indicates if this sector is free.
    public boolean isFree(int track, int sector) {
        return reader.getUsage(track, sector) == UsageType.FREE;
    }
    /// Indicates if this sector is used.
    public boolean isUsed(int track, int sector) {
        return !isFree(track, sector);
    }
    /// Indicates if this sector is free.
    public boolean isFree(TrackAndSectorCoordinate coordinate) {
        return isFree(coordinate.track(), coordinate.sector());
    }
    /// Indicates if this sector is used.
    public boolean isUsed(TrackAndSectorCoordinate coordinate) {
        return isUsed(coordinate.track(), coordinate.sector());
    }

    /// Mark sector as used.
    public void markUsed(int track, int sector) {
        writer.setUsed(track, sector, true);
    }
    /// Mark sector se free.
    public void markFree(int track, int sector) {
        writer.setUsed(track, sector, false);
    }
    /// Mark sector as used.
    public void markUsed(TrackAndSectorCoordinate coordinate) {
        markUsed(coordinate.track(), coordinate.sector());
    }
    /// Mark sector se free.
    public void markFree(TrackAndSectorCoordinate coordinate) {
        markFree(coordinate.track(), coordinate.sector());
    }

    /// Allocate the given number of sectors. Only marks them used if all the sectors can be allocated.
    public List<TrackAndSectorCoordinate> allocateSectors(int sectors) {
        assert sectors > 0;
        List<TrackAndSectorCoordinate> allocatedSectors = new ArrayList<>();
        for (int t=0; t<geometry.tracksOnDisk(); t++) {
            for (int s=0; s<geometry.sectorsPerTrack(); s++) {
                if (isFree(t,s)) {
                    allocatedSectors.add(Coordinate.trackAndSector(t,s));
                    if (allocatedSectors.size() == sectors) {
                        allocatedSectors.forEach(coordinate -> {
                            writer.setUsed(coordinate, true);
                        });
                        return allocatedSectors;
                    }
                }
            }
        }
        throw new DiskFullException("unable to find %d free sectors in bitmap", sectors);
    }

    /// Allocate the given number of sectors by reusing the current set of sectors. Only marks sectors used
    /// (or free if current sectors are not used entirely) if all sectors are allocated.
    public List<TrackAndSectorCoordinate> allocateSectors(int sectors, List<TrackAndSectorCoordinate> currentSectors) {
        List<TrackAndSectorCoordinate> allocatedSectors = new ArrayList<>();
        // Allocate from existing sectors first.
        while (sectors > 0 && !currentSectors.isEmpty()) {
            allocatedSectors.add(currentSectors.removeFirst());
            sectors--;
        }
        // Allocate any remaining sectors last.
        if (sectors > 0) {
            allocatedSectors.addAll(allocateSectors(sectors));
        }
        // Mark all allocated sectors as used.
        allocatedSectors.forEach(this::markUsed);
        // Mark any leftover sectors as free.
        currentSectors.forEach(this::markFree);
        return allocatedSectors;
    }
}
