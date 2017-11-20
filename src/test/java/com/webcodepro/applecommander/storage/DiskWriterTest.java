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
package com.webcodepro.applecommander.storage;

import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import com.webcodepro.applecommander.storage.FormattedDisk.DiskUsage;
import com.webcodepro.applecommander.storage.os.dos33.DosFormatDisk;
import com.webcodepro.applecommander.storage.os.dos33.OzDosFormatDisk;
import com.webcodepro.applecommander.storage.os.dos33.UniDosFormatDisk;
import com.webcodepro.applecommander.storage.os.pascal.PascalFormatDisk;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;
import com.webcodepro.applecommander.storage.physical.ByteArrayImageLayout;
import com.webcodepro.applecommander.storage.physical.DosOrder;
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import com.webcodepro.applecommander.storage.physical.NibbleOrder;
import com.webcodepro.applecommander.storage.physical.ProdosOrder;

/**
 * Test Disk and FormattedDisk for write.
 * <p>
 * Date created: Oct 3, 2002 11:35:26 PM
 * @author Rob Greene
 */
public class DiskWriterTest extends TestCase {
	/**
	 * Determine if the created disk images should be saved for later
	 * perusal.
	 */
	private boolean saveImage = false;

	/**
	 * Create the DiskWriterTest.
	 */
	public DiskWriterTest(String name) {
		super(name);
	}

	/**
	 * Run the test in text mode.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(DiskWriterTest.class);
	}

	/**
	 * Test writing and reading random files to a DOS 3.3 140K disk.
	 */
	public void testWriteToDos33() throws DiskFullException, IOException {
		ByteArrayImageLayout imageLayout = new ByteArrayImageLayout(Disk.APPLE_140KB_DISK);
		ImageOrder imageOrder = new DosOrder(imageLayout);
		FormattedDisk[] disks = DosFormatDisk.create("write-test-dos33.dsk", imageOrder); //$NON-NLS-1$
		writeFiles(disks, "B", "T", false); //$NON-NLS-1$ //$NON-NLS-2$
		saveDisks(disks);
	}

	/**
	 * Test writing and reading random files to a DOS 3.3 140K nibbilized disk.
	 */
	public void testWriteToDos33Nibble() throws DiskFullException, IOException {
		ByteArrayImageLayout imageLayout = new ByteArrayImageLayout(Disk.APPLE_140KB_NIBBLE_DISK);
		ImageOrder imageOrder = new NibbleOrder(imageLayout);
		FormattedDisk[] disks = DosFormatDisk.create("write-test-dos33.nib", imageOrder); //$NON-NLS-1$
		writeFiles(disks, "B", "T", false); //$NON-NLS-1$ //$NON-NLS-2$
		saveDisks(disks);
	}

	/**
	 * Test writing and reading random files to a ProDOS 140K disk.
	 */	
	public void testWriteToPascal140kDisk() throws DiskFullException, IOException {
		ByteArrayImageLayout imageLayout = new ByteArrayImageLayout(Disk.APPLE_140KB_DISK);
		ImageOrder imageOrder = new ProdosOrder(imageLayout);
		FormattedDisk[] disks = PascalFormatDisk.create(
			"write-test-pascal-140k.po", "TEST", imageOrder); //$NON-NLS-1$ //$NON-NLS-2$
		writeFiles(disks, "code", "text", false); //$NON-NLS-1$ //$NON-NLS-2$
		saveDisks(disks);
	}

	/**
	 * Test writing and reading random files to a ProDOS 140K disk.
	 */	
	public void testWriteToPascal800kDisk() throws DiskFullException, IOException {
		ByteArrayImageLayout imageLayout = new ByteArrayImageLayout(Disk.APPLE_800KB_DISK);
		ImageOrder imageOrder = new ProdosOrder(imageLayout);
		FormattedDisk[] disks = PascalFormatDisk.create(
			"write-test-pascal-800k.po", "TEST", imageOrder); //$NON-NLS-1$ //$NON-NLS-2$
		//writeFiles(disks, "code", "text", false); //$NON-NLS-1$ //$NON-NLS-2$
		saveDisks(disks);
	}

	/**
	 * Test writing and reading random files to a ProDOS 140K disk.
	 */	
	public void testWriteToProdos140kDisk() throws DiskFullException, IOException {
		ByteArrayImageLayout imageLayout = new ByteArrayImageLayout(Disk.APPLE_140KB_DISK);
		ImageOrder imageOrder = new ProdosOrder(imageLayout);
		FormattedDisk[] disks = ProdosFormatDisk.create(
			"write-test-prodos-140k.dsk", "TEST", imageOrder); //$NON-NLS-1$ //$NON-NLS-2$
		writeFiles(disks, "BIN", "TXT", true); //$NON-NLS-1$ //$NON-NLS-2$
		saveDisks(disks);
	}

	/**
	 * Test writing and reading random files to a ProDOS 800K disk.
	 */	
	public void testWriteToProdos800kDisk() throws DiskFullException, IOException {
		ByteArrayImageLayout imageLayout = new ByteArrayImageLayout(Disk.APPLE_800KB_DISK);
		ImageOrder imageOrder = new ProdosOrder(imageLayout);
		FormattedDisk[] disks = ProdosFormatDisk.create(
			"write-test-prodos-800k.po", "TEST", imageOrder); //$NON-NLS-1$ //$NON-NLS-2$
		writeFiles(disks, "BIN", "TXT", true); //$NON-NLS-1$ //$NON-NLS-2$
		saveDisks(disks);
	}

	/**
	 * Test writing and reading random files to a ProDOS 5MB disk.
	 */	
	public void testWriteToProdos5mbDisk() throws DiskFullException, IOException {
		ByteArrayImageLayout imageLayout = new ByteArrayImageLayout(Disk.APPLE_5MB_HARDDISK);
		ImageOrder imageOrder = new ProdosOrder(imageLayout);
		FormattedDisk[] disks = ProdosFormatDisk.create(
			"write-test-prodos-5mb.hdv", "TEST", imageOrder); //$NON-NLS-1$ //$NON-NLS-2$
		writeFiles(disks, "BIN", "TXT", true); //$NON-NLS-1$ //$NON-NLS-2$
		saveDisks(disks);
	}
	
	/**
	 * Test creating and deleting many files on a DOS 3.3 140K disk.
	 */
	public void testCreateAndDeleteDos33() throws IOException {
		ByteArrayImageLayout imageLayout = new ByteArrayImageLayout(Disk.APPLE_140KB_DISK);
		ImageOrder imageOrder = new DosOrder(imageLayout);
		FormattedDisk[] disks = DosFormatDisk.create(
			"createanddelete-test-dos33.dsk", imageOrder); //$NON-NLS-1$
		createAndDeleteFiles(disks, "B"); //$NON-NLS-1$
		saveDisks(disks);
	}

	/**
	 * Test creating and deleting many files on an OzDOS 800K disk.
	 */
	public void testCreateAndDeleteOzDos() throws IOException {
		ByteArrayImageLayout imageLayout = new ByteArrayImageLayout(Disk.APPLE_800KB_DISK);
		ImageOrder imageOrder = new ProdosOrder(imageLayout);
		FormattedDisk[] disks = OzDosFormatDisk.create(
			"createanddelete-test-ozdos.po", imageOrder); //$NON-NLS-1$
		createAndDeleteFiles(disks, "B"); //$NON-NLS-1$
		saveDisks(disks);
	}

	/**
	 * Test creating and deleting many files on a UniDOS 800K disk.
	 */
	public void testCreateAndDeleteUniDos() throws IOException {
		ByteArrayImageLayout imageLayout = new ByteArrayImageLayout(Disk.APPLE_800KB_DISK);
		ImageOrder imageOrder = new DosOrder(imageLayout);
		FormattedDisk[] disks = UniDosFormatDisk.create(
			"createanddelete-test-unidos.dsk", imageOrder); //$NON-NLS-1$
		createAndDeleteFiles(disks, "B"); //$NON-NLS-1$
		saveDisks(disks);
	}

	/**
	 * Test creating and deleting many files on a Pascal 140K disk.
	 */
	public void testCreateAndDeletePascal140kDisk() throws IOException {
		ByteArrayImageLayout imageLayout = new ByteArrayImageLayout(Disk.APPLE_140KB_DISK);
		ImageOrder imageOrder = new ProdosOrder(imageLayout);
		FormattedDisk[] disks = PascalFormatDisk.create(
			"createanddelete-test-pascal-140k.po", "TEST",  //$NON-NLS-1$ //$NON-NLS-2$
			imageOrder);
		createAndDeleteFiles(disks, "CODE"); //$NON-NLS-1$
		saveDisks(disks);
	}

	/**
	 * Test creating and deleting many files on a Pascal 800K disk.
	 */
	public void testCreateAndDeletePascal800kDisk() throws IOException {
		ByteArrayImageLayout imageLayout = new ByteArrayImageLayout(Disk.APPLE_800KB_DISK);
		ImageOrder imageOrder = new ProdosOrder(imageLayout);
		FormattedDisk[] disks = PascalFormatDisk.create(
			"createanddelete-test-pascal-800k.po", "TEST",  //$NON-NLS-1$ //$NON-NLS-2$
			imageOrder);
		createAndDeleteFiles(disks, "CODE"); //$NON-NLS-1$
		saveDisks(disks);
	}

	/**
	 * Test creating and deleting many files on a ProDOS 140K disk.
	 */
	public void testCreateAndDeleteProdos140kDisk() throws IOException {
		ByteArrayImageLayout imageLayout = new ByteArrayImageLayout(Disk.APPLE_140KB_DISK);
		ImageOrder imageOrder = new DosOrder(imageLayout);
		FormattedDisk[] disks = ProdosFormatDisk.create(
			"createanddelete-test-prodos-140k.dsk", "TEST",  //$NON-NLS-1$ //$NON-NLS-2$
			imageOrder);
		createAndDeleteFiles(disks, "BIN"); //$NON-NLS-1$
		saveDisks(disks);
	}

	/**
	 * Test creating and deleting many files on a ProDOS 800K disk.
	 */
	public void testCreateAndDeleteProdos800kDisk() throws IOException {
		ByteArrayImageLayout imageLayout = new ByteArrayImageLayout(Disk.APPLE_800KB_DISK);
		ImageOrder imageOrder = new ProdosOrder(imageLayout);
		FormattedDisk[] disks = ProdosFormatDisk.create(
			"createanddelete-test-prodos-800k.po", "TEST", //$NON-NLS-1$ //$NON-NLS-2$
			imageOrder);
		createAndDeleteFiles(disks, "BIN"); //$NON-NLS-1$
		saveDisks(disks);
	}
	
	/**
	 * Test creating, deleting, and then creating another file which re-uses
	 * the old directory entry on a DOS 3.3 140K disk.
	 */
	public void testCreateDeleteCreateDosDisk() 
	throws DiskFullException, IOException {
		ByteArrayImageLayout imageLayout = new ByteArrayImageLayout(Disk.APPLE_140KB_DISK);
		ImageOrder imageOrder = new DosOrder(imageLayout);
		FormattedDisk[] disks = DosFormatDisk.create(
			"createdeletecreate-test-dos-140k.dsk", imageOrder); //$NON-NLS-1$
		createDeleteCreate(disks, "B"); //$NON-NLS-1$
		saveDisks(disks);
	}

	/**
	 * Test creating, deleting, and then creating another file which re-uses
	 * the old directory entry on a OzDOS 800K disk.
	 */
	public void testCreateDeleteCreateOzdosDisk() 
	throws DiskFullException, IOException {
		ByteArrayImageLayout imageLayout = new ByteArrayImageLayout(Disk.APPLE_800KB_DISK);
		ImageOrder imageOrder = new ProdosOrder(imageLayout);
		FormattedDisk[] disks = OzDosFormatDisk.create(
			"createdeletecreate-test-ozdos-800k.po", imageOrder); //$NON-NLS-1$
		createDeleteCreate(disks, "B"); //$NON-NLS-1$
		saveDisks(disks);
	}

	/**
	 * Test creating, deleting, and then creating another file which re-uses
	 * the old directory entry on a UniDOS 800K disk.
	 */
	public void testCreateDeleteCreateUnidosDisk() 
	throws DiskFullException, IOException {
		ByteArrayImageLayout imageLayout = new ByteArrayImageLayout(Disk.APPLE_800KB_DISK);
		ImageOrder imageOrder = new DosOrder(imageLayout);
		FormattedDisk[] disks = UniDosFormatDisk.create(
			"createdeletecreate-test-unidos-800k.dsk", imageOrder); //$NON-NLS-1$
		createDeleteCreate(disks, "B"); //$NON-NLS-1$
		saveDisks(disks);
	}

	/**
	 * Test creating, deleting, and then creating another file which re-uses
	 * the old directory entry on a Pascal 140K disk.
	 */
	public void testCreateDeleteCreatePascalDisk() 
	throws DiskFullException, IOException {
		ByteArrayImageLayout imageLayout = new ByteArrayImageLayout(Disk.APPLE_140KB_DISK);
		ImageOrder imageOrder = new ProdosOrder(imageLayout);
		FormattedDisk[] disks = PascalFormatDisk.create(
			"createdeletecreate-test-pascal-140k.po", "TEST", //$NON-NLS-1$ //$NON-NLS-2$
			imageOrder);
		createDeleteCreate(disks, "CODE"); //$NON-NLS-1$
		saveDisks(disks);
	}
	
	/**
	 * Test creating, deleting, and then creating another file which re-uses
	 * the old directory entry on a ProDOS 140K disk.
	 */
	public void testCreateDeleteCreateProdosDisk() 
	throws DiskFullException, IOException {
		ByteArrayImageLayout imageLayout = new ByteArrayImageLayout(Disk.APPLE_140KB_DISK);
		ImageOrder imageOrder = new ProdosOrder(imageLayout);
		FormattedDisk[] disks = ProdosFormatDisk.create(
			"createdeletecreate-test-prodos-140k.dsk", "TEST", //$NON-NLS-1$ //$NON-NLS-2$
			imageOrder);
		createDeleteCreate(disks, "BIN"); //$NON-NLS-1$
		saveDisks(disks);
	}
	
	/**
	 * Write many files to disk, read from disk, and verify contents.
	 * The intention is to verify creating files is done correctly,
	 * writing of contents is done correctly (the files are a series of
	 * random bytes), and reading of files is done correctly.
	 */
	protected void writeFiles(FormattedDisk[] disks, String binaryType, 
		String textType, boolean testText) throws DiskFullException {
		FormattedDisk disk = disks[0];
		showDirectory(disks, "BEFORE FILE CREATION"); //$NON-NLS-1$
		writeFile(disk, 1, binaryType, true);
		writeFile(disk, 2, binaryType, true);
		writeFile(disk, 4, binaryType, true);
		writeFile(disk, 8, binaryType, true);
		writeFile(disk, 16, binaryType, true);
		writeFile(disk, 256, binaryType, true);
		writeFile(disk, 512, binaryType, true);
		writeFile(disk, 1234, binaryType, true);
		writeFile(disk, 54321, binaryType, true);
		writeFile(disk, 
			"This is a test text file create from the DiskWriterTest".getBytes(),  //$NON-NLS-1$
			textType, testText);
		if (disk.getPhysicalSize() > Disk.APPLE_140KB_DISK
			&& disk.getPhysicalSize() != Disk.APPLE_140KB_NIBBLE_DISK) {
			// create a few big files
			writeFile(disk, 150000, binaryType, true);
			writeFile(disk, 300000, binaryType, true);
		}
		showDirectory(disks, "AFTER FILE CREATION"); //$NON-NLS-1$
	}
	
	/**
	 * Generate randomized data for writing to disk.
	 */
	protected void writeFile(FormattedDisk disk, int size, String fileType,
		boolean test) throws DiskFullException {
		byte[] data = new byte[size];
		for (int i=0; i<data.length; i++) {
			data[i] = (byte)(Math.random() * 1024);
		}
		writeFile(disk, data, fileType, test);
	}
	
	/**
	 * Create a file, write the file, and if specified, verify that the file
	 * contents match.  The verification is optional because some files,
	 * depending on the operating system format may not come back in the
	 * exact same length.
	 */
	protected void writeFile(FormattedDisk disk, byte[] data, String fileType,
		boolean test) throws DiskFullException {
		FileEntry entry = disk.createFile();
		entry.setFilename("file-" + data.length); //$NON-NLS-1$
		entry.setFiletype(fileType);
		entry.setFileData(data);
		byte[] data2 = entry.getFileData();
		if (test) {
			assertTrue("File lengths do not match", data.length == data2.length); //$NON-NLS-1$
			//assertTrue("File contents do not match", Arrays.equals(data, data2));
			for (int i=0; i<data.length; i++) {
				assertTrue("File contents differ at " + i, data[i] == data2[i]); //$NON-NLS-1$
			}
		}
	}
	
	/**
	 * Display the contents of a directory.
	 */
	protected void showDirectory(FormattedDisk[] formattedDisks, String title) {
		System.out.println();
		System.out.println("************************************************"); //$NON-NLS-1$
		System.out.println(title);
		for (int i=0; i<formattedDisks.length; i++) {
			FormattedDisk formattedDisk = formattedDisks[i];
			System.out.println();
			System.out.println(formattedDisk.getDiskName());
			List files = formattedDisk.getFiles();
			if (files != null) {
				showFiles(files, "", false); //$NON-NLS-1$
			}
			System.out.println(formattedDisk.getFreeSpace() + " bytes free."); //$NON-NLS-1$
			System.out.println(formattedDisk.getUsedSpace() + " bytes used."); //$NON-NLS-1$
			System.out.println("This disk " + (formattedDisk.canHaveDirectories() ? "does" : "does not") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				" support directories."); //$NON-NLS-1$
			System.out.println("This disk is formatted in the " + formattedDisk.getFormat() + " format."); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println();
			
			showDiskUsage(formattedDisk);
		}
		System.out.println();
		System.out.println("************************************************"); //$NON-NLS-1$
		System.out.println();
	}
	
	/**
	 * Display a list of files.
	 */
	protected void showFiles(List files, String indent, boolean showDeleted) {
		for (int i=0; i<files.size(); i++) {
			FileEntry entry = (FileEntry) files.get(i);
			if (showDeleted || !entry.isDeleted()) {
				List data = entry.getFileColumnData(FormattedDisk.FILE_DISPLAY_NATIVE);
				System.out.print(indent);
				for (int d=0; d<data.size(); d++) {
					System.out.print(data.get(d));
					System.out.print(" "); //$NON-NLS-1$
				}
				System.out.println();
			}
			if (entry.isDirectory()) {
				showFiles(((DirectoryEntry)entry).getFiles(), 
					indent + "  ", showDeleted); //$NON-NLS-1$
			}
		}
	}
	
	/**
	 * Draw a disk usage map.
	 */
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
	
	/**
	 * Create a bunch of files and then delete them repeatedly.
	 * This is intended to excersize not only creating and deleting
	 * files but the disk management (ala Disk Map).
	 */
	protected void createAndDeleteFiles(FormattedDisk[] disks, String filetype) {
		byte[] data = new byte[129 * 1024];
		for (int i=0; i<data.length; i++) {
			data[i] = (byte)(Math.random() * 1024);
		}
		for (int d=0; d<disks.length; d++) {
			FormattedDisk disk = disks[d];
			System.out.println("Excercising create and delete on disk "  //$NON-NLS-1$
				+ disk.getDiskName() + " in the " + disk.getFormat()  //$NON-NLS-1$
				+ " format."); //$NON-NLS-1$
			int originalUsed = disk.getUsedSpace();
			int originalFree = disk.getFreeSpace();
			for (int count=0; count<5; count++) {
				// Fill the disk with files:
				try {
					while (true) {
						writeFile(disk, data, filetype, false);
					}
				} catch (DiskFullException ex) {
					// ignored
				}
				// Remove the files:
				List files = disk.getFiles();
				for (int i=0; i<files.size(); i++) {
					FileEntry entry = (FileEntry) files.get(i);
					entry.delete();
				}
				// Verify that we're back to what we started with:
				assertTrue("Free space does not match",  //$NON-NLS-1$
					originalFree == disk.getFreeSpace());
				assertTrue("Used space does not match",  //$NON-NLS-1$
					originalUsed == disk.getUsedSpace());
			}
		}
	}
	
	/**
	 * Test a create file, delete file, create file sequence.
	 * The expected result is that the deleted file entry is reused.
	 */
	protected void createDeleteCreate(FormattedDisk[] disks, String filetype) 
	throws DiskFullException {
		for (int d=0; d<disks.length; d++) {
			FormattedDisk disk = disks[d];
			System.out.println("Exercising create, delete, create sequence " //$NON-NLS-1$
				+ "on disk " + disk.getDiskName() + "."); //$NON-NLS-1$ //$NON-NLS-2$
			writeFile(disk, 5432, filetype, false);
			List files = disk.getFiles();
			for (int i=0; i<files.size(); i++) {
				FileEntry entry = (FileEntry) files.get(i);
				entry.delete();
			}
			writeFile(disk, 1234, filetype, false);
			files = disk.getFiles();
			for (int i=0; i<files.size(); i++) {
				FileEntry entry = (FileEntry) files.get(i);
				if (entry.isDeleted()) {
					showFiles(files, "", true); //$NON-NLS-1$
					fail("There should be no deleted files"); //$NON-NLS-1$
				}
			}
		}
	}

	/**
	 * Save a disk, if the saveImage flag has been set to true.
	 */
	protected void saveDisks(FormattedDisk[] disks) throws IOException {
		if (saveImage) {
			for (int i=0; i<disks.length; i++) {
				disks[i].save();
			}
		}
	}
}
