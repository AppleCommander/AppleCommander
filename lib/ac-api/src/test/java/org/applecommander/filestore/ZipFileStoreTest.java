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

import org.applecommander.archive.zip.ZipFileStore;
import org.applecommander.source.DataBufferSource;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ZipFileStoreTest {
    @Test
    public void test() {
        final String fullPath = "src/test/resources/disks/emptyDSK_Prodos.zip";
        Source source = Sources.create(fullPath).orElseThrow();
        FileStoreFactory.Context ctx = FileStores.inspect(source);
        assertFalse(ctx.fileStores.isEmpty());
        assertEquals(1, ctx.fileStores.size());
        FileStore fileStore = ctx.fileStores.getFirst();
        if (fileStore instanceof ZipFileStore zipFileStore) {
            Directory directory = zipFileStore.getRootDirectory();
            List<FileEntry> files = directory.getFiles();
            assertNotNull(files);
            assertEquals(1, files.size());
            FileEntry fileEntry = files.getFirst();
            assertEquals("EMPTY PRODOS.DSK", fileEntry.getName());
            byte[] data = fileEntry.getDataFork().asBytes();
            assertNotNull(data);
            assertEquals(143360, data.length);
            Source disk = DataBufferSource.create(data, fileEntry.getName()).get();
            FileStoreTestHelper.showDirectory(disk);
        }
        else {
            fail("Expecting a ZipFileStore!");
        }
    }
}
