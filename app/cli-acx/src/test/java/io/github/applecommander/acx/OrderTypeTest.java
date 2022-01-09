package io.github.applecommander.acx;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.webcodepro.applecommander.storage.Disk;

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
