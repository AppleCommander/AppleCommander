package com.webcodepro.applecommander.storage.os.dos33;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.physical.ByteArrayImageLayout;
import com.webcodepro.applecommander.storage.physical.DosOrder;
import com.webcodepro.applecommander.storage.physical.ImageOrder;

public class DosFormatDiskTest {
    @Test
    public void testSanitizeFilename() throws DiskFullException {
         ByteArrayImageLayout layout = new ByteArrayImageLayout(Disk.APPLE_140KB_DISK);
         ImageOrder order = new DosOrder(layout);
         DosFormatDisk[] disks = DosFormatDisk.create("deleteme.do", order);
         DosFormatDisk disk = disks[0];
         
         assertEquals("FILENAME", disk.getSuggestedFilename("FileName"));
         assertEquals("A2021", disk.getSuggestedFilename("2021"));
         assertEquals("A..", disk.getSuggestedFilename(".."));
         assertEquals("THE FILE NAME", disk.getSuggestedFilename("The File Name"));
         assertEquals("A\t HIDDEN TAB", disk.getSuggestedFilename("\t hidden tab"));
    }
}
