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

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.StorageBundle;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFileEntry;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;
import com.webcodepro.applecommander.storage.physical.ByteArrayImageLayout;
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import com.webcodepro.applecommander.storage.physical.ProdosOrder;
import com.webcodepro.applecommander.ui.ac.Name;
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
	 * 
	 * @return byte[] buffer containing full disk of data; null if unable to
	 *         read
	 * @throws IllegalArgumentException
	 *             if the filename is not able to be read
	 * @throws IOException
	 *             the file has some malformed-ness about it
	 */
	public static byte[] unpackSHKFile(String fileName) throws IOException
	{
		return unpackSHKFile(fileName, 0);
	}

	/**
	 * Interpret a NuFile/NuFX/Shrinkit archive as a full disk image.
	 * 
	 * @return byte[] buffer containing full disk of data; null if unable to
	 *         read
	 * @throws IllegalArgumentException
	 *             if the filename is not able to be read
	 * @throws IOException
	 *             the file has some malformed-ness about it
	 */
	public static byte[] unpackSHKFile(String fileName, int startBlocks) throws IOException
	{
		TextBundle textBundle = StorageBundle.getInstance();
		byte dmgBuffer[] = null;
		File file = new File(fileName);
		if (file.isDirectory() || !file.canRead())
		{
			throw new IOException(textBundle.format("NotAFile", fileName, 1)); //$NON-NLS-1$ 
		}
		InputStream is = new FileInputStream(file);
		NuFileArchive a = new NuFileArchive(is);
		// If we need to build a disk to hold files (i.e. .shk vs. .sdk), how big would that disk need to be?
		int newDiskSize = Disk.sizeToFit(a.getArchiveSize());
		if (startBlocks > 0)
			newDiskSize = startBlocks*512;
		ByteArrayImageLayout layout = new ByteArrayImageLayout(newDiskSize);
		ImageOrder imageOrder = new ProdosOrder(layout);
		// Create a new disk in anticipation of unpacking files - we don't actually know if we'll need it yet, though.
		FormattedDisk[] disks = ProdosFormatDisk.create(fileName, "APPLECOMMANDER", imageOrder); //$NON-NLS-1$
		// Make some typing easier... get a handle to the disk we created, with ProdosFormatDisk extensions. 
		ProdosFormatDisk pdDisk = (ProdosFormatDisk) disks[0];
		ThreadRecord dataFork, resourceFork;
		for (HeaderBlock b : a.getHeaderBlocks())
		{
			ProdosFileEntry newFile = null;
			dataFork = null;
			resourceFork = null;
			for (ThreadRecord r : b.getThreadRecords())
			{
				try
				{
					switch (r.getThreadKind())
					{
					case ASCII_TEXT:
						break;
					case ALLOCATED_SPACE:
						break;
					case APPLE_IIGS_ICON:
						break;
					case CREATE_DIRECTORY:
						break;
					case DATA_FORK:
						// This is a normal-ish file - hang on to the thread record
						dataFork = r;
						break;
					case DISK_IMAGE:
						dmgBuffer = readThread(r);
						break;
					case RESOURCE_FORK:
						// This is a resource fork - we're talking GSOS FST here
						resourceFork = r;
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
					throw new IOException(ex.getMessage());
				}
			}
			try
			{
				if ((dataFork != null) || (resourceFork != null))
				{
					Name name = new Name(b.getFilename());
					newFile = (ProdosFileEntry)name.createEntry(pdDisk);
					if (newFile != null)
					{
						if (resourceFork != null)
						{
							// If we have a resource fork in addition to a data fork,
							// then we've got a GSOS storage type $5. 
							newFile.setFileData(readThread(dataFork), readThread(resourceFork));
							newFile.setStorageType(0x05);
						}
						else
						{
							// We have a traditional file, no resource fork.
							newFile.setFileData(readThread(dataFork));
						}
						newFile.setFilename(b.getFinalFilename());
						newFile.setFiletype(b.getFileType());
						newFile.setAuxiliaryType((int) b.getExtraType());
						// TODO: dates differ by a month or so from what CiderPress reports.  
						newFile.setCreationDate(b.getCreateWhen());
						newFile.setLastModificationDate(b.getModWhen());
						newFile = null;
					}
				}
			}
			catch (Exception ex)
			{
				throw new IOException(ex.getMessage());
			}
		}
		if (dmgBuffer != null)
		{
			// Disk images take precedence... if they have both disk images and files, just return the disk.
			return dmgBuffer;
		}
		else
			return imageOrder.readBytes(0, newDiskSize);
	}

	/**
	 * readThread
	 * 
	 * Reads the data from a thread
	 * 
	 * @returns byte[] buffer, possibly null
	 */
	public static byte[] readThread(ThreadRecord thread) throws IOException
	{
		byte[] buffer = null;
		if (thread != null)
		{
			thread.readThreadData(new LittleEndianByteInputStream(thread.getRawInputStream()));
			InputStream fis = thread.getInputStream();
			buffer = new byte[(int) (thread.getThreadEof())];
			fis.read(buffer, 0, buffer.length);
			fis.close();
		}
		return buffer;
	}
}
