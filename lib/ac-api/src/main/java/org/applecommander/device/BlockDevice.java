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

/**
 * Represents block devices in the Apple II world. These devices should be
 * capable of supporting ProDOS and Pascal 512 byte blocks, as well as RDOS
 * 256 byte blocks, and even CP/M 1024 byte blocks.  Those statistics should
 * be supported by the given Geometry.
 */
public interface BlockDevice extends Device {
    int STANDARD_BLOCK_SIZE = 512;

    Geometry getGeometry();
    DataBuffer readBlock(int block);
    void writeBlock(int block, DataBuffer blockData);
    /**
     * Format a disk. For most disks, this is simply a wipe to all zeros. If this
     * disk has extended format (such as nibble formats), this is the opportunity
     * to write out that format.
     */
    default void format() {
        DataBuffer blockData = DataBuffer.create(getGeometry().blockSize());
        for (int block = 0; block < getGeometry().blocksOnDevice(); block++) {
            writeBlock(block, blockData);
        }
    }

    record Geometry(int blockSize, int blocksOnDevice) {}
}
