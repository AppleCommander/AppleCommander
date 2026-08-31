/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2026 by Robert Greene and others
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

import com.webcodepro.applecommander.storage.DiskConstants;
import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.FileEntry;
import org.applecommander.device.BlockDevice;
import org.applecommander.device.ProdosOrderedBlockDevice;
import org.applecommander.os.DiskOptimizer;
import org.applecommander.source.DataBufferSource;
import org.applecommander.source.Source;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class PascalDiskOptimizerTest {
    @Test
    public void testKrunch() throws DiskFullException {
        Source source = DataBufferSource.create(DiskConstants.APPLE_140KB_DISK, "new-disk").get();
        BlockDevice blockDevice = new ProdosOrderedBlockDevice(source, BlockDevice.STANDARD_BLOCK_SIZE);
        PascalFormatDisk[] disks = PascalFormatDisk.create("deleteme.po", "TEST", blockDevice);
        PascalFormatDisk disk = disks[0];

        // Strategy:
        // 1. Create a set of files. At least one single block file.
        // 2. Delete the first file (opening up a gap).
        // 3. "Krunch"
        // 4. Check expected block numbers.

        PascalFileEntry file0 = createFileData(disk, "file0", 2000);    // 4 blocks (gets deleted)
        PascalFileEntry file1 = createFileData(disk, "file1", 2048);    // 4 blocks
        PascalFileEntry file2 = createFileData(disk, "file2", 4000);    // 8 blocks
        PascalFileEntry file3 = createFileData(disk, "file3", 500);     // 1 block
        PascalFileEntry file4 = createFileData(disk, "file4", 2000);    // 4 blocks

        // Need to capture the random data BEFORE optimization occurs
        final byte[][] expectedFileData = { file1.getFileData(), file2.getFileData(), file3.getFileData(), file4.getFileData() };

        DiskOptimizer optimizer = disk.get(DiskOptimizer.class).orElseThrow();
        assertFalse(optimizer.canOptimizeDisk());

        file0.delete();
        assertTrue(optimizer.canOptimizeDisk());

        optimizer.optimizeDisk();

        // Some of the file data is cached, so we do a fresh pull from the image
        final int[] expectedFirstBlock = { 6, 10, 18, 19 };
        final int[] expectedLastBlock = { 10, 18, 19, 23 };
        List<FileEntry> fileEntries = disk.getFiles();
        assertEquals(4, fileEntries.size());
        for (int i=0; i<fileEntries.size(); i++) {
            PascalFileEntry fileEntry = (PascalFileEntry) fileEntries.get(i);
            assertEquals(expectedFirstBlock[i], fileEntry.getFirstBlock());
            assertEquals(expectedLastBlock[i], fileEntry.getLastBlock());
            assertArrayEquals(expectedFileData[i], fileEntry.getFileData());
        }
    }

    public PascalFileEntry createFileData(PascalFormatDisk disk, String filename, int size) throws DiskFullException {
        byte[] fileData = new byte[size];
        new Random().nextBytes(fileData);

        PascalFileEntry fileEntry = disk.createFile();
        fileEntry.setFilename(filename);
        fileEntry.setFiletype("data");
        fileEntry.setFileData(fileData);
        return fileEntry;
    }
}
