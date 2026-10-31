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

import org.applecommander.device.BlockDevice;
import org.applecommander.device.SectorOrder;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.util.DataBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileStoreOrganizationTest {
    public static Stream<FileStoreOrganization> sixteenSectorDevices() {
        return Stream.of(FileStoreOrganization.values())
            .filter(organization -> organization.getSectorOrder() != null)
            .filter(organization -> organization != FileStoreOrganization.DISK_13SECTOR_DO);
    }

    /// A generalized test to verify sector skewing.
    @ParameterizedTest
    @MethodSource("sixteenSectorDevices")
    public void testTrackSectorDevices(FileStoreOrganization organization) {
        TrackSectorDevice device = organization.createTrackSectorDevice();
        DataBuffer sectorData = DataBuffer.create(TrackSectorDevice.SECTOR_SIZE);
        final int sectorsPerTrack = device.getGeometry().sectorsPerTrack();
        // Place markers on every physical sector
        TrackSectorDevice physicalOrder = device.request(SectorOrder.NIBBLE);
        for (int s=0; s<sectorsPerTrack; s++) {
            sectorData.putByte(0, s);
            physicalOrder.writeSector(0, s, sectorData);
        }
        // Read in ProDOS order and the markers should be the physical->ProDOS mapping
        final int[] prodosInterleave = { 0, 2, 4, 6, 8, 10, 12, 14, 1, 3, 5, 7, 9, 11, 13, 15 };
        TrackSectorDevice prodosOrder = device.request(SectorOrder.PRODOS);
        for (int s=0; s<sectorsPerTrack; s++) {
            sectorData = prodosOrder.readSector(0, s);
            assertEquals(prodosInterleave[s], sectorData.getUnsignedByte(0));
        }
        // Read in Physical (Nibble) order and the markers should be DOS->Physical mapping
        final int[] dosInterleave = { 0, 13, 11, 9, 7, 5, 3, 1, 14, 12, 10, 8, 6, 4, 2, 15 };
        TrackSectorDevice dosOrder = device.request(SectorOrder.DOS);
        for (int s=0; s<sectorsPerTrack; s++) {
            sectorData = dosOrder.readSector(0, s);
            assertEquals(dosInterleave[s], sectorData.getUnsignedByte(0));
        }
    }

    /// A generalized test to verify sector skewing. The premise is that we now create
    /// a `TrackSectorDevice` underlying the ProDOS `BlockDevice`. Meaning that the
    /// individual sectors can be accessed and _then_ the blocks can be verified.
    @ParameterizedTest
    @MethodSource("sixteenSectorDevices")
    public void testBlockDevices(FileStoreOrganization organization) {
        BlockDevice device = organization.createBlockDevice();
        TrackSectorDevice trackSectorDevice = device.get(TrackSectorDevice.class).orElseThrow();
        DataBuffer sectorData = DataBuffer.create(TrackSectorDevice.SECTOR_SIZE);
        final int sectorsPerTrack = trackSectorDevice.getGeometry().sectorsPerTrack();
        // Place markers on every physical sector
        TrackSectorDevice physicalOrder = trackSectorDevice.request(SectorOrder.NIBBLE);
        for (int s=0; s<sectorsPerTrack; s++) {
            sectorData.putByte(0, s);
            physicalOrder.writeSector(0, s, sectorData);
        }
        // Now check the blocks read back.
        final int[][] prodosInterleave = {{0, 2}, {4, 6}, {8, 10}, {12, 14}, {1, 3}, {5, 7}, {9, 11}, {13, 15} };
        for (int b=0; b<prodosInterleave.length; b++) {
            DataBuffer blockData = device.readBlock(b);
            assertEquals(prodosInterleave[b][0], blockData.getUnsignedByte(0));
            assertEquals(prodosInterleave[b][1], blockData.getUnsignedByte(0x100));
        }
    }
}
