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
package com.webcodepro.applecommander.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.webcodepro.applecommander.storage.*;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFileEntry;
import com.webcodepro.applecommander.ui.ac;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AppleSingleTest {
	private static final String AS_HELLO_BIN = "/hello.applesingle.bin";

	@Test
	public void testViaAcTool() throws IOException, DiskException {
		// Create a file that the JVM *should* delete for us.
		File tmpDiskImage = File.createTempFile("deleteme-", ".po");
		tmpDiskImage.deleteOnExit();
		String tmpImageName = tmpDiskImage.getAbsolutePath();
		
		// Create disk
		ac.createProDisk(tmpImageName, "DELETEME", Disk.APPLE_140KB_DISK);
		
		// Actually test the implementation!
		ac.putAppleSingle(tmpImageName, "HELLO", getClass().getResourceAsStream(AS_HELLO_BIN));

        Source source = Sources.create(Path.of(tmpImageName)).orElseThrow();
        DiskFactory.Context ctx = Disks.inspect(source);
		FormattedDisk formattedDisk = ctx.disks.getFirst();
		List<FileEntry> files = formattedDisk.getFiles();
		assertNotNull(files);
		assertEquals(1, files.size());
		ProdosFileEntry file = (ProdosFileEntry)files.getFirst();
		assertEquals("HELLO", file.getFilename());
		assertEquals("BIN", file.getFiletype());
		assertEquals(0x0803, file.getAuxiliaryType());
	}
}
