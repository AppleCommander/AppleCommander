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

import org.applecommander.device.TrackSectorDevice;
import org.applecommander.exception.DirectoryFullException;
import org.applecommander.exception.DiskFullException;
import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.FileStoreOrganization;
import org.applecommander.filestore.FileStores;
import org.applecommander.os.usdos.*;
import org.applecommander.usage.SectorUsage;
import org.applecommander.usage.UsageType;
import org.applecommander.util.DataBuffer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class USDOSTest implements USDOSConstants {
    @Test
    public void testCreateAndWrite() {
        // Create a disk
        USDOSFileStore fileStore = FileStores.get(USDOSFileStoreFactory.class).create(FileStoreOrganization.DISK_16SECTOR_DO, Map.of());
        assertNotNull(fileStore);

        // Watch the sector bitmap
        SectorUsage usage = fileStore.get(SectorUsage.class).orElseThrow();
        assertEquals(16, usage.getUsed());

        // Get the root directory and verify there are no files
        USDOSDirectory directory = fileStore.getRootDirectory();
        assertNotNull(directory);
        assertEquals(0, directory.getFiles().size());

        // Create and verify the first file.
        final byte[] textContents = "THIS IS A THE FILE DATA.".getBytes();
        final String firstFileName = "FIRST.TXT";
        USDOSFileEntry firstFile = createFile(directory, firstFileName, textContents);
        verifyFile(firstFile, "FIRST", "TXT", 1, 0, textContents);
        assertEquals(18, usage.getUsed());
        assertEquals(UsageType.FILE, usage.getUsage(1, 0));
        assertEquals(UsageType.FILE, usage.getUsage(1, 1));

        // Create and verify a second file.
        final byte[] binaryContents = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        final String secondFileName = "SECOND.BIN";
        USDOSFileEntry secondFile = createFile(directory, secondFileName, binaryContents);
        verifyFile(secondFile, "SECOND", "BIN", 1, 2, binaryContents);
        assertEquals(20, usage.getUsed());
        assertEquals(UsageType.FILE, usage.getUsage(1, 2));
        assertEquals(UsageType.FILE, usage.getUsage(1, 3));

        // Read back the first file entry from the directory and then delete it.
        assertEquals(2, directory.getFiles().size());
        USDOSFileEntry firstFileEntry = (USDOSFileEntry) directory.getFiles().getFirst();
        assertNotNull(firstFileEntry);
        directory.deleteFile(firstFileEntry);
        assertEquals(2, directory.getFiles().size());
        assertEquals(18, usage.getUsed());
        assertEquals(UsageType.FREE, usage.getUsage(1, 0));
        assertEquals(UsageType.FREE, usage.getUsage(1, 1));

        // Finally, create a 3rd file and expect it to be placed where the FIRST file was.
        // Note that since we're re-using the deleted file, we start with T1,S1 to use (the data sector).
        // That data sector becomes the index sector and T1,S0 becomes the data sector.
        final byte[] thirdFile = "THIS IS THE THIRD FILE.".getBytes();
        final String thirdFileName = "THIRD.TXT";
        USDOSFileEntry thirdFileEntry = createFile(directory, thirdFileName, thirdFile);
        verifyFile(thirdFileEntry, "THIRD", "TXT", 1, 1, thirdFile);
        assertEquals(20, usage.getUsed());
        assertEquals(UsageType.FILE, usage.getUsage(1, 0));
        assertEquals(UsageType.FILE, usage.getUsage(1, 1));
    }

    public USDOSFileEntry createFile(USDOSDirectory directory, String fileName, byte[] dataFork) {
        USDOSFileEntry fileEntry = directory.createFile(fileName);
        assertNotNull(fileEntry);
        fileEntry.setDataFork(DataBuffer.wrap(dataFork));
        return fileEntry;
    }

    public void verifyFile(USDOSFileEntry fileEntry, String expectedBaseName, String expectedFileExtension,
                           int expectedIndexTrack, int expectedIndexSector, byte[] expectedDataFork) {
        assertEquals(expectedBaseName, fileEntry.getBaseName());
        assertEquals(expectedFileExtension, fileEntry.getExtension());
        assertEquals(expectedIndexTrack, fileEntry.getTrackSectorIndex().track());
        assertEquals(expectedIndexSector, fileEntry.getTrackSectorIndex().sector());
        assertArrayEquals(expectedDataFork,
                fileEntry.getDataFork().getFixedLengthString(0, expectedDataFork.length).getBytes());
    }

    @Test
    public void testDirectories() {
        // Create a disk
        USDOSFileStore fileStore = FileStores.get(USDOSFileStoreFactory.class).create(FileStoreOrganization.DISK_16SECTOR_DO, Map.of());
        assertNotNull(fileStore);

        // Get the root directory and verify there are no files
        USDOSDirectory rootDirectory = fileStore.getRootDirectory();
        assertNotNull(rootDirectory);

        final String filename = "README.TXT";
        createFile(rootDirectory, filename, "ROOT DIRECTORY".getBytes());

        // Create a bunch of directories and files in those directories
        for (String dirName : List.of("DIR-A", "DIR-B", "DIR-C")) {
            USDOSDirectory subdir = rootDirectory.createDirectory(dirName);
            createFile(subdir, filename, dirName.getBytes());
        }

        // Now build list
        final List<String> expected = List.of(
            "README.TXT",
            "DIR-A/", "DIR-A/README.TXT",
            "DIR-B/", "DIR-B/README.TXT",
            "DIR-C/", "DIR-C/README.TXT"
        );
        List<String> actual = new ArrayList<>();
        List<USDOSDirectory> directories = new ArrayList<>();
        directories.add(rootDirectory);
        while (!directories.isEmpty()) {
            USDOSDirectory dir = directories.removeFirst();
            String currentDirName = "";
            if (dir.getParent().isPresent()) {
                currentDirName = dir.getName() + "/";
                actual.add(currentDirName);
            }
            for (FileEntry entry : dir.getFiles()) {
                Optional<USDOSDirectory> subdir = entry.get(USDOSDirectory.class);
                if (subdir.isPresent()) {
                    directories.add(subdir.get());
                }
                else {
                    actual.add(currentDirName + entry.getName());
                }
            }
        }
        assertEquals(expected, actual);
    }

    @Test
    public void fillDirectoryEntries() {
        // Create a disk
        USDOSFileStore fileStore = FileStores.get(USDOSFileStoreFactory.class).create(FileStoreOrganization.DISK_16SECTOR_DO, Map.of());
        assertNotNull(fileStore);

        // Get the root directory
        USDOSDirectory directory = fileStore.getRootDirectory();
        assertNotNull(directory);

        // Generate small files until we've filled the directory. This should not generate an error.
        // Note that we subtract 1 because we have the "number of files" but the volume entry is also taking
        // up space.
        final byte[] textContents = "THIS IS A THE FILE DATA.".getBytes();
        int fileNumber = 1;
        while (directory.getFiles().size() < DIRECTORY_MAX_ENTRIES) {
            String fileName = String.format("FILE%d.TXT", fileNumber++);
            createFile(directory, fileName, textContents);
        }
        assertEquals(DIRECTORY_MAX_ENTRIES, directory.getFiles().size());

        // Generate one more file, which should cause an exception
        assertThrows(DirectoryFullException.class, () -> {
            createFile(directory, "FAILED.TXT", textContents);
        });

        // And be certain we don't (accidentally) increment that number.
        assertEquals(DIRECTORY_MAX_ENTRIES, directory.getFiles().size());
    }

    @Test
    public void fillDisk() {
        // Create a disk
        USDOSFileStore fileStore = FileStores.get(USDOSFileStoreFactory.class).create(FileStoreOrganization.DISK_16SECTOR_DO, Map.of());
        assertNotNull(fileStore);

        // Get the root directory
        USDOSDirectory directory = fileStore.getRootDirectory();
        assertNotNull(directory);

        // Get some useful stuff
        TrackSectorDevice device = fileStore.get(TrackSectorDevice.class).orElseThrow();
        SectorUsage usage = fileStore.get(SectorUsage.class).orElseThrow();

        // Generate a bunch of large files until we fill the disk
        final byte[] bigfile = new byte[16000];
        final int expectedSectors = device.calculateRequiredSectors(bigfile.length) + 1;

        // Proceed to fill the disk
        int fileNumber = 1;
        while (usage.getFree() >= expectedSectors) {
            String fileName = String.format("FILE%d.TXT", fileNumber++);
            createFile(directory, fileName, bigfile);
        }

        // Try to write a file that won't fit
        assertThrows(DiskFullException.class, () -> {
            createFile(directory, "FAILED.TXT", bigfile);
        });
    }
}
