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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * Helper class to load Apple2 disk images.
 * This class identifies the format, and the type of disk, loading the
 * appropriate disk management class.
 * <p>
 * Date created: Oct 3, 2002 10:54:34 PM
 * @author: Rob Greene
 * @deprecated
 */
public class DiskHelper {
	public static final int APPLE_140KB_DISK = 143360;
	public static final int APPLE_800KB_DISK = 819200;
	public static final int APPLE_800KB_2IMG_DISK = APPLE_800KB_DISK + 0x40;

	/**
	 * Load an Apple2 disk from a given filename.
	 * Determine type of disk, load approparitely, and
	 * construct the appropriate Disk object.
	 */
	public static FormattedDisk load(String filename) throws IOException {
		if (filename == null) return null;
		byte[] diskImage = loadDisk(filename);
		
		Disk test = new Disk(filename, diskImage);
		if (test.isProdosFormat()) {
			return new ProdosFormatDisk(filename, diskImage);
		} else if (test.isDosFormat()) {
			return new DosFormatDisk(filename, diskImage);
		} else if (test.isPascalFormat()) {
			return new PascalFormatDisk(filename, diskImage);
		} else if (test.isRdosFormat()) {
			return new RdosFormatDisk(filename, diskImage);
		}
			
		// FIXME: Should return unknown disk Disk
		return null;
	}
	
	/**
	 * Read in a disk in the same order as the image.
	 * Disk itself will handle location translation.
	 */
	private static byte[] loadDisk(String filename) throws IOException {
		InputStream input = new FileInputStream(filename);
		if (filename.toLowerCase().endsWith(".gz")) {
			input = new GZIPInputStream(input);
		}
		int diskSize = APPLE_140KB_DISK;
		if (filename.toLowerCase().endsWith(".2img") || filename.toLowerCase().endsWith(".2mg")) {
			diskSize = APPLE_800KB_2IMG_DISK;
		}
		ByteArrayOutputStream diskImage = 
			new ByteArrayOutputStream(diskSize);
		byte[] data = new byte[1024];
		int bytes;
		while ((bytes = input.read(data)) > 0) {
			diskImage.write(data, 0, bytes);
		}
		input.close();
		return diskImage.toByteArray();
	}
}
