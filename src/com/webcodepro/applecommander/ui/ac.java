/*
 * ac - an AppleCommander command line utility
 * Copyright (C) 2002 by Robert Greene
 * robgreene at users.sourceforge.net
 * Copyright (C) 2003, 2004 by John B.  Matthews
 * jmatthews at wight dot edu
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
package com.webcodepro.applecommander.ui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.FormattedDisk.DiskInformation;
import com.webcodepro.applecommander.storage.os.dos33.DosFormatDisk;
import com.webcodepro.applecommander.storage.os.pascal.PascalFormatDisk;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;
import com.webcodepro.applecommander.storage.physical.ByteArrayImageLayout;
import com.webcodepro.applecommander.storage.physical.DosOrder;
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import com.webcodepro.applecommander.storage.physical.ProdosOrder;
import com.webcodepro.applecommander.util.TextBundle;

public class ac {
	private static TextBundle textBundle = UiBundle.getInstance();

	public static void main(String[] args) {
		try {
			if (args.length == 0) {
				help();
			} else if ("-ls".equalsIgnoreCase(args[0])) {
				showDirectory(args[1], FormattedDisk.FILE_DISPLAY_STANDARD);
			} else if ("-l".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				showDirectory(args[1], FormattedDisk.FILE_DISPLAY_NATIVE);
			} else if ("-ll".equalsIgnoreCase(args[0])) {
				showDirectory(args[1], FormattedDisk.FILE_DISPLAY_DETAIL);
			} else if ("-e".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				getFile(args[1], args[2], true);
			} else if ("-g".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				getFile(args[1], args[2], false);
			} else if ("-p".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				putFile(args[1], args[2], args[3], args[4]);
			} else if ("-d".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				deleteFile(args[1], args[2]);
			} else if ("-i".equalsIgnoreCase(args[0])) {
				getDiskInfo(args[1]);
			} else if ("-dos140".equalsIgnoreCase(args[0])) {
				createDosDisk(args[1], Disk.APPLE_140KB_DISK);
			} else if ("-pas140".equalsIgnoreCase(args[0])) {
				createPasDisk(args[1], args[2], Disk.APPLE_140KB_DISK);
			} else if ("-pro140".equalsIgnoreCase(args[0])) {
				createProDisk(args[1], args[2], Disk.APPLE_140KB_DISK);
			} else if ("-pro800".equalsIgnoreCase(args[0])) {
				createProDisk(args[1], args[2], Disk.APPLE_800KB_DISK);
			} else {
				help();
			}
		} catch (Exception ex) {
			System.err.println(textBundle.format("CommandLineErrorMessage",  //$NON-NLS-1$
					ex.getLocalizedMessage()));
			ex.printStackTrace();
			help();
		}
	}

	/**
	 * Put <stdin> into the file named fileName on the disk named imageName;
	 * Note: only volume level supported; input size unlimited.
	 */
	static void putFile(String fileName, String fileType,
			String address, String imageName)
			throws IOException, DiskFullException {
		
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		byte[] inb = new byte[1024];
		int byteCount = 0;
		while ((byteCount = System.in.read(inb)) > 0) {
			buf.write(inb, 0, byteCount);
		}
		Disk disk = new Disk(imageName);
		FormattedDisk[] formattedDisks = disk.getFormattedDisks();
		FormattedDisk formattedDisk = formattedDisks[0];
		FileEntry entry = formattedDisk.createFile();
		entry.setFilename(fileName);
		entry.setFiletype(fileType);
		if (entry.needsAddress()) {
			entry.setAddress(stringToInt(address));
		}
		entry.setFileData(buf.toByteArray());
		formattedDisk.save();
	}
	
	/**
	 * Delete the file named fileName from the disk named imageName.
	 */
	static void deleteFile(String fileName, String imageName) throws IOException {
		Disk disk = new Disk(imageName);
		FormattedDisk[] formattedDisks = disk.getFormattedDisks();
		for (int i=0; i<formattedDisks.length; i++) {
			FormattedDisk formattedDisk = formattedDisks[i];
			List files = formattedDisk.getFiles();
			FileEntry entry = getEntry(files, fileName);
			if (entry != null) {
				entry.delete();
				disk.save();
			} else {
				System.err.println(textBundle.format(
					"CommandLineNoMatchMessage", fileName)); //$NON-NLS-1$
			}
		}
	}
	
	/**
	 * Get the file named filename from the disk named imageName;
	 * the file is filtered according to its type and sent to <stdout>.
	 */
	static void getFile(String fileName, String imageName, boolean filter) throws IOException {
		Disk disk = new Disk(imageName);
		FormattedDisk[] formattedDisks = disk.getFormattedDisks();
		for (int i=0; i<formattedDisks.length; i++) {
			FormattedDisk formattedDisk = formattedDisks[i];
			List files = formattedDisk.getFiles();
			FileEntry entry = getEntry(files, fileName);
			if (entry != null) {
				if (filter) {
					FileFilter ff = entry.getSuggestedFilter();
					byte[] buf = ff.filter(entry);
					System.out.write(buf, 0, buf.length);
				} else {
					byte[] buf = entry.getFileData();
					System.out.write(buf, 0, buf.length);
				}
			} else {
				System.err.println(textBundle.format(
						"CommandLineNoMatchMessage", fileName)); //$NON-NLS-1$
			}
		}
	}
	
	/**
	 * Recursive routine to locate a specific file by filename;
	 * In the instance of a system with directories (e.g. ProDOS),
	 * this really returns the first file with the given filename.
	 */
	static FileEntry getEntry(List files, String fileName) {
		FileEntry theEntry = null;
		if (files != null) {
			for (int i=0; i<files.size(); i++) {
				FileEntry entry = (FileEntry) files.get(i);
				String entryName = entry.getFilename();
				if (!entry.isDeleted() && 
					fileName.equalsIgnoreCase(entryName)) {
					return entry;
				}
				if (entry.isDirectory()) {
					theEntry = getEntry(((DirectoryEntry) entry).
						getFiles(), fileName);
					if (theEntry != null) {return theEntry;}
				}
			}
		}
		return null;
	}
	
	/**
	 * Display a directory listing of the disk named imageName.
	 */
	static void showDirectory(String imageName, int display) throws IOException {
		Disk disk = new Disk(imageName);
		FormattedDisk[] formattedDisks = disk.getFormattedDisks();
		for (int i=0; i<formattedDisks.length; i++) {
			FormattedDisk formattedDisk = formattedDisks[i];
			System.out.println();
			System.out.println(formattedDisk.getDiskName());
			List files = formattedDisk.getFiles();
			if (files != null) {
				showFiles(files, "", display); //$NON-NLS-1$
			}
			System.out.println(textBundle.format(
				"CommandLineStatus", //$NON-NLS-1$
				new Object[] {
					formattedDisk.getFormat(),
					new Integer(formattedDisk.getFreeSpace()),
					new Integer(formattedDisk.getUsedSpace())
				}));
			System.out.println();
		}
	}
	
	/**
	 * Recursive routine to display directory entries.
	 * In the instance of a system with directories (e.g. ProDOS),
	 * this really returns the first file with the given filename.
	 */
	static void showFiles(List files, String indent, int display) {
		for (int i=0; i<files.size(); i++) {
			FileEntry entry = (FileEntry) files.get(i);
			if (!entry.isDeleted()) {
				List data = entry.getFileColumnData(display);
				System.out.print(indent);
				for (int d=0; d<data.size(); d++) {
					System.out.print(data.get(d));
					System.out.print(" "); //$NON-NLS-1$
				}
				System.out.println();
			}
			if (entry.isDirectory()) {
				showFiles(((DirectoryEntry)entry).getFiles(),
					indent + "  ", display); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Display information about the given disk image.
	 */
	static void getDiskInfo(String imageName) throws IOException {
		Disk disk = new Disk(imageName);
		FormattedDisk[] formattedDisks = disk.getFormattedDisks();
		for (int i=0; i<formattedDisks.length; i++) {
			FormattedDisk formattedDisk = formattedDisks[i];
			Iterator iterator = formattedDisk.getDiskInformation().iterator();
			while (iterator.hasNext()) {
				DiskInformation diskinfo = (DiskInformation) iterator.next();
				System.out.println(diskinfo.getLabel() + ": " + diskinfo.getValue());
 			}
 		}
 	}

	/**
	 * Create a DOS disk image.
	 */
	static void createDosDisk(String fileName, int imageSize)
			throws IOException {
		ByteArrayImageLayout layout = new ByteArrayImageLayout(imageSize);
		ImageOrder imageOrder = new DosOrder(layout);
		FormattedDisk[] disks =
			DosFormatDisk.create(fileName, imageOrder);
		disks[0].save();
	}

	/**
	 * Create a Pascal disk image.
	 */
	static void createPasDisk(String fileName, String volName, int imageSize)
			throws IOException {
		ByteArrayImageLayout layout = new ByteArrayImageLayout(imageSize);
		ImageOrder imageOrder = new ProdosOrder(layout);
		FormattedDisk[] disks =
			PascalFormatDisk.create(fileName, volName, imageOrder);
		disks[0].save();
	}
	
	/**
	 * Create a ProDOS disk image.
	 */
	static void createProDisk(String fileName, String volName, int imageSize)
			throws IOException {
		ByteArrayImageLayout layout = new ByteArrayImageLayout(imageSize);
		ImageOrder imageOrder = new ProdosOrder(layout);
		FormattedDisk[] disks =
			ProdosFormatDisk.create(fileName, volName, imageOrder);
		disks[0].save();
	}
	
	static int stringToInt(String s) {
		int i = 0;
		try {
			i = Integer.parseInt(s.trim());
		} catch (NumberFormatException ignored) {
			// ignored
		}
		return i;
	}
	
	static void help() {
		System.err.println(textBundle.format("CommandLineHelp", AppleCommander.VERSION)); //$NON-NLS-1$
	}

}
