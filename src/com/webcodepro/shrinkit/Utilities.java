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

import com.webcodepro.applecommander.storage.StorageBundle;
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
	 * Interpret a SDK NuFile/NuFX/Shrinkit archive as a full disk image.
	 *
	 * @return byte[] buffer containing full disk of data; null if unable to read
	 * @throws IllegalArgumentException if the filename is not able to be read
	 * @throws IOException the file has some malformed-ness about it
	 */
	public static byte[] unpackSDKFile(String fileName) throws IOException {
		TextBundle textBundle = StorageBundle.getInstance();
		byte buffer[] = null;
		ThreadRecord dataThread = null;
		File file = new File(fileName);
		if (file.isDirectory() || !file.canRead()) {
			throw new IllegalArgumentException(textBundle.format("NotAFile", fileName, 1)); //$NON-NLS-1$ 
		}
		InputStream is = new FileInputStream(file);
		NuFileArchive a = new NuFileArchive(is);
		for (HeaderBlock b : a.getHeaderBlocks()) {
			for (ThreadRecord r : b.getThreadRecords()) {
				try
				{
					if (r.getThreadKind() == ThreadKind.DISK_IMAGE)
					{
						dataThread = r;
					}
				}
				catch (Exception ex)
				{
					System.out.println(ex);
				}
			}
			dataThread.readThreadData(new LittleEndianByteInputStream(dataThread.getRawInputStream()));
			InputStream fis = dataThread.getInputStream();
			int dmgLen = (int)(dataThread.getThreadEof());
			buffer = new byte[dmgLen];
			fis.read(buffer,0,dmgLen);
			fis.close();
		}
		return buffer;
	}
}
