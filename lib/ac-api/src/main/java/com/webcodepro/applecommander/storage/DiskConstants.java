/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2022 by Robert Greene
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

import org.applecommander.image.UniversalDiskImage;

/**
 * Disk related constants.
 * @author Rob Greene
 */
public interface DiskConstants {
    int BLOCK_SIZE = 512;
	int SECTOR_SIZE = 256;
	int PRODOS_BLOCKS_ON_140KB_DISK = 280;
    int PRODOS_BLOCKS_ON_800KB_DISK = 1600;
    int DOS32_SECTORS_ON_115KB_DISK = 455;
	int DOS33_SECTORS_ON_140KB_DISK = 560;
	int APPLE_140KB_DISK = 143360;
    int APPLE_13SECTOR_DISK = 116480;
	int APPLE_140KB_NIBBLE_DISK = 232960;
    int APPLE_400KB_DISK = 409600;
	int APPLE_800KB_DISK = 819200;
	int APPLE_800KB_2IMG_DISK = APPLE_800KB_DISK + UniversalDiskImage.HEADER_SIZE;
	int APPLE_5MB_HARDDISK = 5242880;
	int APPLE_10MB_HARDDISK = 10485760;
	int APPLE_20MB_HARDDISK = 20971520;
	int APPLE_32MB_HARDDISK = 33553920;	// short one block!
}
