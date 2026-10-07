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

        // Create our file
        GamedosFileEntry fileEntry = (GamedosFileEntry) directory.createFile("This is a text file.");
        assertNotNull(fileEntry);
        final String textContents = "THIS IS A THE FILE DATA.";
        DataBuffer fileData = DataBuffer.wrap(textContents.getBytes());
        fileEntry.setFiletype("T");
        fileEntry.setDataFork(fileData);

        // Verify the file went where we expected it to
        assertEquals("T", fileEntry.getFiletype());
        assertEquals(1, fileEntry.getFirstTrack());
        assertEquals(0, fileEntry.getFirstSector());
        assertEquals(1, fileEntry.getSectorCount());
        assertEquals(textContents, fileEntry.getDataFork().getFixedLengthString(0, textContents.length()));

        // Read back to file entry from the directory and then delete it.
        assertEquals(1, directory.getFiles().size());
        GamedosFileEntry firstFileEntry = (GamedosFileEntry) directory.getFiles().getFirst();
        assertNotNull(firstFileEntry);
        directory.deleteFile(firstFileEntry);
        assertEquals(0, directory.getFiles().size());
    }
}
