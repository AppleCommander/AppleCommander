package com.webcodepro.applecommander.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFileEntry;
import com.webcodepro.applecommander.ui.ac;

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
		
		Disk disk = new Disk(tmpImageName);
		FormattedDisk formattedDisk = disk.getFormattedDisks()[0];
		List<FileEntry> files = formattedDisk.getFiles();
		assertNotNull(files);
		assertEquals(1, files.size());
		ProdosFileEntry file = (ProdosFileEntry)files.get(0);
		assertEquals("HELLO", file.getFilename());
		assertEquals("BIN", file.getFiletype());
		assertEquals(0x0803, file.getAuxiliaryType());
	}
}
