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

import com.webcodepro.applecommander.storage.DiskConstants;
import org.applecommander.device.DosOrderedTrackSectorDevice;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.FileStore;
import org.applecommander.filestore.FileStoreFactory;
import org.applecommander.filestore.FileStores;
import org.applecommander.os.gamedos.GamedosDirectory;
import org.applecommander.os.gamedos.GamedosFileEntry;
import org.applecommander.os.gamedos.GamedosFileStore;
import org.applecommander.os.gamedos.GamedosFileStoreFactory;
import org.applecommander.source.DataBufferSource;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;
import org.applecommander.util.DataBuffer;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class GamedosTest {
    @Test
    public void testGamedosExample() {
        final String fullPath = "src/test/resources/disks/gamedos-example.dsk";
        Source source = Sources.create(fullPath).orElseThrow();
        FileStoreFactory.Context ctx = FileStores.inspect(source);
        assertFalse(ctx.fileStores.isEmpty());
        assertEquals(1, ctx.fileStores.size());
        FileStore fileStore = ctx.fileStores.getFirst();
        if (fileStore instanceof GamedosFileStore gamedos) {
            assertEquals("GAMEDOS V0.8", gamedos.getLabel());
            GamedosDirectory directory = gamedos.getRootDirectory();
            assertEquals(1, directory.getFiles().size());
            FileEntry fileEntry = directory.getFiles().getFirst();
            if (fileEntry instanceof GamedosFileEntry file) {
                assertEquals("T", file.getFiletype());
                assertEquals("Test Text File", file.getName());
                assertEquals(1, file.getFirstTrack());
                assertEquals(0, file.getFirstSector());
                assertEquals(2, file.getSectorCount());
                DataBuffer data = file.getDataFork();
                assertNotNull(data);
                Optional<Integer> found = data.scan("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".getBytes(StandardCharsets.UTF_8), 0);
                assertTrue(found.isPresent());
            }
            else {
                fail ("Expecting a GamedosFileEntry!");
            }
        }
        else {
            fail("Expecting a GamedosFileStore!");
        }
    }

    @Test
    public void testCreateAndWrite() {
        // Create a disk
        Source source = DataBufferSource.create(DiskConstants.APPLE_140KB_DISK, "GAMEDOS-TEST").get();
        TrackSectorDevice device = new DosOrderedTrackSectorDevice(source);
        GamedosFileStore fileStore = GamedosFileStoreFactory.create(device);
        assertNotNull(fileStore);

        // Get the root directory and verify there are no files
        GamedosDirectory directory = fileStore.getRootDirectory();
        assertNotNull(directory);
        assertEquals(0, directory.getFiles().size());

        // Create and verify the first file.
        final byte[] textContents = "THIS IS A THE FILE DATA.".getBytes();
        final String firstFileName = "This is the FIRST file.";
        GamedosFileEntry firstFile = createFile(directory, firstFileName, "T", textContents);
        verifyFile(firstFile, firstFileName, "T", 1, 0, 1, textContents);

        // Create and verify a second file.
        final byte[] binaryContents = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        final String secondFileName = "This is the SECOND file.";
        GamedosFileEntry secondFile = createFile(directory, secondFileName, "B", binaryContents);
        verifyFile(secondFile, secondFileName, "B", 1, 1, 1, binaryContents);

        // Read back the first file entry from the directory and then delete it.
        assertEquals(2, directory.getFiles().size());
        GamedosFileEntry firstFileEntry = (GamedosFileEntry) directory.getFiles().getFirst();
        assertNotNull(firstFileEntry);
        directory.deleteFile(firstFileEntry);
        assertEquals(1, directory.getFiles().size());

        // Finally, create a 3rd file and expect it to be placed where the FIRST file was.
        final byte[] thirdFile = "THIS IS THE THIRD FILE.".getBytes();
        final String thirdFileName = "This is the THIRD file.";
        GamedosFileEntry thirdFileEntry = createFile(directory, thirdFileName, "T", thirdFile);
        verifyFile(thirdFileEntry, thirdFileName, "T", 1, 0, 1, thirdFile);
    }

    public GamedosFileEntry createFile(GamedosDirectory directory, String fileName, String fileType, byte[] dataFork) {
        GamedosFileEntry fileEntry = (GamedosFileEntry) directory.createFile(fileName);
        assertNotNull(fileEntry);
        fileEntry.setFiletype(fileType);
        fileEntry.setDataFork(DataBuffer.wrap(dataFork));
        return fileEntry;
    }

    public void verifyFile(GamedosFileEntry fileEntry, String expectedFileName, String expectedFileType,
                           int expectedFirstTrack, int expectedFirstSector, int expectedSectorCount, byte[] expectedDataFork) {
        assertEquals(expectedFileName, fileEntry.getName());
        assertEquals(expectedFileType, fileEntry.getFiletype());
        assertEquals(expectedFirstTrack, fileEntry.getFirstTrack());
        assertEquals(expectedFirstSector, fileEntry.getFirstSector());
        assertEquals(expectedSectorCount, fileEntry.getSectorCount());
        assertArrayEquals(expectedDataFork,
                fileEntry.getDataFork().getFixedLengthString(0, expectedDataFork.length).getBytes());
    }
}
