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

import com.webcodepro.applecommander.storage.Disk;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrderTypeTest {
    @Test
    public void testCreateDosImageOrder() {
        assertEquals(Disk.APPLE_140KB_DISK, OrderType.createDosImageOrder(0).getPhysicalSize());
        assertEquals(Disk.APPLE_140KB_DISK, OrderType.createDosImageOrder(Disk.APPLE_140KB_DISK).getPhysicalSize());
        assertEquals(Disk.APPLE_800KB_DISK, OrderType.createDosImageOrder(Disk.APPLE_140KB_DISK+1).getPhysicalSize());
        assertEquals(Disk.APPLE_800KB_DISK, OrderType.createDosImageOrder(Disk.APPLE_800KB_DISK).getPhysicalSize());
        assertEquals(Disk.APPLE_800KB_DISK, OrderType.createDosImageOrder(Disk.APPLE_800KB_DISK+1).getPhysicalSize());
    }
    
    @Test
    public void testCreate140kNibbleImageOrder() {
        assertEquals(Disk.APPLE_140KB_NIBBLE_DISK, OrderType.create140kNibbleImageOrder(0).getPhysicalSize());
        assertEquals(Disk.APPLE_140KB_NIBBLE_DISK, OrderType.create140kNibbleImageOrder(Disk.APPLE_140KB_DISK).getPhysicalSize());
        assertEquals(Disk.APPLE_140KB_NIBBLE_DISK, OrderType.create140kNibbleImageOrder(Disk.APPLE_140KB_DISK+1).getPhysicalSize());
    }
    
    @Test
    public void testCreateProdosImageOrder() {
        assertEquals(Disk.APPLE_140KB_DISK, OrderType.createProdosImageOrder(0).getPhysicalSize());
        assertEquals(Disk.APPLE_140KB_DISK, OrderType.createProdosImageOrder(Disk.APPLE_140KB_DISK).getPhysicalSize());
        assertEquals(Disk.APPLE_800KB_DISK, OrderType.createProdosImageOrder(Disk.APPLE_140KB_DISK+1).getPhysicalSize());
        assertEquals(Disk.APPLE_800KB_DISK, OrderType.createProdosImageOrder(Disk.APPLE_800KB_DISK).getPhysicalSize());
        assertEquals(Disk.APPLE_800KB_DISK+1, OrderType.createProdosImageOrder(Disk.APPLE_800KB_DISK+1).getPhysicalSize());
        assertEquals(Disk.APPLE_10MB_HARDDISK, OrderType.createProdosImageOrder(Disk.APPLE_10MB_HARDDISK).getPhysicalSize());
    }
}
