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


/**
 * A file filter taks a buffer of bytes and filters or converts the bytes
 * into another format.  An example would be to filter Apple text by
 * removing the high bit from each byte and stripping out all $00 values,
 * as that signified either the end of a file or filler space.
 * <p>
 * Date created: Nov 2, 2002 9:02:47 PM
 * @author Rob Greene
 */
public interface FileFilter {
	/**
	 * Process the given FileEntry and return a byte array with filtered data.
	 */
	public byte[] filter(FileEntry fileEntry);
	/**
	 * Give suggested file name.
	 */
	public String getSuggestedFileName(FileEntry fileEntry);
}
