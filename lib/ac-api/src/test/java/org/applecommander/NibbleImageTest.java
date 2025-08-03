package org.applecommander;

import com.webcodepro.applecommander.testconfig.TestConfig;
import org.applecommander.codec.Nibble53Disk525Codec;
import org.applecommander.device.*;
import org.applecommander.source.FileSource;
import org.applecommander.source.Source;
import org.applecommander.util.DataBuffer;
import org.junit.Test;

import java.nio.file.Path;

public class NibbleImageTest {
    private static final String DISKS = TestConfig.getInstance().getDiskDir();

    @Test
    public void readTest() {
        Path path = Path.of(DISKS, "original321sysmaspls.nib");
        Source source = new FileSource(path);
        NibbleTrackReaderWriter trackReaderWriter = new NibbleImage(source);
        TrackSectorDevice tsDevice = new TrackSectorNibbleDevice(trackReaderWriter, DiskMarker.disk525sector13(), new Nibble53Disk525Codec());
        DataBuffer sectorData = tsDevice.readSector(17,12);
        StringBuilder sb = new StringBuilder();
        sb.setLength(16);
        for (int hi=0; hi<256; hi+=16) {
            System.out.printf("+%02x- ", hi);
            for (int lo=0; lo<16; lo++) {
                int b = sectorData.getUnsignedByte(hi + lo);
                System.out.printf("%02x ", b);
                b &= 0x7f;
                if (b < 0x20) {
                    b = '.';
                }
                sb.setCharAt(lo, (char)b);
            }
            System.out.printf("  %s\n", sb);
        }
    }
}
