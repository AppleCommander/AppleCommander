/*
 * ac - an AppleCommander command line utility
 * Copyright (C) 2002 by Robert Greene
 * robgreene at users.sourceforge.net
 * Copyright (C) 2003 by John B.  Matthews
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

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.ProdosFormatDisk;

import java.io.IOException;
import java.util.List;

public class ac {

	public static void main(String[] args)  throws IOException {
		try {
			if (args.length == 0) {
				help();
			} else if ("-l".equalsIgnoreCase(args[0])) {
				showDirectory(args[1]);
			} else if ("-e".equalsIgnoreCase(args[0])) {
				getFile(args[1], args[2], true);
			} else if ("-g".equalsIgnoreCase(args[0])) {
				getFile(args[1], args[2], false);
			} else if ("-p".equalsIgnoreCase(args[0])) {
				putFile(args[1], args[2], args[3], args[4]);
			} else if ("-d".equalsIgnoreCase(args[0])) {
				deleteFile(args[1], args[2]);
			} else if ("-p140".equalsIgnoreCase(args[0])) {
				createPDisk(args[1], args[2], ProdosFormatDisk.APPLE_140KB_DISK);
			} else if ("-p800".equalsIgnoreCase(args[0])) {
				createPDisk(args[1], args[2], ProdosFormatDisk.APPLE_800KB_DISK);
			} else {
				help();
			}
		} catch (Exception ex) {
			System.err.println("Error: " + ex);
			ex.printStackTrace();
			help();
		}
	}

	/**
	 * Put <stdin> into the file named fileName on the disk named imageName;
	 * Note: only volume level supported, 32K limit.
	 */
	static void putFile(String fileName, String fileType,
			String address, String imageName)
			throws IOException, DiskFullException {
		
		byte[] buf = new byte[32768];
		Disk disk = new Disk(imageName);
		FormattedDisk[] formattedDisks = disk.getFormattedDisks();
		FormattedDisk formattedDisk = formattedDisks[0];
		int byteCount = System.in.read(buf);
		byte[] data = new byte[byteCount];
		System.arraycopy(buf, 0, data, 0, byteCount);
		FileEntry entry = formattedDisk.createFile();
		entry.setFilename(fileName);
		entry.setFiletype(fileType);
		if (entry.needsAddress()) {
			entry.setAddress(stringToInt(address));
		}
		entry.setFileData(data);
		formattedDisk.save();
	}
	
	/**
	 * Delete the file named fileName from the disk named imageName.
	 */
	static void deleteFile(String fileName, String imageName)
			throws IOException {
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
				System.err.println(fileName + ": No match.");
			}
		}
	}
	
	/**
	 * Get the file named filename from the disk named imageName;
	 * the file is filtered according to its type and sent to <stdout>.
	 */
	static void getFile(String fileName, String imageName, boolean filter)
			throws IOException {
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
				System.err.println(fileName + ": No match.");
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
	static void showDirectory(String imageName) throws IOException {
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
			System.out.print(formattedDisk.getFormat() + " format; ");
			System.out.print(formattedDisk.getFreeSpace() + " bytes free; ");
			System.out.println(formattedDisk.getUsedSpace() + " bytes used.");
			System.out.println();
		}
	}
	
	/**
	 * Recursive routine to display directory entries.
	 * In the instance of a system with directories (e.g. ProDOS),
	 * this really returns the first file with the given filename.
	 */
	static void showFiles(List files, String indent) {
		for (int i=0; i<files.size(); i++) {
			FileEntry entry = (FileEntry) files.get(i);
			if (!entry.isDeleted()) {
				List data = entry.getFileColumnData(
					FormattedDisk.FILE_DISPLAY_NATIVE);
				System.out.print(indent);
				for (int d=0; d<data.size(); d++) {
					System.out.print(data.get(d));
					System.out.print(" ");
				}
				System.out.println();
			}
			if (entry.isDirectory()) {
				showFiles(((DirectoryEntry)entry).getFiles(),
					indent + "  ");
			}
		}
	}
	
	/**
	 * Create a ProDOS disk image.
	 */
	static void createPDisk(String fileName, String volName, int imageSize)
			throws IOException {
		FormattedDisk[] disks =
			ProdosFormatDisk.create(fileName, volName, imageSize);
		disks[0].save();
	}
	
	static int stringToInt(String s) {
		int i = 0;
		try {
			i = Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
		}
		return i;
	}
	
	static void help() {
		System.err.println("AppleCommander command line options:");
		System.err.println("-l <imagename> list directory of image.");
		System.err.println("-e <filename> <imagename> export file from image to stdout.");
		System.err.println("-g <filename> <imagename> get raw file from image to stdout.");
		System.err.println("-p <destname> <type> <addr> <imagename> put stdin");
		System.err.println("   in destname on image, using file type and address.");
		System.err.println("-d <filename> <imagename> delete file from image.");
		System.err.println("-p140 <imagename> <volname> create a 140K ProDOS image.");
		System.err.println("-p800 <imagename> <volname> create a 800K ProDOS image.");
	}

}
