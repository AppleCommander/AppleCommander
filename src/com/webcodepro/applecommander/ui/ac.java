/*
 * ac - an AppleCommander command line utility
 * Copyright (C) 2002 by Robert Greene
 * robgreene at users.sourceforge.net
 * Copyright (C) 2003, 2004 by John B.  Matthews
 * matthewsj at users.sourceforge.net
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
import com.webcodepro.applecommander.util.AppleUtil;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * ac provides a command line interface to key AppleCommander functions.
 * Text similar to this is produced in response to the -h option.
 * <pre>
 *   AppleCommander command line options [<i>version</i>]:
 *   -i  &lt;imagename&gt; display information about image.
 *   -ls &lt;imagename&gt; list brief directory of image.
 *   -l  &lt;imagename&gt; list directory of image.
 *   -ll &lt;imagename&gt; list detailed directory of image.
 *   -e  &lt;imagename&gt; &lt;filename&gt; export file from image to stdout.
 *   -g  &lt;imagename&gt; &lt;filename&gt; get raw file from image to stdout.
 *   -p  &lt;imagename&gt; &lt;filename&gt; &lt;type&gt; [[$|0x]&lt;addr&gt;] put stdin
 *       in filename on image, using file type and address [0x2000].
 *   -d  &lt;imagename&gt; &lt;filename&gt; delete file from image.
 *   -cc65 &lt;imagename&gt; &lt;filename&gt; &lt;type&gt; put stdin with cc65 header
 *         in filename on image, using file type and address from header.
 *   -dos140 &lt;imagename&gt; create a 140K DOS 3.3 image.
 *   -pro140 &lt;imagename&gt; &lt;volname&gt; create a 140K ProDOS image.
 *   -pro800 &lt;imagename&gt; &lt;volname&gt; create an 800K ProDOS image.
 *   -pas140 &lt;imagename&gt; &lt;volname&gt; create a 140K Pascal image.
 *   -pas800 &lt;imagename&gt; &lt;volname&gt; create an 800K Pascal image.
 * </pre>
 * @author John B. Matthews
 */
public class ac {
	private static TextBundle textBundle = UiBundle.getInstance();

	public static void main(String[] args) {
		try {
			if (args.length == 0) {
				help();
			} else if ("-ls".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				showDirectory(args[1], FormattedDisk.FILE_DISPLAY_STANDARD);
			} else if ("-l".equalsIgnoreCase(args[0])) {  //$NON-NLS-1$
				showDirectory(args[1], FormattedDisk.FILE_DISPLAY_NATIVE);
			} else if ("-ll".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				showDirectory(args[1], FormattedDisk.FILE_DISPLAY_DETAIL);
			} else if ("-e".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				getFile(args[1], args[2], true);
			} else if ("-g".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				getFile(args[1], args[2], false);
			} else if ("-p".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				putFile(args[1], args[2], args[3],
					(args.length > 4 ? args[4] : "0x2000"));
			} else if ("-d".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				deleteFile(args[1], args[2]);
			} else if ("-i".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				getDiskInfo(args[1]);
			} else if ("-cc65".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				putCC65(args[1], args[2], args[3]);
			} else if ("-dos140".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				createDosDisk(args[1], Disk.APPLE_140KB_DISK);
			} else if ("-pas140".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				createPasDisk(args[1], args[2], Disk.APPLE_140KB_DISK);
			} else if ("-pas800".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				createPasDisk(args[1], args[2], Disk.APPLE_800KB_DISK);
			} else if ("-pro140".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				createProDisk(args[1], args[2], Disk.APPLE_140KB_DISK);
			} else if ("-pro800".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
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
	static void putFile(String imageName, String fileName, String fileType, String address)
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
		entry.setFileData(buf.toByteArray());
		if (entry.needsAddress()) {
			entry.setAddress(stringToInt(address));
		}
		formattedDisk.save();
	}
	
	/**
	 * Put <stdin> into the file named fileName on the disk named imageName;
	 * Assume a cc65 style four-byte header with start address in bytes 0-1.
	 */
	static void putCC65(String imageName, String fileName, String fileType)
			throws IOException, DiskFullException {
		
		byte[] header = new byte[4];
		if (System.in.read(header, 0, 4) == 4) {
			int address = AppleUtil.getWordValue(header, 0);
			putFile(imageName, fileName, fileType, Integer.toString(address));
		}
	}
	
	/**
	 * Delete the file named fileName from the disk named imageName.
	 */
	static void deleteFile(String imageName, String fileName) throws IOException {
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
	static void getFile(String imageName, String fileName, boolean filter) throws IOException {
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
			s = s.trim().toLowerCase();
			if (s.startsWith("$")) { // 6502, Motorola
				i = Integer.parseInt(s.substring(1), 0x10);
			} else if (s.startsWith("0x")) { // Java, C
				i = Integer.parseInt(s.substring(2), 0x10);
			} else {
			    i = Integer.parseInt(s);
			}
		} catch (NumberFormatException nfe) {
			i = 0x2000;
		}
		return i;
	}
	
	static void help() {
		System.err.println(textBundle.format("CommandLineHelp", AppleCommander.VERSION)); //$NON-NLS-1$
	}

}
