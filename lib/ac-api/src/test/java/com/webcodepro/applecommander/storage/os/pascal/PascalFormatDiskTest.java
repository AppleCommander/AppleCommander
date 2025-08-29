/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2022 by Robert Greene and others
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
package com.webcodepro.applecommander.storage.os.pascal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.applecommander.device.BlockDevice;
import org.applecommander.device.ProdosOrderedBlockDevice;
import org.applecommander.source.DataBufferSource;
import org.applecommander.source.Source;

import com.webcodepro.applecommander.storage.DiskConstants;
import com.webcodepro.applecommander.storage.DiskFullException;
import org.junit.jupiter.api.Test;

public class PascalFormatDiskTest {
    @Test
    public void testSanitizeFilename() throws DiskFullException {
        Source source = DataBufferSource.create(DiskConstants.APPLE_140KB_DISK, "new-disk").get();
        BlockDevice blockDevice = new ProdosOrderedBlockDevice(source, BlockDevice.STANDARD_BLOCK_SIZE);
        PascalFormatDisk[] disks = PascalFormatDisk.create("deleteme.po", "TEST", blockDevice);
        PascalFormatDisk disk = disks[0];
        
        assertEquals("FILENAME", disk.getSuggestedFilename("FileName"));
        assertEquals("2021", disk.getSuggestedFilename("2021"));
        assertEquals("..", disk.getSuggestedFilename(".."));
        assertEquals("THEFILENAME", disk.getSuggestedFilename("The File Name"));
        assertEquals("HIDDENTAB", disk.getSuggestedFilename("\t hidden tab"));
    }
}
