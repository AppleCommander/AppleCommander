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
package org.applecommander;

import com.webcodepro.applecommander.testconfig.TestConfig;
import org.applecommander.device.nibble.Nibble53Disk525Codec;
import org.applecommander.device.nibble.Nibble62Disk525Codec;
import org.applecommander.device.*;
import org.applecommander.device.nibble.DiskMarker;
import org.applecommander.device.nibble.NibbleTrackReaderWriter;
import org.applecommander.hint.Hint;
import org.applecommander.image.DiskCopyImage;
import org.applecommander.image.NibbleImage;
import org.applecommander.image.UniversalDiskImage;
import org.applecommander.image.WozImage;
import org.applecommander.os.dos.UnidosAdapterStrategy;
import org.applecommander.source.FileSource;
import org.applecommander.source.Source;
import org.applecommander.util.DataBuffer;
import org.junit.jupiter.api.Test;

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
        DosOrderedTrackSectorDevice doDevice = new DosOrderedTrackSectorDevice(source, Hint.DOS_SECTOR_ORDER);
        TrackSectorDevice skewedDevice = SkewedTrackSectorDevice.dosToPascalSkew(doDevice);
        TrackSectorToBlockAdapter blockDevice = new TrackSectorToBlockAdapter(skewedDevice, TrackSectorToBlockAdapter.BlockStyle.PRODOS);
        DataBuffer blockData = blockDevice.readBlock(2);
        dumpAsHex(blockData, filename);
    }

    @Test
    public void readDOCavernsOfFreitag() {
        final String filename = "CavernsOfFreitag.dsk";
        Source source = sourceDisk(filename);
        DosOrderedTrackSectorDevice tsDevice = new DosOrderedTrackSectorDevice(source, Hint.DOS_SECTOR_ORDER);
        DataBuffer sectorData = tsDevice.readSector(17, 15);
        dumpAsHex(sectorData, filename);
    }

    @Test
    public void readUniDOS33() {
        final String filename = "UniDOS_3.3.dsk";
        Source source = sourceDisk(filename);
        BlockDevice blockDevice = new ProdosOrderedBlockDevice(source, BlockDevice.STANDARD_BLOCK_SIZE);
        TrackSectorDevice disk1 = new BlockToTrackSectorAdapter(blockDevice, UnidosAdapterStrategy.UNIDOS_DISK_1);
        TrackSectorDevice disk2 = new BlockToTrackSectorAdapter(blockDevice, UnidosAdapterStrategy.UNIDOS_DISK_2);
        DataBuffer sector1 = disk1.readSector(17, 31);
        DataBuffer sector2 = disk2.readSector(17, 31);
        dumpAsHex(sector1, filename + " (1)");
        dumpAsHex(sector2, filename + " (2)");
    }

    @Test
    public void read2ImgDisk() {
        // Note: Writing this as if we have to determine details from the 2IMG structure itself
        final String filename = "Marble Madness (1985)(Electronic Arts).2mg";
        Source source = sourceDisk(filename);
        UniversalDiskImage image = new UniversalDiskImage(source);
        UniversalDiskImage.Info info = image.getInfo();
        // An attempt at discovery
        Object device = null;
        if (info.isDOSOrdered() && info.dataLength() == 143360) {
            device = new DosOrderedTrackSectorDevice(image, Hint.DOS_SECTOR_ORDER);
        }
        else if (info.isProdosOrdered()) {
            device = new ProdosOrderedBlockDevice(image, BlockDevice.STANDARD_BLOCK_SIZE);
        }
        else if (info.isNibbleOrder()) {
            // this is just guessing, and likely never occurs from what I've found, but...
            NibbleTrackReaderWriter trackReaderWriter = new NibbleImage(image);
            device = new TrackSectorNibbleDevice(trackReaderWriter, DiskMarker.disk525sector16(), new Nibble62Disk525Codec(), 16);
        }
        assert(device != null);
        // Report out... making grand assumption that TS=DOS and block=ProDOS
        if (device instanceof TrackSectorDevice tsDevice) {
            DataBuffer sectorData = tsDevice.readSector(17, 15);
            dumpAsHex(sectorData, filename);
        }
        else {
            BlockDevice blkDevice = (BlockDevice) device;
            DataBuffer blockData = blkDevice.readBlock(2);
            dumpAsHex(blockData, filename);
        }
    }

    @Test
    public void readDiskCopyDisk() {
        final String filename = "Installer.dc";
        Source source = sourceDisk(filename);
        DiskCopyImage image = new DiskCopyImage(source);
        BlockDevice device = new ProdosOrderedBlockDevice(image, BlockDevice.STANDARD_BLOCK_SIZE);
        DataBuffer block = device.readBlock(2);
        dumpAsHex(block, filename);
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
