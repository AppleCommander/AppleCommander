/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002 by Robert Greene
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
package com.webcodepro.applecommander.test;

import com.webcodepro.applecommander.storage.ApplesoftFileFilter;
import com.webcodepro.applecommander.storage.BinaryFileFilter;
import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.GraphicsFileFilter;
import com.webcodepro.applecommander.storage.IntegerBasicFileFilter;
import com.webcodepro.applecommander.storage.TextFileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk.DiskUsage;

import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

/**
 * Test Disk and FormattedDisk for read.
 * <p>
 * Date created: Oct 3, 2002 11:35:26 PM
 * @author: Rob Greene
 */
public class DiskHelperTest extends TestCase {

	public DiskHelperTest(String name) {
		super(name);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(DiskHelperTest.class);
	}

	public void testLoadDos33() throws IOException {
		FormattedDisk[] disks = showDirectory("C:/My Apple2/Disks/DOS 3.3.po");
		assertApplesoftFile(disks[0], "HELLO");
		assertIntegerFile(disks[0], "ANIMALS");
		assertTextFile(disks[0], "APPLE PROMS");
		assertBinaryFile(disks[0], "BOOT13");
	}

	public void testLoadMaster() throws IOException {
		showDirectory("C:/My Apple2/Disks/MASTER.DSK");
	}
	
	public void testLoadGalacticAttack1() throws IOException {
		showDirectory("C:/My Apple2/Disks/galatt.dsk");
	}
	
	public void testLoadProdos() throws IOException {
		FormattedDisk[] disks = showDirectory("C:/My Apple2/Disks/Prodos.dsk");
		assertApplesoftFile(disks[0], "COPY.ME");
		assertBinaryFile(disks[0], "SETTINGS");
		assertBinaryFile(disks[0], "PRODOS");
	}
	
	public void testLoadMarbleMadness() throws IOException {
		showDirectory("C:/My Apple2/Disks/Marble Madness (1985)(Electronic Arts).2mg");
	}
	
	public void testLoadHd1() throws IOException {
		showDirectory("C:/My Apple2/ApplePC/hd1.hdv");
	}
	
	public void testRdosBoot() throws IOException {
		showDirectory("C:/My Apple2/Disks/RDOSboot.dsk");
	}

	public void testSsiSave() throws IOException {
		showDirectory("C:/My Apple2/Disks/SSIsave.dsk");
	}

	public void testPhan2d1() throws IOException {
		FormattedDisk[] disks = showDirectory("C:/My Apple2/Disks/phan2d1.dsk");
		assertApplesoftFile(disks[0], "PHANTASIE II");
		assertBinaryFile(disks[0], "TWN21");
		assertTextFile(disks[0], "ITEM");
		assertGraphicsFile(disks[0], "ICE DRAGON");
	}

	public void testPhan2d2() throws IOException {
		showDirectory("C:/My Apple2/Disks/phan2d2.dsk");
	}

	public void testPhantasie1() throws IOException {
		showDirectory("C:/My Apple2/Disks/Phantasie1.dsk");
	}

	public void testPhantasie2() throws IOException {
		showDirectory("C:/My Apple2/Disks/Phantasie2.dsk");
	}

	public void testCavernsOfFreitag() throws IOException {
		FormattedDisk[] disks = showDirectory("C:/My Apple2/Disks/CavernsOfFreitag.dsk");
		assertGraphicsFile(disks[0], "TITLE.PIC");
	}
	
	public void testUniDosD3110() throws IOException {
		showDirectory("C:/My Apple2/Disks/UniDOS/D3110.dsk");
	}

	public void testUniDosD3151() throws IOException {
		showDirectory("C:/My Apple2/Disks/UniDOS/D3151.dsk");
	}
	
	protected FormattedDisk[] showDirectory(String imageName) throws IOException {
		Disk disk = new Disk(imageName);
		FormattedDisk[] formattedDisks = disk.getFormattedDisks();
		for (int i=0; i<formattedDisks.length; i++) {
			FormattedDisk formattedDisk = formattedDisks[i];
			System.out.println();
			System.out.println(formattedDisk.getDiskName());
			List files = formattedDisk.getFiles();
			if (files != null) {
				showFiles(files, "");
			}
			System.out.println(formattedDisk.getFreeSpace() + " bytes free.");
			System.out.println(formattedDisk.getUsedSpace() + " bytes used.");
			System.out.println("This disk " + (formattedDisk.canHaveDirectories() ? "does" : "does not") +
				" support directories.");
			System.out.println("This disk is formatted in the " + formattedDisk.getFormat() + " format.");
			System.out.println();
			
			showDiskUsage(formattedDisk);
		}
		return formattedDisks;
	}
	
	protected void showFiles(List files, String indent) {
		for (int i=0; i<files.size(); i++) {
			FileEntry entry = (FileEntry) files.get(i);
			if (!entry.isDeleted()) {
				List data = entry.getFileColumnData(FormattedDisk.FILE_DISPLAY_NATIVE);
				System.out.print(indent);
				for (int d=0; d<data.size(); d++) {
					System.out.print(data.get(d));
					System.out.print(" ");
				}
				System.out.println();
			}
			if (entry.isDirectory()) {
				showFiles(entry.getFiles(), indent + "  ");
			}
		}
	}
	
	protected void showDiskUsage(FormattedDisk disk) {
		int[] dimensions = disk.getBitmapDimensions();
		DiskUsage usage = disk.getDiskUsage();
		if (usage == null) {
			System.out.println("A bitmap is not available.");
			return;
		}
		if (dimensions == null) {
			int i=0;
			while (usage.hasNext()) {
				if (i > 0 && i % 80 == 0) System.out.println();
				usage.next();
				System.out.print(usage.isFree() ? "." : "U");
				i++;
			}
			System.out.println();
		} else {
			for (int y=dimensions[0]-1; y>=0; y--) {
				for (int x=0; x<dimensions[1]; x++) {
					usage.next();
					System.out.print(usage.isFree() ? "." : "U");
				}
				System.out.println();
			}
		}
		System.out.println("U = used, . = free");
	}
	
	protected void assertApplesoftFile(FormattedDisk disk, String filename) {
		assertNotNull(filename + " test: Disk should not be null", disk);
		FileEntry fileEntry = disk.getFile(filename);
		assertNotNull(filename + " test: File not found", disk);
		assertTrue("ApplesoftFileFilter was not chosen", 
			fileEntry.getSuggestedFilter() instanceof ApplesoftFileFilter);
	}

	protected void assertIntegerFile(FormattedDisk disk, String filename) {
		assertNotNull(filename + " test: Disk should not be null", disk);
		FileEntry fileEntry = disk.getFile(filename);
		assertNotNull(filename + " test: File not found", disk);
		assertTrue("IntegerBasicFileFilter was not chosen", 
			fileEntry.getSuggestedFilter() instanceof IntegerBasicFileFilter);
	}
	
	protected void assertTextFile(FormattedDisk disk, String filename) {
		assertNotNull(filename + " test: Disk should not be null", disk);
		FileEntry fileEntry = disk.getFile(filename);
		assertNotNull(filename + " test: File not found", disk);
		assertTrue("TextFileFilter was not chosen", 
			fileEntry.getSuggestedFilter() instanceof TextFileFilter);
	}
	
	protected void assertBinaryFile(FormattedDisk disk, String filename) {
		assertNotNull(filename + " test: Disk should not be null", disk);
		FileEntry fileEntry = disk.getFile(filename);
		assertNotNull(filename + " test: File not found", disk);
		assertTrue("BinaryFileFilter was not chosen", 
			fileEntry.getSuggestedFilter() instanceof BinaryFileFilter);
	}
	
	protected void assertGraphicsFile(FormattedDisk disk, String filename) {
		assertNotNull(filename + " test: Disk should not be null", disk);
		FileEntry fileEntry = disk.getFile(filename);
		assertNotNull(filename + " test: File not found", disk);
		assertTrue("GraphicsFileFilter was not chosen", 
			fileEntry.getSuggestedFilter() instanceof GraphicsFileFilter);
	}
}
