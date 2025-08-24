/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2019-2022 by Robert Greene and others
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
package io.github.applecommander.acx;

import com.webcodepro.applecommander.storage.DiskConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SystemTypeTest {
    @Test
    public void testEnforce140KbDisk() {
        assertEquals(DiskConstants.APPLE_140KB_DISK, SystemType.enforce140KbDisk(0));
        assertEquals(DiskConstants.APPLE_140KB_DISK, SystemType.enforce140KbDisk(DiskConstants.APPLE_140KB_DISK));
        assertEquals(DiskConstants.APPLE_140KB_DISK, SystemType.enforce140KbDisk(DiskConstants.APPLE_800KB_DISK));
    }
    
    @Test
    public void testEnforce800KbDisk() {
        assertEquals(DiskConstants.APPLE_800KB_DISK, SystemType.enforce800KbDisk(0));
        assertEquals(DiskConstants.APPLE_800KB_DISK, SystemType.enforce800KbDisk(DiskConstants.APPLE_800KB_DISK));
        assertEquals(DiskConstants.APPLE_800KB_DISK, SystemType.enforce800KbDisk(DiskConstants.APPLE_32MB_HARDDISK));
    }
    
    @Test
    public void testEnforce140KbOr800KbUpTo32MbDisk() {
        assertEquals(DiskConstants.APPLE_140KB_DISK, SystemType.enforce140KbOr800KbUpTo32MbDisk(0));
        assertEquals(DiskConstants.APPLE_140KB_DISK, SystemType.enforce140KbOr800KbUpTo32MbDisk(DiskConstants.APPLE_140KB_DISK));
        assertEquals(DiskConstants.APPLE_800KB_DISK, SystemType.enforce140KbOr800KbUpTo32MbDisk(DiskConstants.APPLE_140KB_DISK+1));
        assertEquals(DiskConstants.APPLE_800KB_DISK, SystemType.enforce140KbOr800KbUpTo32MbDisk(DiskConstants.APPLE_800KB_DISK));
        assertEquals(DiskConstants.APPLE_800KB_DISK+1, SystemType.enforce140KbOr800KbUpTo32MbDisk(DiskConstants.APPLE_800KB_DISK+1));
        assertEquals(DiskConstants.APPLE_32MB_HARDDISK, SystemType.enforce140KbOr800KbUpTo32MbDisk(DiskConstants.APPLE_32MB_HARDDISK));
        assertEquals(DiskConstants.APPLE_32MB_HARDDISK, SystemType.enforce140KbOr800KbUpTo32MbDisk(Integer.MAX_VALUE));
    }
}
