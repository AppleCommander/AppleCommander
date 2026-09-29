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

import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.FileStore;
import org.applecommander.filestore.FileStoreFactory;
import org.applecommander.filestore.FileStores;
import org.applecommander.os.gamedos.GamedosDirectory;
import org.applecommander.os.gamedos.GamedosFileEntry;
import org.applecommander.os.gamedos.GamedosFileStore;
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
}
