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

import com.webcodepro.applecommander.storage.DiskFullException;

/**
 * A ProdosDiskSizeDoesNotMatchException is thrown during write operations
 * on a ProDOS volume if the physical size of the disk image (*.HDV file)
 * is not large enough to handle the actual size of the disk.  By actual size,
 * it is intended to be the ProDOS bitmap.
 * <br>
 * Created on Mar 22, 2003.
 * @author Rob Greene
 */
public class ProdosDiskSizeDoesNotMatchException extends DiskFullException {

	private static final long serialVersionUID = 0xFFFFFFFF80000000L;

	/**
	 * Constructor for ProdosDiskSizeDoesNotMatchException.
	 */
	public ProdosDiskSizeDoesNotMatchException(String description) {
		super(description);
	}
}
