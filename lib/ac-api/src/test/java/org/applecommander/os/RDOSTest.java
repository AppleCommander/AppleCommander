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

import org.applecommander.filestore.FileStoreOrganization;
import org.applecommander.filestore.FileStores;
import org.applecommander.os.rdos.RDOSDirectory;
import org.applecommander.os.rdos.RDOSFileEntry;
import org.applecommander.os.rdos.RDOSFileStore;
import org.applecommander.os.rdos.RDOSFileStoreFactory;
import org.applecommander.util.DataBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RDOSTest {
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

}
