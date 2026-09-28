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

import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntSupplier;

/// Provides block-level usage information. Note that block size can vary from 256 bytes (RDOS) to
/// 512 bytes (Pascal, ProDOS) to 1024 bytes (CP/M), so it must be passed in on the constructor.
public class BlockUsage extends DiskUsage {
    private final Function<Integer,Boolean> usedFn;

    public BlockUsage(int blockSize, IntSupplier used, IntSupplier free, Function<Integer,Boolean> usedFn) {
        super(blockSize, used, free);
        Objects.requireNonNull(usedFn);
        this.usedFn = usedFn;
    }

    public boolean isUsed(int block) {
        return usedFn.apply(block);
    }
}
