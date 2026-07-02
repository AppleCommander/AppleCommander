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
package com.webcodepro.applecommander.util;

import com.webcodepro.applecommander.storage.DiskConstants;
import com.webcodepro.applecommander.storage.os.dos33.DosFormatDisk;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;
import org.applecommander.device.BlockDevice;
import org.applecommander.device.DosOrderedTrackSectorDevice;
import org.applecommander.device.ProdosOrderedBlockDevice;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.hint.Hint;
import org.applecommander.source.DataBufferSource;
import org.applecommander.source.Source;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GlobGeneratorTest {
    public static final ProdosFormatDisk prodosFormatDisk;
    static {
        Source source = DataBufferSource.create(DiskConstants.APPLE_140KB_DISK, "new-disk").get();
        BlockDevice blockDevice = new ProdosOrderedBlockDevice(source, BlockDevice.STANDARD_BLOCK_SIZE);
        ProdosFormatDisk[] disks = ProdosFormatDisk.create("deleteme.po", "nothere", blockDevice);
        prodosFormatDisk = disks[0];
    }
    public static final DosFormatDisk dosFormatDisk;
    static {
        Source source = DataBufferSource.create(DiskConstants.APPLE_140KB_DISK, "new-disk").get();
        TrackSectorDevice device = new DosOrderedTrackSectorDevice(source, Hint.DOS_SECTOR_ORDER);
        DosFormatDisk[] disks = DosFormatDisk.create("deleteme.do", device);
        dosFormatDisk = disks[0];
    }

    @Test
    public void testNoDirectories() {
        assertEquals("(?i)^.*$", GlobGenerator.globToRegex("*", dosFormatDisk));
        assertEquals("(?i)^.*\\.txt$", GlobGenerator.globToRegex("*.txt", dosFormatDisk));
        assertEquals("(?i)^file.\\.txt$", GlobGenerator.globToRegex("file?.txt", dosFormatDisk));
        assertEquals("(?i)^asterisks\\\\*$", GlobGenerator.globToRegex("asterisks\\*", dosFormatDisk));
    }

    @Test
    public void testWithDirectories() {
        assertEquals("(?i)^[^/]*$", GlobGenerator.globToRegex("*", prodosFormatDisk));
        assertEquals("(?i)^[^/]*\\.txt$", GlobGenerator.globToRegex("*.txt", prodosFormatDisk));
        assertEquals("(?i)^.*/[^/]*\\.txt$", GlobGenerator.globToRegex("**/*.txt", prodosFormatDisk));
        assertEquals("(?i)^file.\\.txt$", GlobGenerator.globToRegex("file?.txt", prodosFormatDisk));
        assertEquals("(?i)^asterisks\\\\*$", GlobGenerator.globToRegex("asterisks\\*", prodosFormatDisk));
    }
}
