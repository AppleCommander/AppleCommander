/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2022 by Robert Greene and others
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
package com.webcodepro.applecommander.storage.os.dos33;

import com.webcodepro.applecommander.storage.DiskConstants;
import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.util.AppleUtil;
import org.applecommander.device.OrderedTrackSectorDevice;
import org.applecommander.device.SectorOrder;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.source.DataBufferSource;
import org.applecommander.source.Source;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DosFormatDiskTest {
    @Test
    public void testSanitizeFilename() {
        Source source = DataBufferSource.create(DiskConstants.APPLE_140KB_DISK, "new-image").get();
        TrackSectorDevice device = new OrderedTrackSectorDevice(source, SectorOrder.DOS);
        DosFormatDisk[] disks = DosFormatDisk.create("deleteme.do", device);
        DosFormatDisk disk = disks[0];
         
        assertEquals("FILENAME", disk.getSuggestedFilename("FileName"));
        assertEquals("A2021", disk.getSuggestedFilename("2021"));
        assertEquals("A..", disk.getSuggestedFilename(".."));
        assertEquals("THE FILE NAME", disk.getSuggestedFilename("The File Name"));
        assertEquals("A\t HIDDEN TAB", disk.getSuggestedFilename("\t hidden tab"));
    }

    @Test
    public void testLargeFiles_ticket331() throws DiskFullException {
        Source source = DataBufferSource.create(DiskConstants.APPLE_140KB_DISK, "new-image").get();
        TrackSectorDevice device = new OrderedTrackSectorDevice(source, SectorOrder.DOS);
        DosFormatDisk[] disks = DosFormatDisk.create("deleteme.do", device);
        DosFormatDisk disk = disks[0];

        // Eack T/S list holds 122*256 = 31232 bytes (less the first 4 bytes)
        final byte[] data = new byte[70000];
        Arrays.fill(data, (byte)0xaa);
        DosFileEntry fileEntry = disk.createFile();
        fileEntry.setFilename("TEST FILE");
        fileEntry.setFiletype("B");
        fileEntry.setAddress(0x800);
        fileEntry.setFileData(data);

        // First track/sector list
        byte[] sectorData = disk.readSector(fileEntry.getTrack(), fileEntry.getSector());
        assertTrue(sectorData[0x01] != 0);
        assertEquals(0, AppleUtil.getWordValue(sectorData, 0x05));
        // Second track/sector list
        sectorData = disk.readSector(Byte.toUnsignedInt(sectorData[0x01]), Byte.toUnsignedInt(sectorData[0x02]));
        assertTrue(sectorData[0x01] != 0);
        assertEquals(122, AppleUtil.getWordValue(sectorData, 0x05));
        // Third track/sector list
        sectorData = disk.readSector(Byte.toUnsignedInt(sectorData[0x01]), Byte.toUnsignedInt(sectorData[0x02]));
        assertTrue(sectorData[0x01] == 0);
        assertEquals(244, AppleUtil.getWordValue(sectorData, 0x05));
    }
}
