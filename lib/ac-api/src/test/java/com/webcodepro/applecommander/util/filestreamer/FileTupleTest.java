/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2021-2022 by Robert Greene and others
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
