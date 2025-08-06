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
