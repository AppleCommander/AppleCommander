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

import org.applecommander.device.TrackSectorToBlockStrategy;

/**
 * OzDOS implements 1 sector per block (50 tracks * 32 sectors = 1600 sectors).
 * The differential is that disk 1 is the first half of the block while disk 2
 * is in the second half of the block. 
 */
public enum OzdosAdapterStrategy implements TrackSectorToBlockStrategy {
	/** Use this strategy to work with logical disk #1. */
	OZDOS_DISK_1(0x0),
	/** Use this strategy to work with logical disk #2. */
	OZDOS_DISK_2(0x100);
	
	private final int offset;
	
	private OzdosAdapterStrategy(int offset) {
		this.offset = offset;
	}
	/** {@inheritDoc} */
	@Override
	public int computeBlock(int track, int sector) {
		return track * 32 + sector;
	}
	/** {@inheritDoc} */
	@Override
	public int computeOffset(int track, int sector) {
		return offset;
	}
	/** {@inheritDoc} */
	@Override
	public String getName() {
		return String.format("OzDOS,offset=%x", offset);
	}
}
