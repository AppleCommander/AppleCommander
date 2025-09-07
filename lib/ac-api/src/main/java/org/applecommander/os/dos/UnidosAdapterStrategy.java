/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2025 by Robert Greene and others
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
package org.applecommander.os.dos;

import org.applecommander.device.TrackSectorDevice;
import org.applecommander.device.TrackSectorToBlockStrategy;

/**
 * UniDOS implements the disk by halves -- the first 800
 * blocks of the disk is disk 1 while the second 800 blocks
 * are disk 2.  Therefore, each block contains two sectors
 * of the same disk.
 */
public enum UnidosAdapterStrategy implements TrackSectorToBlockStrategy {
	/** Use this strategy to work with logical disk #1. */
	UNIDOS_DISK_1(0),
	/** Use this strategy to work with logical disk #2. */
	UNIDOS_DISK_2(50);
	
	private final int trackOffset;
	
	UnidosAdapterStrategy(int trackOffset) {
		this.trackOffset = trackOffset;
	}
	/** {@inheritDoc} */
	@Override
	public int computeBlock(int track, int sector) {
		return ((trackOffset + track) * getSectorsPerTrack() + sector) / 2;
	}
	/** {@inheritDoc} */
	@Override
	public int computeOffset(int track, int sector) {
		return (sector & 1) * TrackSectorDevice.SECTOR_SIZE;
	}
	/** {@inheritDoc} */
	@Override
	public String getName() {
		return String.format("UniDOS,trackOffset=%d", trackOffset);
	}
}
