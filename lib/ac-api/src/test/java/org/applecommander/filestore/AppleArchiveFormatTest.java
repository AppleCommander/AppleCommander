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
package org.applecommander.filestore;

import org.applecommander.archive.aaf.AppleArchiveFormatFileStore;
import org.applecommander.source.DataBufferSource;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AppleArchiveFormatTest {
    @Test
    public void test() {
        final String fullPath = "src/test/resources/disks/example.aaf";
        Source source = Sources.create(fullPath).orElseThrow();
        FileStoreFactory.Context ctx = FileStores.inspect(source);
        assertFalse(ctx.fileStores.isEmpty());
        assertEquals(1, ctx.fileStores.size());
        FileStore fileStore = ctx.fileStores.getFirst();
        if (fileStore instanceof AppleArchiveFormatFileStore aafFileStore) {
            Directory directory = aafFileStore.getRootDirectory();
            List<FileEntry> files = directory.getFiles();
            assertNotNull(files);
            assertEquals(2, files.size());
            // File 1
            FileEntry fileEntry = files.getFirst();
            assertEquals("helloworld.c", fileEntry.getName());
            byte[] data = fileEntry.getDataFork().asBytes();
            assertNotNull(data);
            assertEquals("""
                    main()
                    {
                           printf("Hello World\\n");
                    }
                    """,
                    new String(data));
            // File 2
            fileEntry = files.getLast();
            assertEquals("Read.Me", fileEntry.getName());
            data = fileEntry.getDataFork().asBytes();
            assertNotNull(data);
            assertEquals("""
                    Test out a C compiler; just compile and execute.
                    """,
                    new String(data));
        }
        else {
            fail("Expecting a AppleArchiveFormatFileStore!");
        }
    }
}
