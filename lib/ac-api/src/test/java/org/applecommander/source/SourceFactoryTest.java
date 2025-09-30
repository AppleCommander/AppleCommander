/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2025 by Robert Greene and others
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
package org.applecommander.source;

import com.webcodepro.applecommander.testconfig.TestConfig;
import org.applecommander.image.DiskCopyImage;
import org.applecommander.image.UniversalDiskImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class SourceFactoryTest {
    private static final String DISKS = TestConfig.getInstance().getDiskDir();

    @ParameterizedTest
    @ValueSource(strings = {
            "CavernsOfFreitag.dsk",
            "D3110.dsk",
            "D3151.dsk",
            "DOS 3.2 System Master.woz",
            "DOS 3.3.po",
            "DOS 3.3 System Master.woz1",
            "DOS 3.3 System Master.woz2",
            "galatt.dsk",
            "Gutenberg_side1.DSK",
            "MASTER.DSK",
            "MERLIN8PRO1.DSK",
            "original321sysmaspls.nib",
            "original332sysmas.do",
            "phan2d1.dsk",
            "phan2d2.dsk",
            "PHANTA31.DSK",
            "PHANTA32.DSK",
            "Phantasie1.dsk",
            "Phantasie2.dsk",
            "Prodos.dsk",
            "RDOSboot.dsk",
            "SSIsave.dsk",
            "Super-Mon-dev.dsk",
            "UniDOS_3.3.dsk"
        })
    public void testFileSourceRecognition(String filename) {
        Path path = Path.of(DISKS, filename);
        Optional<Source> source = Sources.create(path);
        assertTrue(source.isPresent());
        assertInstanceOf(FileSource.class, source.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Installer.dc",
            "3132.DSK.gz"
        })
    public void testDiskCopyImageRecognition(String filename) {
        Path path = Path.of(DISKS, filename);
        Optional<Source> source = Sources.create(path);
        assertTrue(source.isPresent());
        assertInstanceOf(DiskCopyImage.class, source.get());
        // We are assuming that these DiskCopy images are well-formed to validate the checksum algorithm
        DiskCopyImage dcImage = (DiskCopyImage) source.get();
        assertEquals(dcImage.getInfo().dataChecksum(), dcImage.getInfo().calculatedDataChecksum());
        assertEquals(dcImage.getInfo().tagChecksum(), dcImage.getInfo().calculatedTagChecksum());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Marble Madness (1985)(Electronic Arts).2mg"
        })
    public void testUniversalDiskImageRecognition(String filename) {
        Path path = Path.of(DISKS, filename);
        Optional<Source> source = Sources.create(path);
        assertTrue(source.isPresent());
        assertInstanceOf(UniversalDiskImage.class, source.get());
    }
}
