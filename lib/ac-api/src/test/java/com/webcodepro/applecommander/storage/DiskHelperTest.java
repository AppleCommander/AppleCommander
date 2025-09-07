/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2022 by Robert Greene
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
package com.webcodepro.applecommander.storage;

import com.webcodepro.applecommander.storage.FormattedDisk.DiskUsage;
import com.webcodepro.applecommander.storage.filters.*;
import com.webcodepro.applecommander.testconfig.TestConfig;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test Disk and FormattedDisk for read.
 * <p>
 * Date created: Oct 3, 2002 11:35:26 PM
 * @author Rob Greene
 */
public class DiskHelperTest {
	private static final String DOS33_FORMAT = "DOS 3.3";
	private static final String DOS32_FORMAT = "DOS 3.2";

	private TestConfig config = TestConfig.getInstance();

	@Test
	public void testLoadDos33() throws IOException, DiskException {
		FormattedDisk[] disks = showDirectory(config.getDiskDir() +
				"/DOS 3.3.po"); //$NON-NLS-1$
		assertApplesoftFile(disks[0], "HELLO"); //$NON-NLS-1$
		assertIntegerFile(disks[0], "ANIMALS"); //$NON-NLS-1$
		assertTextFile(disks[0], "APPLE PROMS"); //$NON-NLS-1$
		assertBinaryFile(disks[0], "BOOT13"); //$NON-NLS-1$
		assertEquals(DOS33_FORMAT, disks[0].getFormat());
        assertCanReadFiles(disks);
	}

	@Test
	public void testLoadMaster() throws IOException, DiskException {
        FormattedDisk[] disks = showDirectory(config.getDiskDir() + "/MASTER.DSK"); //$NON-NLS-1$
        assertCanReadFiles(disks);
	}
	
	@Test
	public void testLoadGalacticAttack1() throws IOException, DiskException {
        FormattedDisk[] disks = showDirectory(config.getDiskDir() + "/galatt.dsk"); //$NON-NLS-1$
        assertCanReadFiles(disks);
	}
	
	@Test
	public void testLoadProdos() throws IOException, DiskException {
		FormattedDisk[] disks = showDirectory(config.getDiskDir() + "/Prodos.dsk"); //$NON-NLS-1$
		assertApplesoftFile(disks[0], "COPY.ME"); //$NON-NLS-1$
		assertBinaryFile(disks[0], "SETTINGS"); //$NON-NLS-1$
		assertDisassemblyFile(disks[0], "PRODOS"); //$NON-NLS-1$
        assertCanReadFiles(disks);
	}
	
	@Test
	public void testLoadMarbleMadness() throws IOException, DiskException {
        FormattedDisk[] disks = showDirectory(config.getDiskDir()
				+ "/Marble Madness (1985)(Electronic Arts).2mg"); //$NON-NLS-1$
        assertCanReadFiles(disks);
	}
	
	@Test
	public void testRdosBoot() throws IOException, DiskException {
        FormattedDisk[] disks = showDirectory(config.getDiskDir() + "/RDOSboot.dsk"); //$NON-NLS-1$
        assertCanReadFiles(disks);
	}

	@Test
	public void testSsiSave() throws IOException, DiskException {
        FormattedDisk[] disks = showDirectory(config.getDiskDir() + "/SSIsave.dsk"); //$NON-NLS-1$
        assertCanReadFiles(disks);
	}

	@Test
	public void testPhanta31() throws IOException, DiskException {
		FormattedDisk[] disks = showDirectory(config.getDiskDir() 
				+ "/PHANTA31.DSK"); //$NON-NLS-1$
		assertApplesoftFile(disks[0], "PHANTASIE III"); //$NON-NLS-1$
		assertBinaryFile(disks[0], "TWN31"); //$NON-NLS-1$
		assertTextFile(disks[0], "ITEM"); //$NON-NLS-1$
		assertGraphicsFile(disks[0], "ICE DRAGON"); //$NON-NLS-1$
        assertCanReadFiles(disks);
	}

	@Test
	public void testPhanta32() throws IOException, DiskException {
        FormattedDisk[] disks = showDirectory(config.getDiskDir() + "/PHANTA32.DSK"); //$NON-NLS-1$
        assertCanReadFiles(disks);
	}

	@Test
	public void testPhan2d1() throws IOException, DiskException {
		FormattedDisk[] disks = showDirectory(config.getDiskDir() 
				+ "/phan2d1.dsk"); //$NON-NLS-1$
		assertApplesoftFile(disks[0], "PHANTASIE II"); //$NON-NLS-1$
		assertBinaryFile(disks[0], "TWN21"); //$NON-NLS-1$
		assertTextFile(disks[0], "ITEM"); //$NON-NLS-1$
		assertGraphicsFile(disks[0], "ICE DRAGON"); //$NON-NLS-1$
        assertCanReadFiles(disks);
	}

	@Test
	public void testPhan2d2() throws IOException, DiskException {
        FormattedDisk[] disks = showDirectory(config.getDiskDir() + "/phan2d2.dsk"); //$NON-NLS-1$
        assertCanReadFiles(disks);
	}

	@Test
	public void testPhantasie1() throws IOException, DiskException {
        FormattedDisk[] disks = showDirectory(config.getDiskDir() + "/Phantasie1.dsk"); //$NON-NLS-1$
        assertCanReadFiles(disks);
	}

	@Test
	public void testPhantasie2() throws IOException, DiskException {
        FormattedDisk[] disks = showDirectory(config.getDiskDir() + "/Phantasie2.dsk"); //$NON-NLS-1$
        assertCanReadFiles(disks);
	}

	@Test
	public void testCavernsOfFreitag() throws IOException, DiskException {
		FormattedDisk[] disks = showDirectory(config.getDiskDir() 
				+ "/CavernsOfFreitag.dsk"); //$NON-NLS-1$
		assertGraphicsFile(disks[0], "TITLE.PIC"); //$NON-NLS-1$
        assertCanReadFiles(disks);
	}
	
	@Test
	public void testUniDosD3110() throws IOException, DiskException {
        FormattedDisk[] disks = showDirectory(config.getDiskDir()
				+ "/D3110.dsk"); //$NON-NLS-1$
        assertCanReadFiles(disks);
	}

	@Test
	public void testUniDosD3151() throws IOException, DiskException {
		FormattedDisk[] disks = showDirectory(config.getDiskDir()
				+ "/D3151.dsk"); //$NON-NLS-1$
        assertCanReadFiles(disks);
	}

	@Test
	public void testLoadDos33SystemMasterWoz1() throws IOException, DiskException {
		FormattedDisk[] disks = showDirectory(config.getDiskDir() +
				"/DOS 3.3 System Master.woz1");
		assertApplesoftFile(disks[0], "HELLO"); //$NON-NLS-1$
		assertIntegerFile(disks[0], "COPY"); //$NON-NLS-1$
		assertBinaryFile(disks[0], "BOOT13"); //$NON-NLS-1$
		assertEquals(DOS33_FORMAT, disks[0].getFormat());
        assertCanReadFiles(disks);
	}

	@Test
	public void testLoadDos33SystemMasterWoz2() throws IOException, DiskException {
		FormattedDisk[] disks = showDirectory(config.getDiskDir() +
				"/DOS 3.3 System Master.woz2");
		assertApplesoftFile(disks[0], "HELLO"); //$NON-NLS-1$
		assertIntegerFile(disks[0], "COPY"); //$NON-NLS-1$
		assertBinaryFile(disks[0], "BOOT13"); //$NON-NLS-1$
		assertEquals(DOS33_FORMAT, disks[0].getFormat());
        assertCanReadFiles(disks);
	}

	@Test
	public void testLoadDos32SystemMasterWoz() throws IOException, DiskException {
		FormattedDisk[] disks = showDirectory(config.getDiskDir() +
				"/DOS 3.2 System Master.woz");
		assertIntegerFile(disks[0], "HELLO"); //$NON-NLS-1$
		assertBinaryFile(disks[0], "UPDATE 3.2"); //$NON-NLS-1$
		assertEquals(DOS32_FORMAT, disks[0].getFormat());
        assertCanReadFiles(disks);
	}

	@Test
	public void testLoadDos32SystemMasterNib() throws IOException, DiskException {
		FormattedDisk[] disks = showDirectory(config.getDiskDir() +
				"/original321sysmaspls.nib");
		assertApplesoftFile(disks[0], "HELLO");
		assertBinaryFile(disks[0], "UPDATE 3.2.1");
		assertTextFile(disks[0], "APPLE PROMS");
		assertEquals(DOS32_FORMAT, disks[0].getFormat());
        assertCanReadFiles(disks);
	}

    @Test
    public void testLoadNakedosSuperMonDisk() throws DiskException, IOException {
        FormattedDisk[] disks = showDirectory(config.getDiskDir() +
                "/Super-Mon-dev.dsk");
        assertCanReadFiles(disks);
    }

    @Test
    public void testLoadGutenbergDisk() throws DiskException, IOException {
        FormattedDisk[] disks = showDirectory(config.getDiskDir() +
                "/Gutenberg_side1.DSK");
        assertCanReadFiles(disks);
    }

    @Test
    public void testLoad3132Disk() throws DiskException, IOException {
        FormattedDisk[] disks = showDirectory(config.getDiskDir() +
                "/3132.DSK.gz");
        assertCanReadFiles(disks);
    }

    @Test
    public void testCPMV233Disk() throws DiskException, IOException {
        FormattedDisk[] disks = showDirectory(config.getDiskDir() +
                "/CPMV233.DSK");
        assertCanReadFiles(disks);
    }

    @Test
    public void testCPAM51BDisk() throws DiskException, IOException {
        FormattedDisk[] disks = showDirectory(config.getDiskDir() +
                "/CPAM51B.dsk");
        assertCanReadFiles(disks);
    }

    @Test
    public void testOriginal32SystemMasterStd() throws DiskException, IOException {
        FormattedDisk[] disks = showDirectory(config.getDiskDir() +
                "/original32sysmasstd.d13");
        assertCanReadFiles(disks);
    }

	protected FormattedDisk[] showDirectory(String imageName) throws IOException, DiskException {
        Source source = Sources.create(imageName).orElseThrow();
        DiskFactory.Context ctx = Disks.inspect(source);
        if (ctx.disks.isEmpty()) {
            throw new DiskUnrecognizedException("no disks discovered for: " + imageName);
        }
        for (FormattedDisk formattedDisk : ctx.disks) {
			System.out.println();
			System.out.println(formattedDisk.getDiskName());
			List<FileEntry> files = formattedDisk.getFiles();
			if (files != null) {
				showFiles(files, ""); //$NON-NLS-1$
			}
			System.out.println(formattedDisk.getFreeSpace() + " bytes free."); //$NON-NLS-1$
			System.out.println(formattedDisk.getUsedSpace() + " bytes used."); //$NON-NLS-1$
			System.out.println("This disk " + (formattedDisk.canHaveDirectories() ? "does" : "does not") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				" support directories."); //$NON-NLS-1$
			System.out.println("This disk is formatted in the " + formattedDisk.getFormat() + " format."); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println();
			
			showDiskUsage(formattedDisk);
		}
		return ctx.disks.toArray(new FormattedDisk[0]);
	}
	
	protected void showFiles(List<FileEntry> files, String indent) throws DiskException {
		for (int i=0; i<files.size(); i++) {
			FileEntry entry = files.get(i);
			if (!entry.isDeleted()) {
				List<String> data = entry.getFileColumnData(FormattedDisk.FILE_DISPLAY_NATIVE);
				System.out.print(indent);
				for (int d=0; d<data.size(); d++) {
					System.out.print(data.get(d));
					System.out.print(" "); //$NON-NLS-1$
				}
				System.out.println();
			}
			if (entry.isDirectory()) {
				showFiles(((DirectoryEntry)entry).getFiles(), indent + "  "); //$NON-NLS-1$
			}
		}
	}
	
	protected void showDiskUsage(FormattedDisk disk) {
		int[] dimensions = disk.getBitmapDimensions();
		DiskUsage usage = disk.getDiskUsage();
		if (usage == null) {
			System.out.println("A bitmap is not available."); //$NON-NLS-1$
			return;
		}
		if (dimensions == null) {
			int i=0;
			while (usage.hasNext()) {
				if (i > 0 && i % 80 == 0) System.out.println();
				usage.next();
				System.out.print(usage.isFree() ? "." : "U"); //$NON-NLS-1$ //$NON-NLS-2$
				i++;
			}
			System.out.println();
		} else {
			for (int y=dimensions[0]-1; y>=0; y--) {
				for (int x=0; x<dimensions[1]; x++) {
					usage.next();
					System.out.print(usage.isFree() ? "." : "U"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				System.out.println();
			}
		}
		System.out.println("U = used, . = free"); //$NON-NLS-1$
	}
	
	protected void assertApplesoftFile(FormattedDisk disk, String filename) throws DiskException {
		assertNotNull(disk, filename + " test: Disk should not be null");
		FileEntry fileEntry = disk.getFile(filename);
		assertNotNull(fileEntry, filename + " test: File not found");
        assertInstanceOf(ApplesoftFileFilter.class, fileEntry.getSuggestedFilter(), "ApplesoftFileFilter was not chosen");
	}

	protected void assertIntegerFile(FormattedDisk disk, String filename) throws DiskException {
		assertNotNull(disk, filename + " test: Disk should not be null");
		FileEntry fileEntry = disk.getFile(filename);
		assertNotNull(fileEntry, filename + " test: File not found");
        assertInstanceOf(IntegerBasicFileFilter.class, fileEntry.getSuggestedFilter(), "IntegerBasicFileFilter was not chosen");
	}
	
	protected void assertTextFile(FormattedDisk disk, String filename) throws DiskException {
		assertNotNull(disk, filename + " test: Disk should not be null");
		FileEntry fileEntry = disk.getFile(filename);
		assertNotNull(fileEntry, filename + " test: File not found");
        assertInstanceOf(TextFileFilter.class, fileEntry.getSuggestedFilter(), "TextFileFilter was not chosen");
	}
	
	protected void assertBinaryFile(FormattedDisk disk, String filename) throws DiskException {
		assertNotNull(disk, filename + " test: Disk should not be null");
		FileEntry fileEntry = disk.getFile(filename);
		assertNotNull(fileEntry, filename + " test: File not found");
        assertInstanceOf(BinaryFileFilter.class, fileEntry.getSuggestedFilter(), "BinaryFileFilter was not chosen");
	}
	
	protected void assertDisassemblyFile(FormattedDisk disk, String filename) throws DiskException {
        assertNotNull(disk, filename + " test: Disk should not be null");
        FileEntry fileEntry = disk.getFile(filename);
        assertNotNull(fileEntry, filename + " test: File not found");
        assertInstanceOf(DisassemblyFileFilter.class, fileEntry.getSuggestedFilter(), "DisassemblyFileFilter was not chosen");
	}
	
	protected void assertGraphicsFile(FormattedDisk disk, String filename) throws DiskException {
		assertNotNull(disk, filename + " test: Disk should not be null");
		FileEntry fileEntry = disk.getFile(filename);
		assertNotNull(fileEntry, filename + " test: File not found");
        assertInstanceOf(GraphicsFileFilter.class, fileEntry.getSuggestedFilter(), "GraphicsFileFilter was not chosen");
	}

    protected void assertCanReadFiles(FormattedDisk... disks) throws DiskException {
        for (FormattedDisk disk : disks) assertCanReadFiles((DirectoryEntry) disk);
    }

	protected void assertCanReadFiles(DirectoryEntry dir) throws DiskException {
		for (FileEntry file : dir.getFiles()) {
			if (file instanceof DirectoryEntry) {
				assertCanReadFiles((DirectoryEntry) file);
			}
            else if (file.isDeleted()) {
                System.out.printf("Skipping deleted file: %s\n", file.getFilename());
            } else {
				try {
					byte[] data = file.getFileData();
					assertNotNull(data);
				} catch (Exception e) {
					throw new AssertionError(String.format("Unable to read file '%s'", file.getFilename()), e);
				}
			}
		}
	}
}
