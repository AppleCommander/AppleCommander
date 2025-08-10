package org.applecommander.source;

import com.webcodepro.applecommander.testconfig.TestConfig;
import org.applecommander.image.DiskCopyImage;
import org.applecommander.image.UniversalDiskImage;
import org.junit.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertTrue;

public class SourceFactoryTest {
    private static final String DISKS = TestConfig.getInstance().getDiskDir();

    @Test
    public void testFileSourceRecognition() {
        final List<String> testCases = List.of(
                "CavernsOfFreitag.dsk",
                "D3110.dsk",
                "D3151.dsk",
                "DOS 3.2 System Master.woz",
                "DOS 3.3.po",
                "DOS 3.3 System Master.woz1",
                "DOS 3.3 System Master.woz2",
                "galatt.dsk",
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
                "UniDOS_3.3.dsk"
            );

        for (String testCase : testCases) {
            Path path = Path.of(DISKS, testCase);
            Optional<Source> source = Source.create(path);
            assertTrue(source.isPresent());
            assertTrue(source.get() instanceof FileSource);
        }
    }

    @Test
    public void testDiskCopyImageRecognition() {
        Path path = Path.of(DISKS, "Installer.dc");
        Optional<Source> source = Source.create(path);
        assertTrue(source.isPresent());
        assertTrue(source.get() instanceof DiskCopyImage);
    }

    @Test
    public void testUniversalDiskImageRecognition() {
        Path path = Path.of(DISKS, "Marble Madness (1985)(Electronic Arts).2mg");
        Optional<Source> source = Source.create(path);
        assertTrue(source.isPresent());
        assertTrue(source.get() instanceof UniversalDiskImage);
    }
}
