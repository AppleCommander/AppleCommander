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
package com.webcodepro.applecommander.storage;

import com.webcodepro.applecommander.storage.physical.ImageOrder;
import org.applecommander.capability.Capability;
import org.applecommander.device.BlockDevice;
import org.applecommander.hint.Hint;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;

import java.util.Optional;

/**
 * Temporary transition layer between ImageOrder and BlockDevice.
 */
public class BlockDeviceAdapter implements BlockDevice {
    public static BlockDevice from(FormattedDisk disk) {
        if (disk instanceof Container c) {
            Optional<BlockDevice> opt = c.get(BlockDevice.class);
            if (opt.isPresent()) return opt.get();
        }
        if (disk instanceof FormattedDiskX x) {
            return new BlockDeviceAdapter(x.getImageOrder());
        }
        throw new RuntimeException("No BlockDevice present: " + disk.getClass().getName());
    }

    private ImageOrder order;

    private BlockDeviceAdapter(ImageOrder order) {
        this.order = order;
    }

    @Override
    public boolean is(Hint hint) {
        return order.is(hint);
    }

    @Override
    public boolean can(Capability capability) {
        // TODO
        return false;
    }

    @Override
    public Geometry getGeometry() {
        return new Geometry(STANDARD_BLOCK_SIZE, order.getBlocksOnDevice());
    }

    @Override
    public DataBuffer readBlock(int block) {
        return DataBuffer.wrap(order.readBlock(block));
    }

    @Override
    public void writeBlock(int block, DataBuffer blockData) {
        order.writeBlock(block, blockData.asBytes());
    }
}
