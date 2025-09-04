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

import java.io.IOException;
import java.util.List;

import org.applecommander.device.nibble.Nibble62Disk525Codec;
import org.applecommander.device.*;
import org.applecommander.device.nibble.DiskMarker;
import org.applecommander.hint.Hint;
import org.applecommander.image.NibbleImage;
import org.applecommander.os.dos.OzdosAdapterStrategy;
import org.applecommander.os.dos.UnidosAdapterStrategy;
import org.applecommander.source.DataBufferSource;
import org.applecommander.source.Source;
import org.junit.jupiter.api.Test;

import com.webcodepro.applecommander.storage.FormattedDisk.DiskUsage;
import com.webcodepro.applecommander.storage.os.dos33.DosFormatDisk;
import com.webcodepro.applecommander.storage.os.pascal.PascalFormatDisk;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test Disk and FormattedDisk for write.
 * <p>
 * Date created: Oct 3, 2002 11:35:26 PM
 * @author Rob Greene
 */
public class DiskWriterTest {
	/**
	 * Determine if the created disk images should be saved for later
	 * perusal.
	 */
	private final boolean saveImage = System.getenv("SAVE_IMAGE") != null;

	/**
	 * Test writing and reading random files to a DOS 3.3 140K disk.
	 */
	@Test
	public void testWriteToDos33() throws IOException, DiskException {
		Source source = DataBufferSource.create(DiskConstants.APPLE_140KB_DISK, "new-disk").get();
        TrackSectorDevice sectorDevice = new DosOrderedTrackSectorDevice(source, Hint.DOS_SECTOR_ORDER);
		FormattedDisk[] disks = DosFormatDisk.create("write-test-dos33.dsk", sectorDevice);
		writeFiles(disks, "B", "T", false);
		saveDisks(disks);
	}

	/**
	 * Test writing and reading random files to a DOS 3.3 140K nibbilized disk.
	 */
	@Test
	public void testWriteToDos33Nibble() throws IOException, DiskException {
		Source source = DataBufferSource.create(DiskConstants.APPLE_140KB_NIBBLE_DISK, "new-disk").get();
        TrackSectorDevice sectorDevice = new TrackSectorNibbleDevice(new NibbleImage(source), DiskMarker.disk525sector16(),
                new Nibble62Disk525Codec(), 16);
		FormattedDisk[] disks = DosFormatDisk.create("write-test-dos33.nib", sectorDevice);
		writeFiles(disks, "B", "T", false);  
		saveDisks(disks);
	}

	/**
	 * Test writing and reading random files to a ProDOS 140K disk.
	 */	
	@Test
	public void testWriteToPascal140kDisk() throws IOException, DiskException {
		Source source = DataBufferSource.create(DiskConstants.APPLE_140KB_DISK, "new-disk").get();
		BlockDevice blockDevice = new ProdosOrderedBlockDevice(source, BlockDevice.STANDARD_BLOCK_SIZE);
		FormattedDisk[] disks = PascalFormatDisk.create(
			"write-test-pascal-140k.po", "TEST", blockDevice);  
		writeFiles(disks, "code", "text", false);  
		saveDisks(disks);
	}

	/**
	 * Test writing and reading random files to a ProDOS 140K disk.
	 */	
	@Test
	public void testWriteToPascal800kDisk() throws DiskFullException, IOException {
		Source source = DataBufferSource.create(DiskConstants.APPLE_800KB_DISK, "new-disk").get();
		BlockDevice blockDevice = new ProdosOrderedBlockDevice(source, BlockDevice.STANDARD_BLOCK_SIZE);
		FormattedDisk[] disks = PascalFormatDisk.create(
			"write-test-pascal-800k.po", "TEST", blockDevice);  
		//writeFiles(disks, "code", "text", false);  
		saveDisks(disks);
	}

	/**
	 * Test writing and reading random files to a ProDOS 140K disk.
	 */	
	@Test
	public void testWriteToProdos140kDisk() throws IOException, DiskException {
		Source source = DataBufferSource.create(DiskConstants.APPLE_140KB_DISK, "new-disk").get();
		TrackSectorDevice trackSectorDevice = new DosOrderedTrackSectorDevice(source, Hint.DOS_SECTOR_ORDER);
        TrackSectorDevice skewedDevice = SkewedTrackSectorDevice.dosToPascalSkew(trackSectorDevice);
		BlockDevice blockDevice = new TrackSectorToBlockAdapter(skewedDevice, TrackSectorToBlockAdapter.BlockStyle.PRODOS);
		FormattedDisk[] disks = ProdosFormatDisk.create(
			"write-test-prodos-140k.dsk", "TEST", blockDevice);  
		writeFiles(disks, "BIN", "TXT", true);  
		saveDisks(disks);
	}

	/**
	 * Test writing and reading random files to a ProDOS 800K disk.
	 */	
	@Test
	public void testWriteToProdos800kDisk() throws IOException, DiskException {
		Source source = DataBufferSource.create(DiskConstants.APPLE_800KB_DISK, "new-disk").get();
		BlockDevice blockDevice = new ProdosOrderedBlockDevice(source, BlockDevice.STANDARD_BLOCK_SIZE);
		FormattedDisk[] disks = ProdosFormatDisk.create(
			"write-test-prodos-800k.po", "TEST", blockDevice);  
		writeFiles(disks, "BIN", "TXT", true);  
		saveDisks(disks);
	}

	/**
	 * Test writing and reading random files to a ProDOS 5MB disk.
	 */	
	@Test
	public void testWriteToProdos5mbDisk() throws IOException, DiskException {
		Source source = DataBufferSource.create(DiskConstants.APPLE_5MB_HARDDISK, "new-disk").get();
		BlockDevice blockDevice = new ProdosOrderedBlockDevice(source, BlockDevice.STANDARD_BLOCK_SIZE);
		FormattedDisk[] disks = ProdosFormatDisk.create(
			"write-test-prodos-5mb.hdv", "TEST", blockDevice);  
		writeFiles(disks, "BIN", "TXT", true);  
		saveDisks(disks);
	}
	
	/**
	 * Test creating and deleting many files on a DOS 3.3 140K disk.
	 */
	@Test
	public void testCreateAndDeleteDos33() throws IOException, DiskException {
		Source source = DataBufferSource.create(DiskConstants.APPLE_140KB_DISK, "new-disk").get();
        TrackSectorDevice sectorDevice = new DosOrderedTrackSectorDevice(source, Hint.DOS_SECTOR_ORDER);
		FormattedDisk[] disks = DosFormatDisk.create("createanddelete-test-dos33.dsk", sectorDevice);
		createAndDeleteFiles(disks, "B"); 
		saveDisks(disks);
	}

	/**
	 * Test creating and deleting many files on an OzDOS 800K disk.
	 */
	@Test
	public void testCreateAndDeleteOzDos() throws IOException, DiskException {
		Source source = DataBufferSource.create(DiskConstants.APPLE_800KB_DISK, "new-disk").get();
        BlockDevice blockDevice = new ProdosOrderedBlockDevice(source, BlockDevice.STANDARD_BLOCK_SIZE);
		FormattedDisk[] disks = DosFormatDisk.create("createanddelete-test-ozdos.po", blockDevice,
            OzdosAdapterStrategy.values());
		createAndDeleteFiles(disks, "B"); 
		saveDisks(disks);
	}

	/**
	 * Test creating and deleting many files on a UniDOS 800K disk.
	 */
	@Test
	public void testCreateAndDeleteUniDos() throws IOException, DiskException {
		Source source = DataBufferSource.create(DiskConstants.APPLE_800KB_DISK, "new-disk").get();
        BlockDevice blockDevice = new ProdosOrderedBlockDevice(source, BlockDevice.STANDARD_BLOCK_SIZE);
		FormattedDisk[] disks = DosFormatDisk.create("createanddelete-test-unidos.dsk", blockDevice,
            UnidosAdapterStrategy.values());
		createAndDeleteFiles(disks, "B"); 
		saveDisks(disks);
	}

	/**
	 * Test creating and deleting many files on a Pascal 140K disk.
	 */
	@Test
	public void testCreateAndDeletePascal140kDisk() throws IOException, DiskException {
		Source source = DataBufferSource.create(DiskConstants.APPLE_140KB_DISK, "new-disk").get();
		BlockDevice blockDevice = new ProdosOrderedBlockDevice(source, BlockDevice.STANDARD_BLOCK_SIZE);
		FormattedDisk[] disks = PascalFormatDisk.create(
			"createanddelete-test-pascal-140k.po", "TEST",   
			blockDevice);
		createAndDeleteFiles(disks, "CODE"); 
		saveDisks(disks);
	}

	/**
	 * Test creating and deleting many files on a Pascal 800K disk.
	 */
	@Test
	public void testCreateAndDeletePascal800kDisk() throws IOException, DiskException {
		Source source = DataBufferSource.create(DiskConstants.APPLE_800KB_DISK, "new-disk").get();
		BlockDevice blockDevice = new ProdosOrderedBlockDevice(source, BlockDevice.STANDARD_BLOCK_SIZE);
		FormattedDisk[] disks = PascalFormatDisk.create(
			"createanddelete-test-pascal-800k.po", "TEST",   
			blockDevice);
		createAndDeleteFiles(disks, "CODE"); 
		saveDisks(disks);
	}

	/**
	 * Test creating and deleting many files on a ProDOS 140K disk.
	 */
	@Test
	public void testCreateAndDeleteProdos140kDisk() throws IOException, DiskException {
		Source source = DataBufferSource.create(DiskConstants.APPLE_140KB_DISK, "new-disk").get();
		TrackSectorDevice trackSectorDevice = new DosOrderedTrackSectorDevice(source, Hint.DOS_SECTOR_ORDER);
        TrackSectorDevice skewedDevice = SkewedTrackSectorDevice.dosToPascalSkew(trackSectorDevice);
		BlockDevice blockDevice = new TrackSectorToBlockAdapter(skewedDevice, TrackSectorToBlockAdapter.BlockStyle.PRODOS);
		FormattedDisk[] disks = ProdosFormatDisk.create(
			"createanddelete-test-prodos-140k.dsk", "TEST",   
			blockDevice);
		createAndDeleteFiles(disks, "BIN"); 
		saveDisks(disks);
	}

	/**
	 * Test creating and deleting many files on a ProDOS 800K disk.
	 */
	@Test
	public void testCreateAndDeleteProdos800kDisk() throws IOException, DiskException {
		Source source = DataBufferSource.create(DiskConstants.APPLE_800KB_DISK, "new-disk").get();
		BlockDevice blockDevice = new ProdosOrderedBlockDevice(source, BlockDevice.STANDARD_BLOCK_SIZE);
		FormattedDisk[] disks = ProdosFormatDisk.create(
			"createanddelete-test-prodos-800k.po", "TEST",  
			blockDevice);
		createAndDeleteFiles(disks, "BIN"); 
		saveDisks(disks);
	}
	
	/**
	 * Test creating, deleting, and then creating another file which re-uses
	 * the old directory entry on a DOS 3.3 140K disk.
	 */
	@Test
	public void testCreateDeleteCreateDosDisk() throws IOException, DiskException {
		Source source = DataBufferSource.create(DiskConstants.APPLE_140KB_DISK, "new-disk").get();
        TrackSectorDevice sectorDevice = new DosOrderedTrackSectorDevice(source, Hint.DOS_SECTOR_ORDER);
		FormattedDisk[] disks = DosFormatDisk.create("createdeletecreate-test-dos-140k.dsk", sectorDevice);
		createDeleteCreate(disks, "B"); 
		saveDisks(disks);
	}

	/**
	 * Test creating, deleting, and then creating another file which re-uses
	 * the old directory entry on a OzDOS 800K disk.
	 */
	@Test
	public void testCreateDeleteCreateOzdosDisk() throws IOException, DiskException {
		Source source = DataBufferSource.create(DiskConstants.APPLE_800KB_DISK, "new-disk").get();
        BlockDevice blockDevice = new ProdosOrderedBlockDevice(source, BlockDevice.STANDARD_BLOCK_SIZE);
		FormattedDisk[] disks = DosFormatDisk.create("createdeletecreate-test-ozdos-800k.po", blockDevice,
                OzdosAdapterStrategy.values());
        saveDisks(disks);
		createDeleteCreate(disks, "B");
		saveDisks(disks);
	}

	/**
	 * Test creating, deleting, and then creating another file which re-uses
	 * the old directory entry on a UniDOS 800K disk.
	 */
	@Test
	public void testCreateDeleteCreateUnidosDisk() throws IOException, DiskException {
		Source source = DataBufferSource.create(DiskConstants.APPLE_800KB_DISK, "new-disk").get();
        BlockDevice blockDevice = new ProdosOrderedBlockDevice(source, BlockDevice.STANDARD_BLOCK_SIZE);
		FormattedDisk[] disks = DosFormatDisk.create("createdeletecreate-test-unidos-800k.dsk", blockDevice,
                UnidosAdapterStrategy.values());
        saveDisks(disks);
		createDeleteCreate(disks, "B");
		saveDisks(disks);
	}

	/**
	 * Test creating, deleting, and then creating another file which re-uses
	 * the old directory entry on a Pascal 140K disk.
	 */
	@Test
	public void testCreateDeleteCreatePascalDisk() throws IOException, DiskException {
		Source source = DataBufferSource.create(DiskConstants.APPLE_140KB_DISK, "new-disk").get();
		BlockDevice blockDevice = new ProdosOrderedBlockDevice(source, BlockDevice.STANDARD_BLOCK_SIZE);
		FormattedDisk[] disks = PascalFormatDisk.create(
			"createdeletecreate-test-pascal-140k.po", "TEST",  
			blockDevice);
		createDeleteCreate(disks, "CODE"); 
		saveDisks(disks);
	}
	
	/**
	 * Test creating, deleting, and then creating another file which re-uses
	 * the old directory entry on a ProDOS 140K disk.
	 */
	@Test
	public void testCreateDeleteCreateProdosDisk() throws IOException, DiskException {
		Source source = DataBufferSource.create(DiskConstants.APPLE_140KB_DISK, "new-disk").get();
		BlockDevice blockDevice = new ProdosOrderedBlockDevice(source, BlockDevice.STANDARD_BLOCK_SIZE);
		FormattedDisk[] disks = ProdosFormatDisk.create(
			"createdeletecreate-test-prodos-140k.dsk", "TEST",  
			blockDevice);
		createDeleteCreate(disks, "BIN"); 
		saveDisks(disks);
	}
	
	/**
	 * Write many files to disk, read from disk, and verify contents.
	 * The intention is to verify creating files is done correctly,
	 * writing of contents is done correctly (the files are a series of
	 * random bytes), and reading of files is done correctly.
	 */
	protected void writeFiles(FormattedDisk[] disks, String binaryType, 
		String textType, boolean testText) throws DiskException {
		FormattedDisk disk = disks[0];
		showDirectory(disks, "BEFORE FILE CREATION"); 
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
			"This is a test text file create from the DiskWriterTest".getBytes(),  
			textType, testText);
        Source source = disk.getSource();
		if (source.getSize() > DiskConstants.APPLE_140KB_DISK
			&& source.getSize() != DiskConstants.APPLE_140KB_NIBBLE_DISK) {
			// create a few big files
			writeFile(disk, 150000, binaryType, true);
			writeFile(disk, 300000, binaryType, true);
		}
		showDirectory(disks, "AFTER FILE CREATION"); 
	}
	
	/**
	 * Generate randomized data for writing to disk.
	 */
	protected void writeFile(FormattedDisk disk, int size, String fileType,
		boolean test) throws DiskException {
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
		boolean test) throws DiskException {
		FileEntry entry = disk.createFile();
		entry.setFilename("file-" + data.length); 
		entry.setFiletype(fileType);
		entry.setFileData(data);
		byte[] data2 = entry.getFileData();
		if (test) {
            assertEquals(data.length, data2.length, "File lengths do not match");
			//assertTrue("File contents do not match", Arrays.equals(data, data2));
			for (int i=0; i<data.length; i++) {
                assertEquals(data[i], data2[i], "File contents differ at " + i);
			}
		}
	}
	
	/**
	 * Display the contents of a directory.
	 */
	protected void showDirectory(FormattedDisk[] formattedDisks, String title) throws DiskException {
		System.out.println();
		System.out.println("************************************************"); 
		System.out.println(title);
        for (FormattedDisk formattedDisk : formattedDisks) {
            System.out.println();
            System.out.println(formattedDisk.getDiskName());
            List<FileEntry> files = formattedDisk.getFiles();
            if (files != null) {
                showFiles(files, "", false); 
            }
            System.out.println(formattedDisk.getFreeSpace() + " bytes free."); 
            System.out.println(formattedDisk.getUsedSpace() + " bytes used."); 
            System.out.println("This disk " + (formattedDisk.canHaveDirectories() ? "does" : "does not") +   
                    " support directories."); 
            System.out.println("This disk is formatted in the " + formattedDisk.getFormat() + " format.");  
            System.out.println();

            showDiskUsage(formattedDisk);
        }
		System.out.println();
		System.out.println("************************************************"); 
		System.out.println();
	}
	
	/**
	 * Display a list of files.
	 */
	protected void showFiles(List<FileEntry> files, String indent, boolean showDeleted) throws DiskException {
		for (int i=0; i<files.size(); i++) {
			FileEntry entry = (FileEntry) files.get(i);
			if (showDeleted || !entry.isDeleted()) {
				List<String> data = entry.getFileColumnData(FormattedDisk.FILE_DISPLAY_NATIVE);
				System.out.print(indent);
				for (int d=0; d<data.size(); d++) {
					System.out.print(data.get(d));
					System.out.print(" "); 
				}
				System.out.println();
			}
			if (entry.isDirectory()) {
				showFiles(((DirectoryEntry)entry).getFiles(), 
					indent + "  ", showDeleted); 
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
	
	/**
	 * Create a bunch of files and then delete them repeatedly.
	 * This is intended to exercise not only creating and deleting
	 * files but the disk management (ala Disk Map).
	 */
	protected void createAndDeleteFiles(FormattedDisk[] disks, String filetype) throws DiskException {
		byte[] data = new byte[129 * 1024];
		for (int i=0; i<data.length; i++) {
			data[i] = (byte)(Math.random() * 1024);
		}
		for (int d=0; d<disks.length; d++) {
			FormattedDisk disk = disks[d];
			System.out.println("Exercising create and delete on disk "
				+ disk.getDiskName() + " in the " + disk.getFormat()  
				+ " format."); 
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
				List<FileEntry> files = disk.getFiles();
				for (int i=0; i<files.size(); i++) {
					FileEntry entry = (FileEntry) files.get(i);
					entry.delete();
				}
				// Verify that we're back to what we started with:
                assertEquals(originalFree, disk.getFreeSpace(), "Free space does not match");
                assertEquals(originalUsed, disk.getUsedSpace(), "Used space does not match");
			}
		}
	}
	
	/**
	 * Test a create file, delete file, create file sequence.
	 * The expected result is that the deleted file entry is reused.
	 */
	protected void createDeleteCreate(FormattedDisk[] disks, String filetype) throws DiskException {
		for (int d=0; d<disks.length; d++) {
			FormattedDisk disk = disks[d];
			System.out.println("Exercising create, delete, create sequence " 
				+ "on disk " + disk.getDiskName() + ".");  
			writeFile(disk, 5432, filetype, false);
			List<FileEntry> files = disk.getFiles();
			for (int i=0; i<files.size(); i++) {
				FileEntry entry = (FileEntry) files.get(i);
				entry.delete();
			}
			writeFile(disk, 1234, filetype, false);
			files = disk.getFiles();
			for (int i=0; i<files.size(); i++) {
				FileEntry entry = (FileEntry) files.get(i);
				if (entry.isDeleted()) {
					showFiles(files, "", true); 
					fail("There should be no deleted files"); 
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
