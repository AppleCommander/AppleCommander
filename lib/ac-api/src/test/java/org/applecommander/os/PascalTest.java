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
import org.applecommander.exception.DirectoryFullException;
import org.applecommander.exception.DiskFullException;
import org.applecommander.filestore.FileStoreOrganization;
import org.applecommander.filestore.FileStores;
import org.applecommander.os.pascal.*;
import org.applecommander.usage.BlockUsage;
import org.applecommander.util.DataBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PascalTest implements PascalConstants {
    @Test
    public void testCreateAndWrite() {
        // Create a disk
        PascalFileStore fileStore = FileStores.get(PascalFileStoreFactory.class).create(FileStoreOrganization.DISK_16SECTOR_PO);
        assertNotNull(fileStore);

        // Get the root directory and verify there are no files
        PascalDirectory directory = fileStore.getRootDirectory();
        assertNotNull(directory);
        assertEquals(0, directory.getFiles().size());

        // Create and verify the first file.
        final byte[] textContents = "THIS IS A THE FILE DATA.".getBytes();
        final String firstFileName = "FIRST-FILE";
        PascalFileEntry firstFile = createFile(directory, firstFileName, "TEXT", textContents);
        verifyFile(firstFile, firstFileName, "TEXT", DIRECTORY_LAST_BLOCK+1, 1, textContents);
        assertEquals(1, directory.getFiles().size());

        // Create and verify a second file.
        final byte[] binaryContents = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        final String secondFileName = "SECOND-FILE";
        PascalFileEntry secondFile = createFile(directory, secondFileName, "DATA", binaryContents);
        verifyFile(secondFile, secondFileName, "DATA", DIRECTORY_LAST_BLOCK+2, 1, binaryContents);
        assertEquals(2, directory.getFiles().size());

        // Read back the first file entry from the directory and then delete it.
        PascalFileEntry firstFileEntry = (PascalFileEntry) directory.findFile(firstFileName).orElseThrow();
        assertNotNull(firstFileEntry);
        directory.deleteFile(firstFileEntry);
        assertEquals(1, directory.getFiles().size());

        // Finally, create a 3rd file and expect it to be placed where the FIRST file was.
        final byte[] thirdFile = "THIS IS THE THIRD FILE.".getBytes();
        final String thirdFileName = "THIRD-FILE";
        PascalFileEntry thirdFileEntry = createFile(directory, thirdFileName, "TEXT", thirdFile);
        verifyFile(thirdFileEntry, thirdFileName, "TEXT", DIRECTORY_LAST_BLOCK+1, 1, thirdFile);
        assertEquals(2, directory.getFiles().size());
    }

    public PascalFileEntry createFile(PascalDirectory directory, String fileName, String fileType, byte[] dataFork) {
        PascalFileEntry fileEntry = directory.createFile(fileName);
        assertNotNull(fileEntry);
        fileEntry.setType(fileType);
        fileEntry.setDataFork(DataBuffer.wrap(dataFork));
        return fileEntry;
    }

    public void verifyFile(PascalFileEntry fileEntry, String expectedFileName, String expectedFileType,
                           int expectedFirstBlock, int expectedBlockCount, byte[] expectedDataFork) {
        assertEquals(expectedFileName, fileEntry.getName());
        assertEquals(expectedFileType, fileEntry.getType());
        assertEquals(expectedFirstBlock, fileEntry.getFirstBlock());
        assertEquals(expectedBlockCount, fileEntry.getBlocksUsed());
        assertArrayEquals(expectedDataFork, fileEntry.getDataFork().getFixedLengthString(0, expectedDataFork.length).getBytes());
    }

    @Test
    public void fillDirectoryEntries() {
        // Create a disk
        PascalFileStore fileStore = FileStores.get(PascalFileStoreFactory.class).create(FileStoreOrganization.DISK_16SECTOR_PO);
        assertNotNull(fileStore);

        // Get the root directory
        PascalDirectory directory = fileStore.getRootDirectory();
        assertNotNull(directory);

        // Generate small files until we've filled the directory. This should not generate an error.
        // Note that we subtract 1 because we have the "number of files" but the volume entry is also taking
        // up space.
        final byte[] textContents = "THIS IS A THE FILE DATA.".getBytes();
        int fileNumber = 1;
        while (directory.getFiles().size() < MAX_DIRECTORY_ENTRIES-1) {
            String fileName = String.format("FILE%d", fileNumber++);
            createFile(directory, fileName, "TEXT", textContents);
        }
        assertEquals(MAX_DIRECTORY_ENTRIES-1, directory.getFiles().size());

        // Generate one more file, which should cause an exception
        assertThrows(DirectoryFullException.class, () -> {
            createFile(directory, "FAILED-FILE", "TEXT", textContents);
        });

        // And be certain we don't (accidentally) increment that number.
        assertEquals(MAX_DIRECTORY_ENTRIES-1, directory.getFiles().size());
    }

    @Test
    public void fillDisk() {
        // Create a disk
        PascalFileStore fileStore = FileStores.get(PascalFileStoreFactory.class).create(FileStoreOrganization.DISK_16SECTOR_PO);
        assertNotNull(fileStore);

        // Get the root directory
        PascalDirectory directory = fileStore.getRootDirectory();
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
