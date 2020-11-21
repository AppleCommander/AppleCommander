package com.webcodepro.applecommander.util.filestreamer;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;

public class FileTupleTest {
    @Test
    public void test() throws IOException, DiskException {
        Disk disk = new Disk("./src/test/resources/disks/MERLIN8PRO1.DSK");
        FormattedDisk formattedDisk = disk.getFormattedDisks()[0];
        FileTuple tuple = FileTuple.of(formattedDisk);
        FileEntry sourcerorDir = tuple.formattedDisk.getFile("SOURCEROR");
        tuple = tuple.pushd(sourcerorDir);
        FileEntry labelsSource = tuple.directoryEntry.getFiles().get(2);
        tuple = tuple.of(labelsSource);
        
        assertEquals(Arrays.asList("SOURCEROR"), tuple.paths);
        assertEquals(formattedDisk, tuple.formattedDisk);
        assertEquals(sourcerorDir, tuple.directoryEntry);
        assertEquals(labelsSource, tuple.fileEntry);
    }
}
