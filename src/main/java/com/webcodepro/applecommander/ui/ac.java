/*
 * ac - an AppleCommander command line utility
 * Copyright (C) 2002 by Robert Greene
 * robgreene at users.sourceforge.net
 * Copyright (C) 2003, 2004 by John B. Matthews
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.FormattedDisk.DiskInformation;
import com.webcodepro.applecommander.storage.filters.BinaryFileFilter;
import com.webcodepro.applecommander.storage.filters.HexDumpFileFilter;
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
 * ac provides a command line interface to key AppleCommander functions. Text
 * similar to this is produced in response to the -h option.
 * 
 * <pre>
 * CommandLineHelp = AppleCommander command line options [{0}]:
 * -i  &lt;imagename&gt; [&lt;imagename&gt;] display information about image(s).
 * -ls &lt;imagename&gt; [&lt;imagename&gt;] list brief directory of image(s).
 * -l  &lt;imagename&gt; [&lt;imagename&gt;] list directory of image(s).
 * -ll &lt;imagename&gt; [&lt;imagename&gt;] list detailed directory of image(s).
 * -e  &lt;imagename&gt; &lt;filename&gt; [&lt;output&gt;] export file from image to stdout
 *     or to an output file. 
 * -x  &lt;imagename&gt; [&lt;directory&gt;] extract all files from image to directory.
 * -g  &lt;imagename&gt; &lt;filename&gt; [&lt;output&gt;] get raw file from image to stdout
 *     or to an output file. 
 * -p  &lt;imagename&gt; &lt;filename&gt; &lt;type&gt; [[$|0x]&lt;addr&gt;] put stdin
 *     in filename on image, using file type and address [0x2000].
 * -d  &lt;imagename&gt; &lt;filename&gt; delete file from image.
 * -k  &lt;imagename&gt; &lt;filename&gt; lock file on image.
 * -u  &lt;imagename&gt; &lt;filename&gt; unlock file on image.
 * -n  &lt;imagename&gt; &lt;volname&gt; change volume name (ProDOS or Pascal).
 * -cc65 &lt;imagename&gt; &lt;filename&gt; &lt;type&gt; put stdin with cc65 header
 *       in filename on image, using file type and address from header.
 * -geos &lt;imagename&gt; interpret stdin as a ProDOS GEOS transfer file and place on image.
 * -dos140 &lt;imagename&gt; create a 140K DOS 3.3 image.
 * -pro140 &lt;imagename&gt; &lt;volname&gt; create a 140K ProDOS image.
 * -pro800 &lt;imagename&gt; &lt;volname&gt; create an 800K ProDOS image.
 * -pas140 &lt;imagename&gt; &lt;volname&gt; create a 140K Pascal image.
 * -pas800 &lt;imagename&gt; &lt;volname&gt; create an 800K Pascal image.
 * -convert &lt;filename&gt; &lt;imagename&gt; uncompress a ShrinkIt file or disk image
 *           or convert a DiskCopy 4.2 image into a ProDOS disk image.
 * </pre>
 * 
 * @author John B. Matthews
 */
public class ac {
	private static TextBundle textBundle = UiBundle.getInstance();

	public static void main(String[] args) {
		try {
			if (args.length == 0) {
				help();
			} else if ("-i".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				getDiskInfo(args);
			} else if ("-ls".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				showDirectory(args, FormattedDisk.FILE_DISPLAY_STANDARD);
			} else if ("-l".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				showDirectory(args, FormattedDisk.FILE_DISPLAY_NATIVE);
			} else if ("-ll".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				showDirectory(args, FormattedDisk.FILE_DISPLAY_DETAIL);
			} else if ("-e".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				getFile(args[1], args[2], true,
					(args.length > 3 ? new PrintStream(new FileOutputStream(args[3])) : System.out));
			} else if ("-x".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				getFiles(args[1], (args.length > 2 ? args[2] : ""));
			} else if ("-g".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				getFile(args[1], args[2], false,
					(args.length > 3 ? new PrintStream(new FileOutputStream(args[3])) : System.out));
			} else if ("-p".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				putFile(args[1], new Name(args[2]), args[3],
					(args.length > 4 ? args[4] : "0x2000"));
			} else if ("-d".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				deleteFile(args[1], args[2]);
			} else if ("-k".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				setFileLocked(args[1], args[2], true);
			} else if ("-u".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				setFileLocked(args[1], args[2], false);
			} else if ("-n".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				setDiskName(args[1], args[2]);
			} else if ("-cc65".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				putCC65(args[1], new Name(args[2]), args[3]);
			} else if ("-geos".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				putGEOS(args[1]);
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
			} else if ("-convert".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				if (args.length > 3)
					convert(args[1], args[2], Integer.parseInt(args[3]));
				else
					convert(args[1], args[2]);
			} else {
				help();
			}
		} catch (Exception ex) {
			System.err.println(textBundle.format("CommandLineErrorMessage", //$NON-NLS-1$
				ex.getLocalizedMessage()));
			ex.printStackTrace();
			help();
		}
	}

	/**
	 * Put fileName from the local filesytem into the file named fileOnImageName on the disk named imageName;
	 * Note: only volume level supported; input size unlimited.
	 */
	public static void putFile(String fileName, String imageName, String fileOnImageName, String fileType, String address) throws IOException, DiskFullException {
		Name name = new Name(fileOnImageName);
		File file = new File(fileName);
		if (!file.canRead())
		{
			throw new IOException("Unable to read input file named "+fileName+"."); // FIXME - NLS
		}
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		byte[] inb = new byte[1024];
		int byteCount = 0;
		InputStream is = new FileInputStream(file);
		while ((byteCount = is.read(inb)) > 0) {
			buf.write(inb, 0, byteCount);
		}
		Disk disk = new Disk(imageName);
		FormattedDisk[] formattedDisks = disk.getFormattedDisks();
		FormattedDisk formattedDisk = formattedDisks[0];
		FileEntry entry = name.createEntry(formattedDisk);
		if (entry != null) {
			entry.setFiletype(fileType);
			entry.setFilename(name.name);
			entry.setFileData(buf.toByteArray());
			if (entry.needsAddress()) {
				entry.setAddress(stringToInt(address));
			}
			formattedDisk.save();
		}
	}

	/**
	 * Put &lt;stdin&gt. into the file named fileName on the disk named imageName;
	 * Note: only volume level supported; input size unlimited.
	 */
	static void putFile(String imageName, Name name, String fileType,
		String address) throws IOException, DiskFullException {

		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		byte[] inb = new byte[1024];
		int byteCount = 0;
		while ((byteCount = System.in.read(inb)) > 0) {
			buf.write(inb, 0, byteCount);
		}
		Disk disk = new Disk(imageName);
		FormattedDisk[] formattedDisks = disk.getFormattedDisks();
		if (formattedDisks == null)
			System.out.println("Dude, formattedDisks is null!");
		FormattedDisk formattedDisk = formattedDisks[0];
		if (!disk.isSDK() && !disk.isDC42()) {
			FileEntry entry = name.createEntry(formattedDisk);
			if (entry != null) {
				entry.setFiletype(fileType);
				entry.setFilename(name.name);
				entry.setFileData(buf.toByteArray());
				if (entry.needsAddress()) {
					entry.setAddress(stringToInt(address));
				}
				formattedDisk.save();
			} else {
				throw new IOException("Unable to create entry...");
			}
				
		}
		else
			throw new IOException(textBundle.get("CommandLineSDKReadOnly"));  //$NON-NLS-1$
	}

	/**
	 * Put file fileName into the file named fileOnImageName on the disk named imageName;
	 * Assume a cc65 style four-byte header with start address in bytes 0-1.
	 */
	public static void putCC65(String fileName, String imageName, String fileOnImageName, String fileType)
		throws IOException, DiskFullException {

		byte[] header = new byte[4];
		if (System.in.read(header, 0, 4) == 4) {
			int address = AppleUtil.getWordValue(header, 0);
			putFile(fileName, imageName, fileOnImageName, fileType, Integer.toString(address));
		}
	}

	/**
	 * Put &lt;stdin> into the file named fileName on the disk named imageName;
	 * Assume a cc65 style four-byte header with start address in bytes 0-1.
	 */
	static void putCC65(String imageName, Name name, String fileType)
		throws IOException, DiskFullException {

		byte[] header = new byte[4];
		if (System.in.read(header, 0, 4) == 4) {
			int address = AppleUtil.getWordValue(header, 0);
			putFile(imageName, name, fileType, Integer.toString(address));
		}
	}

	/**
	 * Interpret &lt;stdin> as a GEOS file and place it on the disk named imageName.
	 * This would only make sense for a ProDOS-formatted disk.
	 */
	static void putGEOS(String imageName)
		throws IOException, DiskFullException {
		putFile(imageName, new Name("GEOS-Should Be ProDOS"), "GEO", "0"); //$NON-NLS-2$ $NON-NLS-3$
	}

	/**
	 * Delete the file named fileName from the disk named imageName.
	 */
	static void deleteFile(String imageName, String fileName)
		throws IOException {
		Disk disk = new Disk(imageName);
		Name name = new Name(fileName);
		if (!disk.isSDK() && !disk.isDC42()) {
			FormattedDisk[] formattedDisks = disk.getFormattedDisks();
			for (int i = 0; i < formattedDisks.length; i++) {
				FormattedDisk formattedDisk = formattedDisks[i];
				FileEntry entry = name.getEntry(formattedDisk);
				if (entry != null) {
					entry.delete();
					disk.save();
				} else {
					System.err.println(textBundle.format(
							"CommandLineNoMatchMessage", name.fullName)); //$NON-NLS-1$
				}
			}
		}
		else
			throw new IOException(textBundle.get("CommandLineSDKReadOnly"));
	}

	/**
	 * Get the file named filename from the disk named imageName; the file is
	 * filtered according to its type and sent to &lt;stdout>.
	 */
	static void getFile(String imageName, String fileName, boolean filter, PrintStream out)
		throws IOException {
		Disk disk = new Disk(imageName);
		Name name = new Name(fileName);
		FormattedDisk[] formattedDisks = disk.getFormattedDisks();
		if (out == null)
			out = System.out;
		for (int i = 0; i < formattedDisks.length; i++) {
			FormattedDisk formattedDisk = formattedDisks[i];
			FileEntry entry = name.getEntry(formattedDisk);
			if (entry != null) {
				if (filter) {
					FileFilter ff = entry.getSuggestedFilter();
					if (ff instanceof BinaryFileFilter)
						ff = new HexDumpFileFilter();
					byte[] buf = ff.filter(entry);
					out.write(buf, 0, buf.length);
				} else {
					byte[] buf = entry.getFileData();
					out.write(buf, 0, buf.length);
				}
			} else {
				System.err.println(textBundle.format(
					"CommandLineNoMatchMessage", name.fullName)); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Extract all files in the image according to their respective filetype.
	 */
	static void getFiles(String imageName, String directory) throws IOException {
		Disk disk = new Disk(imageName);
		if ((directory != null) && (directory.length() > 0)) {
			// Add a final directory separator if the user didn't supply one
			if (!directory.endsWith(File.separator))
				directory = directory + File.separator;
		} else {
			directory = "."+File.separator;
		}
		FormattedDisk[] formattedDisks = disk.getFormattedDisks();
		for (int i = 0; i < formattedDisks.length; i++) {
			FormattedDisk formattedDisk = formattedDisks[i];
			writeFiles(formattedDisk.getFiles(), directory);			
		}
	}

	/**
	 * Recursive routine to write directory and file entries.
	 */
	static void writeFiles(List files, String directory) throws IOException {
		Iterator it = files.iterator();
		while (it.hasNext()) {
			FileEntry entry = (FileEntry) it.next();
			if ((entry != null) && (!entry.isDeleted()) && (!entry.isDirectory())) {
				FileFilter ff = entry.getSuggestedFilter();
				if (ff instanceof BinaryFileFilter)
					ff = new HexDumpFileFilter();
				byte[] buf = ff.filter(entry);
				String filename = ff.getSuggestedFileName(entry);
				File file = new File(directory + filename);
				File dir = new File(directory);
				dir.mkdirs();
				OutputStream output = new FileOutputStream(file);
				output.write(buf, 0, buf.length);
				output.close();
			} else if (entry.isDirectory()) {
				writeFiles(((DirectoryEntry) entry).getFiles(),directory+entry.getFilename()+File.separator);
			}
		}
	}
	
	/**
	 * Recursive routine to locate a specific file by filename; In the instance
	 * of a system with directories (e.g. ProDOS), this really returns the first
	 * file with the given filename.
	 * @deprecated
	 */
	static FileEntry getEntry(List files, String fileName) {
		FileEntry entry = null;
		if (files != null) {
			for (int i = 0; i < files.size(); i++) {
				entry = (FileEntry) files.get(i);
				String entryName = entry.getFilename();
				if (!entry.isDeleted() && fileName.equalsIgnoreCase(entryName)) {
					return entry;
				}
				if (entry.isDirectory()) {
					entry = getEntry(((DirectoryEntry) entry).getFiles(), fileName);
					if (entry != null) {
						return entry;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Display a directory listing of each disk in args.
	 */
	static void showDirectory(String[] args, int display) throws IOException {
		for (int d = 1; d < args.length; d++) {
			try {
				Disk disk = new Disk(args[d]);
				FormattedDisk[] formattedDisks = disk.getFormattedDisks();
				for (int i = 0; i < formattedDisks.length; i++) {
					FormattedDisk formattedDisk = formattedDisks[i];
					System.out.print(args[d] + " ");
					System.out.println(formattedDisk.getDiskName());
					List files = formattedDisk.getFiles();
					if (files != null) {
						showFiles(files, "", display); //$NON-NLS-1$
					}
					System.out.println(textBundle.format("CommandLineStatus", //$NON-NLS-1$
						new Object[] { formattedDisk.getFormat(),
						new Integer(formattedDisk.getFreeSpace()),
						new Integer(formattedDisk.getUsedSpace()) }));
					System.out.println();
				}
			} catch (RuntimeException e) {
				System.out.println(args[d] + ": " + e.getMessage()); //$NON-NLS-1$
				System.out.println();
			}
		}
	}

	/**
	 * Recursive routine to display directory entries. In the instance of a
	 * system with directories (e.g. ProDOS), this really returns the first file
	 * with the given filename.
	 */
	static void showFiles(List files, String indent, int display) {
		for (int i = 0; i < files.size(); i++) {
			FileEntry entry = (FileEntry) files.get(i);
			if (!entry.isDeleted()) {
				List data = entry.getFileColumnData(display);
				System.out.print(indent);
				for (int d = 0; d < data.size(); d++) {
					System.out.print(data.get(d));
					System.out.print(" "); //$NON-NLS-1$
				}
				System.out.println();
			}
			if (entry.isDirectory()) {
				showFiles(((DirectoryEntry) entry).getFiles(),
					indent + "  ", display); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Display information about each disk in args.
	 */
	static void getDiskInfo(String[] args) throws IOException {
		for (int d = 1; d < args.length; d++) {
			Disk disk = new Disk(args[d]);
			FormattedDisk[] formattedDisks = disk.getFormattedDisks();
			for (int i = 0; i < formattedDisks.length; i++) {
				FormattedDisk formattedDisk = formattedDisks[i];
				Iterator iterator = formattedDisk.getDiskInformation().iterator();
				while (iterator.hasNext()) {
					DiskInformation diskinfo = (DiskInformation) iterator.next();
					System.out.println(diskinfo.getLabel() + ": " + diskinfo.getValue());
				}
			}
			System.out.println();
		}
	}

	/**
	 * Set the lockState of the file named fileName on the disk named imageName.
	 * Proposed by David Schmidt.
	 */
	public static void setFileLocked(String imageName, String name, boolean lockState) throws IOException {
		setFileLocked(imageName, new Name(name), lockState);
	}

	/**
	 * Set the lockState of the file named fileName on the disk named imageName.
	 * Proposed by David Schmidt.
	 */
	static void setFileLocked(String imageName, Name name,
		boolean lockState) throws IOException {
		Disk disk = new Disk(imageName);
		if (!disk.isSDK() && !disk.isDC42()) {
			FormattedDisk[] formattedDisks = disk.getFormattedDisks();
			for (int i = 0; i < formattedDisks.length; i++) {
				FormattedDisk formattedDisk = formattedDisks[i];
				FileEntry entry = name.getEntry(formattedDisk);
				if (entry != null) {
					entry.setLocked(lockState);
					disk.save();
				} else {
					System.err.println(textBundle.format(
						"CommandLineNoMatchMessage", name.fullName)); //$NON-NLS-1$
				}
			}
		}
		else
			throw new IOException(textBundle.get("CommandLineSDKReadOnly"));
	}

	/**
	 * Set the volume name for a given disk image. Only effective for ProDOS or
	 * Pascal disks; others ignored. Proposed by David Schmidt.
	 */
	public static void setDiskName(String imageName, String volName)
		throws IOException {
		Disk disk = new Disk(imageName);
		if (!disk.isSDK() && !disk.isDC42()) {
			FormattedDisk[] formattedDisks = disk.getFormattedDisks();
			FormattedDisk formattedDisk = formattedDisks[0];
			formattedDisk.setDiskName(volName);
			formattedDisks[0].save();
		}
		else
			throw new IOException(textBundle.get("CommandLineSDKReadOnly"));
	}

	/**
	 * Create a DOS disk image.
	 */
	public static void createDosDisk(String fileName, int imageSize)
		throws IOException {
		ByteArrayImageLayout layout = new ByteArrayImageLayout(imageSize);
		ImageOrder imageOrder = new DosOrder(layout);
		FormattedDisk[] disks = DosFormatDisk.create(fileName, imageOrder);
		disks[0].save();
	}

	/**
	 * Create a Pascal disk image.
	 */
	public static void createPasDisk(String fileName, String volName, int imageSize)
		throws IOException {
		ByteArrayImageLayout layout = new ByteArrayImageLayout(imageSize);
		ImageOrder imageOrder = new ProdosOrder(layout);
		FormattedDisk[] disks = PascalFormatDisk.create(fileName, volName, imageOrder);
		disks[0].save();
	}

	/**
	 * Create a ProDOS disk image.
	 */
	public static void createProDisk(String fileName, String volName, int imageSize)
		throws IOException {
		ByteArrayImageLayout layout = new ByteArrayImageLayout(imageSize);
		ImageOrder imageOrder = new ProdosOrder(layout);
		FormattedDisk[] disks = ProdosFormatDisk.create(fileName, volName, imageOrder);
		disks[0].save();
	}

	/**
	 * Unshrink or otherwise interpret incoming data depending on what kind it is:
	 * 
	 * DiskCopy 4.2 image - convert it to a ProDOS image
	 * SDK disk image - unpack it to a disk image
	 * ShrinkIt file bundle - unpack files onto a disk image sized to fit
	 */
	static void convert(String shrinkName, String imageName)
		throws IOException {
		convert(shrinkName, imageName, 0);
	}

	/**
	 * Unshrink or otherwise interpret incoming data depending on what kind it is:
	 * 
	 * DiskCopy 4.2 image - convert it to a ProDOS image
	 * SDK disk image - unpack it to a disk image
	 * ShrinkIt file bundle - unpack files onto a disk image sized to fit, or as specified in numbers of blocks
	 */
	static void convert(String shrinkName, String imageName, int imageSize)
		throws IOException {
		Disk disk = new Disk(shrinkName, imageSize);
		disk.setFilename(imageName);
		disk.save();
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
		System.err.println(textBundle.format(
			"CommandLineHelp", AppleCommander.VERSION)); //$NON-NLS-1$
	}

	public static class Name {
		private String fullName;
		private String name;
		private String[] path;
		
		public Name(String s) {
			this.fullName = s;
			if (s.startsWith("/")) {
				fullName = s.substring(1, s.length());
			}
			this.path = s.split("/");
			this.name = path[path.length - 1];
		}
		
		public FileEntry getEntry(FormattedDisk formattedDisk) {
			List files = formattedDisk.getFiles();
			FileEntry entry = null;
			for (int i = 0; i < path.length - 1; i++) {
				String dirName = path[i];
				for (int j = 0; j < files.size(); j++) {
					entry = (FileEntry) files.get(j);
					String entryName = entry.getFilename();
					if (entry.isDirectory() && dirName.equalsIgnoreCase(entryName)) {
						files = ((DirectoryEntry) entry).getFiles();
					}
				}
			}
			for (int i = 0; i < files.size(); i++) {
				entry = (FileEntry) files.get(i);
				String entryName = entry.getFilename();
				if (!entry.isDeleted() && name.equalsIgnoreCase(entryName)) {
					return entry;
				}
			}
			return null;
		}
		
		public FileEntry createEntry(FormattedDisk formattedDisk) throws DiskFullException {
			if (path.length == 1) {
				return formattedDisk.createFile();
			}
			List files = formattedDisk.getFiles();
			DirectoryEntry dir = null, parentDir = null;
			for (int i = 0; i < path.length - 1; i++) {
				String dirName = path[i];
				dir = null;
				for (int j = 0; j < files.size(); j++) {
					FileEntry entry = (FileEntry) files.get(j);
					String entryName = entry.getFilename();
					if (!entry.isDeleted() && entry.isDirectory() && dirName.equalsIgnoreCase(entryName)) {
						dir = (DirectoryEntry) entry;
						parentDir = dir;
						files = dir.getFiles();
					}
				}
				if (dir == null) {
					if (parentDir != null) {
						// If there's a parent directory in the mix, add
						// the new child directory to that.
						dir = parentDir.createDirectory(dirName);
						parentDir = dir;
					} else {
						// Add the directory to the root of the filesystem
						dir = formattedDisk.createDirectory(dirName);
						parentDir = dir;
					}
				}
			}
			if (dir != null) {
				return dir.createFile();
			} else {
				System.err.println(textBundle.format(
					"CommandLineNoMatchMessage", fullName)); //$NON-NLS-1$
				return null;
			}
		}
	}
}
