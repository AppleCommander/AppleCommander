package com.webcodepro.applecommander.storage.os.prodos;

import static org.junit.Assert.*;

import org.junit.Test;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.physical.ByteArrayImageLayout;
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import com.webcodepro.applecommander.storage.physical.ProdosOrder;

public class ProdosFormatDiskTest {
    @Test
    public void testSanitizeFilename() throws DiskFullException {
        ByteArrayImageLayout layout = new ByteArrayImageLayout(Disk.APPLE_140KB_DISK);
        ImageOrder order = new ProdosOrder(layout);
        ProdosFormatDisk[] disks = ProdosFormatDisk.create("deleteme.po", "nothere", order);
        ProdosFormatDisk disk = disks[0];
        
        assertEquals("FILENAME", disk.getSuggestedFilename("FileName"));
        assertEquals("A2021", disk.getSuggestedFilename("2021"));
        assertEquals("A..", disk.getSuggestedFilename(".."));
        assertEquals("THE.FILE.NAME", disk.getSuggestedFilename("The File Name"));
        assertEquals("A..HIDDEN.TAB", disk.getSuggestedFilename("\t hidden tab"));
    }
}
