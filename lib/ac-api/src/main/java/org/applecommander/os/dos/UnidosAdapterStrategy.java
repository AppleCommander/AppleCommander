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
	
	private UnidosAdapterStrategy(int trackOffset) {
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
