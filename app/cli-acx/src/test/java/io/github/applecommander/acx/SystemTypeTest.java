package io.github.applecommander.acx;

import org.junit.Test;

import static org.junit.Assert.*;
import com.webcodepro.applecommander.storage.Disk;

public class SystemTypeTest {
    @Test
    public void testEnforce140KbDisk() {
        assertEquals(Disk.APPLE_140KB_DISK, SystemType.enforce140KbDisk(0));
        assertEquals(Disk.APPLE_140KB_DISK, SystemType.enforce140KbDisk(Disk.APPLE_140KB_DISK));
        assertEquals(Disk.APPLE_140KB_DISK, SystemType.enforce140KbDisk(Disk.APPLE_800KB_DISK));
    }
    
    @Test
    public void testEnforce800KbDisk() {
        assertEquals(Disk.APPLE_800KB_DISK, SystemType.enforce800KbDisk(0));
        assertEquals(Disk.APPLE_800KB_DISK, SystemType.enforce800KbDisk(Disk.APPLE_800KB_DISK));
        assertEquals(Disk.APPLE_800KB_DISK, SystemType.enforce800KbDisk(Disk.APPLE_32MB_HARDDISK));
    }
    
    @Test
    public void testEnforce140KbOr800KbUpTo32MbDisk() {
        assertEquals(Disk.APPLE_140KB_DISK, SystemType.enforce140KbOr800KbUpTo32MbDisk(0));
        assertEquals(Disk.APPLE_140KB_DISK, SystemType.enforce140KbOr800KbUpTo32MbDisk(Disk.APPLE_140KB_DISK));
        assertEquals(Disk.APPLE_800KB_DISK, SystemType.enforce140KbOr800KbUpTo32MbDisk(Disk.APPLE_140KB_DISK+1));
        assertEquals(Disk.APPLE_800KB_DISK, SystemType.enforce140KbOr800KbUpTo32MbDisk(Disk.APPLE_800KB_DISK));
        assertEquals(Disk.APPLE_800KB_DISK+1, SystemType.enforce140KbOr800KbUpTo32MbDisk(Disk.APPLE_800KB_DISK+1));
        assertEquals(Disk.APPLE_32MB_HARDDISK, SystemType.enforce140KbOr800KbUpTo32MbDisk(Disk.APPLE_32MB_HARDDISK));
        assertEquals(Disk.APPLE_32MB_HARDDISK, SystemType.enforce140KbOr800KbUpTo32MbDisk(Integer.MAX_VALUE));
    }
}
