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
package org.applecommander.os;

import org.applecommander.device.BlockDevice;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.exception.DirectoryFullException;
import org.applecommander.exception.DiskFullException;
import org.applecommander.filestore.FileStoreOrganization;
import org.applecommander.filestore.FileStores;
import org.applecommander.os.rdos.*;
import org.applecommander.usage.BlockUsage;
import org.applecommander.util.DataBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RDOSTest implements RDOSConstants {
    @Test
    public void testCreateAndWrite() {
        // Create a disk
        RDOSFileStore fileStore = FileStores.get(RDOSFileStoreFactory.class).create(FileStoreOrganization.DISK_16SECTOR_DO);
        assertNotNull(fileStore);

        // Get the root directory and verify there are no files
        RDOSDirectory directory = fileStore.getRootDirectory();
        assertNotNull(directory);
        assertEquals(2, directory.getFiles().size());

        // Verify SYSTEMBOOT exists
        RDOSFileEntry systemboot = (RDOSFileEntry) directory.findFile("SYSTEMBOOT").orElseThrow();
        assertNotNull(systemboot);
        assertEquals("T", systemboot.getType());
        DataBuffer systembootData = systemboot.getDataFork();
        final String contentPrefix = "PR#0:IN#0:";
        assertEquals(contentPrefix, systembootData.getFixedLengthString(0, contentPrefix.length()));

        // Create and verify the first file.
        final byte[] textContents = "THIS IS A THE FILE DATA.".getBytes();
        final String firstFileName = "FIRST-FILE";
        RDOSFileEntry firstFile = createFile(directory, firstFileName, "T", textContents);
        verifyFile(firstFile, firstFileName, "T", 33, 1, textContents);

        // Create and verify a second file.
        final byte[] binaryContents = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        final String secondFileName = "SECOND-FILE";
        RDOSFileEntry secondFile = createFile(directory, secondFileName, "B", binaryContents);
        verifyFile(secondFile, secondFileName, "B", 34, 1, binaryContents);

        // Read back the first file entry from the directory and then delete it.
        RDOSFileEntry firstFileEntry = (RDOSFileEntry) directory.findFile(firstFileName).orElseThrow();
        assertNotNull(firstFileEntry);
        directory.deleteFile(firstFileEntry);

        // Finally, create a 3rd file and expect it to be placed where the FIRST file was.
        final byte[] thirdFile = "THIS IS THE THIRD FILE.".getBytes();
        final String thirdFileName = "THIRD-FILE";
        RDOSFileEntry thirdFileEntry = createFile(directory, thirdFileName, "T", thirdFile);
        verifyFile(thirdFileEntry, thirdFileName, "T", 33, 1, thirdFile);
        assertEquals(4, directory.getFiles().size());
    }

    public RDOSFileEntry createFile(RDOSDirectory directory, String fileName, String fileType, byte[] dataFork) {
        RDOSFileEntry fileEntry = directory.createFile(fileName);
        assertNotNull(fileEntry);
        fileEntry.setType(fileType);
        fileEntry.setDataFork(DataBuffer.wrap(dataFork));
        return fileEntry;
    }

    public void verifyFile(RDOSFileEntry fileEntry, String expectedFileName, String expectedFileType,
                           int expectedFirstBlock, int expectedBlockCount, byte[] expectedDataFork) {
        assertEquals(expectedFileName, fileEntry.getName());
        assertEquals(expectedFileType, fileEntry.getType());
        assertEquals(expectedFirstBlock, fileEntry.getFirstBlock());
        assertEquals(expectedBlockCount, fileEntry.getBlocks());
        assertArrayEquals(expectedDataFork, fileEntry.getDataFork().getFixedLengthString(0, expectedDataFork.length).getBytes());
    }

    @Test
    public void fillDirectoryEntries() {
        // Create a disk
        RDOSFileStore fileStore = FileStores.get(RDOSFileStoreFactory.class).create(FileStoreOrganization.DISK_16SECTOR_DO);
        assertNotNull(fileStore);

        // Get the root directory
        RDOSDirectory directory = fileStore.getRootDirectory();
        assertNotNull(directory);

        // Generate small files until we've filled the directory. This should not generate an error.
        // Note that we subtract 1 because we have the "number of files" but the volume entry is also taking
        // up space.
        final int maxDirectoryEntries = (RDOSVersionAdapter.RDOS33.lastDirectoryBlock - RDOSVersionAdapter.RDOS33.firstDirectoryBlock + 1)
                * TrackSectorDevice.SECTOR_SIZE / ENTRY_LENGTH;
        final byte[] textContents = "THIS IS A THE FILE DATA.".getBytes();
        int fileNumber = 1;
        while (directory.getFiles().size() < maxDirectoryEntries) {
            String fileName = String.format("FILE%d", fileNumber++);
            createFile(directory, fileName, "TEXT", textContents);
        }
        assertEquals(maxDirectoryEntries, directory.getFiles().size());

        // Generate one more file, which should cause an exception
        assertThrows(DirectoryFullException.class, () -> {
            createFile(directory, "FAILED-FILE", "TEXT", textContents);
        });

        // And be certain we don't (accidentally) increment that number.
        assertEquals(maxDirectoryEntries, directory.getFiles().size());
    }

    @Test
    public void fillDisk() {
        // Create a disk
        RDOSFileStore fileStore = FileStores.get(RDOSFileStoreFactory.class).create(FileStoreOrganization.DISK_16SECTOR_DO);
        assertNotNull(fileStore);

        // Get the root directory
        RDOSDirectory directory = fileStore.getRootDirectory();
        assertNotNull(directory);

        // Get some useful stuff
        BlockDevice device = fileStore.get(BlockDevice.class).orElseThrow();
        BlockUsage usage = fileStore.get(BlockUsage.class).orElseThrow();

        // Generate a bunch of large files until we fill the disk
        final byte[] bigfile = new byte[16000];
        final int expectedBlocks = device.calculateRequiredBlocks(bigfile.length);

        // Proceed to fill the disk
        int fileNumber = 1;
        while (usage.getFree() >= expectedBlocks) {
            String fileName = String.format("FILE%d", fileNumber++);
            createFile(directory, fileName, "DATA", bigfile);
        }

        // Try to write a file that won't fit
        assertThrows(DiskFullException.class, () -> {
            createFile(directory, "FAILED-FILE", "DATA", bigfile);
        });
    }
}
