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
package com.webcodepro.applecommander.storage.os.prodos;

import com.webcodepro.applecommander.util.AppleUtil;

/**
 * Represents the ProDOS volume directory header.
 * <p>
 * Date created: Oct 5, 2002 10:58:24 PM
 * @author Rob Greene
 */
public class ProdosVolumeDirectoryHeader extends ProdosCommonDirectoryHeader {

	/**
	 * Constructor for ProdosVolumeDirectoryHeaderEntry.
	 */
	public ProdosVolumeDirectoryHeader(ProdosFormatDisk disk) {
		super(disk, 2);
	}

	/**
	 * Return the name of this volume.
	 */
	public String getVolumeName() {
		return AppleUtil.getProdosString(readFileEntry(), 0);
	}
	
	/**
	 * Set the name of this volume.
	 */
	public void setVolumeName(String volumeName) {
		byte[] data = readFileEntry();
		AppleUtil.setProdosString(data, 0, volumeName.toUpperCase(), 15);
		writeFileEntry(data);
	}
}
