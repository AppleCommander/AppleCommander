package org.applecommander;

import com.webcodepro.applecommander.testconfig.TestConfig;
import org.applecommander.codec.Nibble53Disk525Codec;
import org.applecommander.codec.Nibble62Disk525Codec;
import org.applecommander.device.*;
import org.applecommander.source.FileSource;
import org.applecommander.source.Source;
import org.applecommander.util.DataBuffer;
import org.junit.Test;

import java.nio.file.Path;

/** This is really an experimental test to exercise the new device components until better tests are written. */
public class NewDeviceTest {
    private static final String DISKS = TestConfig.getInstance().getDiskDir();

    @Test
    public void readNibbleImageDOS321() {
        final String filename = "original321sysmaspls.nib";
        Source source = sourceDisk(filename);
        NibbleTrackReaderWriter trackReaderWriter = new NibbleImage(source);
        TrackSectorDevice tsDevice = new TrackSectorNibbleDevice(trackReaderWriter, DiskMarker.disk525sector13(), new Nibble53Disk525Codec(), 13);
        DataBuffer sectorData = tsDevice.readSector(17,12);
        dumpAsHex(sectorData, filename);
    }

    @Test
    public void readWOZImageDOS32() {
        final String filename = "DOS 3.2 System Master.woz";
        Source source = sourceDisk(filename);
        NibbleTrackReaderWriter trackReaderWriter = new WozImage(source);
        TrackSectorDevice tsDevice = new TrackSectorNibbleDevice(trackReaderWriter, DiskMarker.disk525sector13(), new Nibble53Disk525Codec(), 13);
        DataBuffer sectorData = tsDevice.readSector(17, 12);
        dumpAsHex(sectorData, filename);
    }

    @Test
    public void readWOZ1ImageDOS33() {
        final String filename = "DOS 3.3 System Master.woz1";
        Source source = sourceDisk(filename);
        NibbleTrackReaderWriter trackReaderWriter = new WozImage(source);
        TrackSectorDevice tsDevice = new TrackSectorNibbleDevice(trackReaderWriter, DiskMarker.disk525sector16(), new Nibble62Disk525Codec(), 16);
        DataBuffer sectorData = tsDevice.readSector(17, 15);
        dumpAsHex(sectorData, filename);
    }

    @Test
    public void readWOZ2ImageDOS33() {
        final String filename = "DOS 3.3 System Master.woz2";
        Source source = sourceDisk(filename);
        NibbleTrackReaderWriter trackReaderWriter = new WozImage(source);
        TrackSectorDevice tsDevice = new TrackSectorNibbleDevice(trackReaderWriter, DiskMarker.disk525sector16(), new Nibble62Disk525Codec(), 16);
        DataBuffer sectorData = tsDevice.readSector(17, 15);
        dumpAsHex(sectorData, filename);
    }

    @Test
    public void readBlockGalatt() {
        final String filename = "galatt.dsk";
        Source source = sourceDisk(filename);
        DosOrderedTrackSectorDevice tsDevice = new DosOrderedTrackSectorDevice(source);
        TrackSectorToBlockDevice blockDevice = new TrackSectorToBlockDevice(tsDevice);
        DataBuffer blockData = blockDevice.readBlock(2);
        dumpAsHex(blockData, filename);
    }

    @Test
    public void readDOCavernsOfFreitag() {
        final String filename = "CavernsOfFreitag.dsk";
        Source source = sourceDisk(filename);
        DosOrderedTrackSectorDevice tsDevice = new DosOrderedTrackSectorDevice(source);
        DataBuffer sectorData = tsDevice.readSector(17, 15);
        dumpAsHex(sectorData, filename);
    }

    public Source sourceDisk(String filename) {
        Path path = Path.of(DISKS, filename);
        return new FileSource(path);
    }
    public void dumpAsHex(DataBuffer data, String title) {
        System.out.println("********************************************************");
        System.out.printf("** %-50.50s **\n", title);
        System.out.println("********************************************************");
        StringBuilder sb = new StringBuilder();
        sb.setLength(16);
        for (int hi=0; hi<data.limit(); hi+=16) {
            System.out.printf("+%03x- ", hi);
            for (int lo=0; lo<16; lo++) {
                int b = data.getUnsignedByte(hi + lo);
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
