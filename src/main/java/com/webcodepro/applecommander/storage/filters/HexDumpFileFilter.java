/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2003 by Robert Greene
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
package com.webcodepro.applecommander.storage.filters;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.util.AppleUtil;

/**
 * Filter the given file data to be the appropriate length.
 * <p>
 * Date created: Nov 2, 2002 9:07:52 PM
 * @author Rob Greene
 */
public class HexDumpFileFilter implements FileFilter {
	/**
	 * Constructor for BinaryFileFilter.
	 */
	public HexDumpFileFilter() {
		super();
	}

	/**
	 * Create the hex dump format.
	 * @see com.webcodepro.applecommander.storage.FileFilter#filter(FileEntry)
	 */
	public byte[] filter(FileEntry fileEntry) {
		return AppleUtil.getHexDump(fileEntry.getFileData()).getBytes();
	}

	/**
	 * Give suggested file name.
	 */
	public String getSuggestedFileName(FileEntry fileEntry) {
		String fileName = fileEntry.getFilename().trim();
		if (!fileName.toLowerCase().endsWith(".txt")) { //$NON-NLS-1$
			fileName = fileName + ".txt"; //$NON-NLS-1$
		}
		return fileName;
	}
}
