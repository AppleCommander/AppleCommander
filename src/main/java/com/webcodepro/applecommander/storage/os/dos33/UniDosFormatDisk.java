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
package com.webcodepro.applecommander.storage.os.dos33;

import com.webcodepro.applecommander.storage.StorageBundle;
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Manages a disk that is in UniDOS format.
 * This is basically DOS 3.3 except that the disk has two volumes of
 * each 400K.
 * <br>
 * Created on Dec 13, 2002.
 * @author Rob
 */
public class UniDosFormatDisk extends DosFormatDisk {
	private TextBundle textBundle = StorageBundle.getInstance();
	/**
	 * Use this indicator to work with logical disk #1.
	 * It is essentially the track offset into the disk image.
	 */
	public static final int UNIDOS_DISK_1 = 0;
	/**
	 * Use this indicator to work with logical disk #2.
	 * It is essentially the track offset into the disk image.
	 */
	public static final int UNIDOS_DISK_2 = 50;
	/**
	 * Indicates which logical disk to work with (by offset
	 * into the disk image itself).
	 */
	private int logicalOffset;
	/**
	 * Constructor for UniDosFormatDisk.
	 */
	public UniDosFormatDisk(String filename, ImageOrder imageOrder, int logicalOffset) {
		super(filename, imageOrder);
		this.logicalOffset = logicalOffset;
	}
	/**
	 * Create a UniDosFormatDisk.
	 */
	public static DosFormatDisk[] create(String filename, ImageOrder imageOrder) {
		UniDosFormatDisk disk1 = new UniDosFormatDisk(filename, imageOrder, UNIDOS_DISK_1);
		UniDosFormatDisk disk2 = new UniDosFormatDisk(filename, imageOrder, UNIDOS_DISK_2);
		disk1.format();
		disk2.format();
		return new UniDosFormatDisk[] { disk1, disk2 };
	}
	/**
	 * Answer with the name of this disk.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getDiskName()
	 */
	public String getDiskName() {
		if (logicalOffset == UNIDOS_DISK_1) {
			return textBundle.format("DiskNameN", super.getDiskName(), 1); //$NON-NLS-1$
		} else if (logicalOffset == UNIDOS_DISK_2) {
			return textBundle.format("DiskNameN", super.getDiskName(), 2); //$NON-NLS-1$
		} else {
			return super.getDiskName();
		}
	}

	/**
	 * Returns the logical disk number.  This can be used to identify
	 * between disks when a format supports multiple logical volumes.
	 */
	public int getLogicalDiskNumber() {
		if (logicalOffset == UNIDOS_DISK_1) {
			return 1;
		} else if (logicalOffset == UNIDOS_DISK_2) {
			return 2;
		} else {
			return 0;
		}
	}
	/**
	 * Format the disk as UniDOS.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#format()
	 */
	public void format() {
		getImageOrder().format();
		format(31, 50, 32);
	}

	/**
	 * Retrieve the specified sector.
	 */
	public byte[] readSector(int track, int sector) throws IllegalArgumentException {
		return getImageOrder().readSector(track+logicalOffset, sector);
	}
	
	/**
	 * Write the specified sector.
	 */
	public void writeSector(int track, int sector, byte[] bytes) 
			throws IllegalArgumentException {
		getImageOrder().writeSector(track+logicalOffset, sector, bytes);
	}
}
