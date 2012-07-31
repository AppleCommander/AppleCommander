package com.webcodepro.shrinkit;

/*
 * Copyright (C) 2012 by David Schmidt
 * david__schmidt at users.sourceforge.net
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.StorageBundle;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFileEntry;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;
import com.webcodepro.applecommander.storage.physical.ByteArrayImageLayout;
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import com.webcodepro.applecommander.storage.physical.ProdosOrder;
import com.webcodepro.applecommander.util.TextBundle;
import com.webcodepro.shrinkit.io.LittleEndianByteInputStream;

/**
 * Some higher-level utilities for dealing with a NuFX archive.
 * 
 * @author david__schmidt at users.sourceforge.net
 */
public class Utilities
{
	/**
	 * Interpret a NuFile/NuFX/Shrinkit archive as a full disk image.
	 * Note that a disk within a shk (Disk Disintegrator Deluxe 5.0_D1.SHK) should
	 * be interpreted directly as that disk image.
	 *
	 * @return byte[] buffer containing full disk of data; null if unable to read
	 * @throws IllegalArgumentException if the filename is not able to be read
	 * @throws IOException the file has some malformed-ness about it
	 */
	public static byte[] unpackSHKFile(String fileName) throws IOException {
		TextBundle textBundle = StorageBundle.getInstance();
		byte dmgBuffer[] = null;
		File file = new File(fileName);
		if (file.isDirectory() || !file.canRead()) {
			throw new IllegalArgumentException(textBundle.format("NotAFile", fileName, 1)); //$NON-NLS-1$ 
		}
		InputStream is = new FileInputStream(file);
		NuFileArchive a = new NuFileArchive(is);
		int newDiskSize = Disk.sizeToFit(a.getArchiveSize());
		ByteArrayImageLayout layout = new ByteArrayImageLayout(newDiskSize);
		ImageOrder imageOrder = new ProdosOrder(layout);
		FormattedDisk[] disks = ProdosFormatDisk.create(fileName, "APPLECOMMANDER", imageOrder);
		ProdosFormatDisk pdDisk = (ProdosFormatDisk)disks[0];
		for (HeaderBlock b : a.getHeaderBlocks()) {
			ProdosFileEntry newFile = null;
			for (ThreadRecord r : b.getThreadRecords()) {
				try
				{
					switch (r.getThreadKind()) {
					case ASCII_TEXT:
						break;
					case ALLOCATED_SPACE:
						break;
					case APPLE_IIGS_ICON:
						break;
					case CREATE_DIRECTORY:
						break;
					case DATA_FORK:
						// This is a normal-ish file
						newFile = (ProdosFileEntry) pdDisk.createFile();
						if (newFile != null) {
							newFile.setFileData(readThread(r));
						}
						break;
					case DISK_IMAGE:
						dmgBuffer = readThread(r);
						break;
					case RESOURCE_FORK:
						break;
					case FILENAME:
						break;
					default:
						// Hmmm, this should not occur - but let us not fret about it.
						break;
					}
				}
				catch (Exception ex)
				{
					System.out.println(ex);
				}
			}
			if (newFile != null) {
				newFile.setFilename(b.getFilename());
				newFile.setFiletype(b.getFileType());
				newFile.setAuxiliaryType((int)b.getExtraType());
				newFile.setCreationDate(b.getCreateWhen());
				newFile.setLastModificationDate(b.getModWhen());
				newFile = null;
			}
		}
		if (dmgBuffer != null)
			return dmgBuffer;
		else
			return imageOrder.readBytes(0,newDiskSize);
	}

	/**
	 * readThread
	 * 
	 * Reads the data from a thread
	 * @returns byte[] buffer
	 */
	public static byte[] readThread(ThreadRecord thread) throws IOException {
		thread.readThreadData(new LittleEndianByteInputStream(thread.getRawInputStream()));
		InputStream fis = thread.getInputStream();
		byte[] buffer = new byte[(int)(thread.getThreadEof())];
		fis.read(buffer,0,buffer.length);
		fis.close();
		return buffer;
	}
}
