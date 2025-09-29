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

import java.io.IOException;
import java.util.List;

import com.webcodepro.applecommander.storage.*;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileTupleTest {
    @Test
    public void test() throws DiskException {
        Source source = Sources.create("./src/test/resources/disks/MERLIN8PRO1.DSK").orElseThrow();
        DiskFactory.Context ctx = Disks.inspect(source);
        FormattedDisk formattedDisk = ctx.disks.getFirst();
        FileTuple tuple = FileTuple.of(formattedDisk);
        FileEntry sourcerorDir = tuple.formattedDisk.getFile("SOURCEROR");
        tuple = tuple.pushd(sourcerorDir);
        FileEntry labelsSource = tuple.directoryEntry.getFiles().get(2);
        tuple = tuple.of(labelsSource);
        
        assertEquals(List.of("SOURCEROR"), tuple.paths);
        assertEquals(formattedDisk, tuple.formattedDisk);
        assertEquals(sourcerorDir, tuple.directoryEntry);
        assertEquals(labelsSource, tuple.fileEntry);
    }
}
