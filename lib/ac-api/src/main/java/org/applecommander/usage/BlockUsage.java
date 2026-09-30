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

import org.applecommander.device.BlockDevice;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/// Provides block-level usage information. Note that block size can vary from 256 bytes (RDOS) to
/// 512 bytes (Pascal, ProDOS) to 1024 bytes (CP/M), so it must be passed in on the constructor.
public class BlockUsage extends DiskUsage {
    private final BlockDevice.Geometry geometry;
    private final Function<Integer,UsageType> usageFn;

    public BlockUsage(BlockDevice.Geometry geometry, Function<Integer,UsageType> usageFn) {
        super(geometry.blockSize());
        Objects.requireNonNull(geometry);
        Objects.requireNonNull(usageFn);
        this.geometry = geometry;
        this.usageFn = usageFn;
    }

    @Override
    public Map<UsageType, Integer> getUsageCounts() {
        Map<UsageType,Integer> counts = new HashMap<>();
        for (int b=0; b<getTotal(); b++) {
            counts.compute(getUsage(b), (u, v) -> v == null ? 1 : v + 1);
        }
        return counts;
    }

    @Override
    public int getTotal() {
        return geometry.blocksOnDevice();
    }

    public UsageType getUsage(int block) {
        return usageFn.apply(block);
    }
}
