package com.webcodepro.applecommander.storage.os.pascal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.physical.ByteArrayImageLayout;
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import com.webcodepro.applecommander.storage.physical.ProdosOrder;

public class PascalFormatDiskTest {
    @Test
    public void testSanitizeFilename() throws DiskFullException {
        ByteArrayImageLayout layout = new ByteArrayImageLayout(Disk.APPLE_140KB_DISK);
        ImageOrder order = new ProdosOrder(layout);
        PascalFormatDisk[] disks = PascalFormatDisk.create("deleteme.po", "TEST", order); 
        PascalFormatDisk disk = disks[0];
        
        assertEquals("FILENAME", disk.getSuggestedFilename("FileName"));
        assertEquals("2021", disk.getSuggestedFilename("2021"));
        assertEquals("..", disk.getSuggestedFilename(".."));
        assertEquals("THEFILENAME", disk.getSuggestedFilename("The File Name"));
        assertEquals("HIDDENTAB", disk.getSuggestedFilename("\t hidden tab"));
    }
}
