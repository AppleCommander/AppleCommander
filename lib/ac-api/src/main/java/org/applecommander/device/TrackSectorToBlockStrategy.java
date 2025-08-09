package org.applecommander.device;

/**
 * The AdapterStrategy abstracts the differing components of a device
 * that is expected to be composed of tracks and sectors but sits on
 * a block device instead.  In particular, this is expected to have
 * the capacity to support both UniDOS and OzDOS (both support two 400K
 * DOS images on a 800K 3.5" floppy).  Additionally, there is the 
 * potential that the adapter will function to support DOS.MASTER
 * and other products where DOS was patched to fit on a hard disk. 
 */
public interface TrackSectorToBlockStrategy {
	/** Translate a track and sector reference into the proper block number. */
	public int computeBlock(int track, int sector);
	/** 
	 * Translate a given track and sector into the proper offset into the block. 
	 * This is needed due to the fact that a ProDOS block is 512 bytes but a DOS
	 * sector is 256 bytes -- meaning two sectors fit into a block.
	 */
	public int computeOffset(int track, int sector);
	/**
	 * Indicates the total number of tracks in this device.  The default is
	 * 50 tracks, which is the maximum number of tracks DOS will support;
	 * this method will have to be overridden for DOS.MASTER type products.
	 */
	default public int getTotalTracks() {
		return 50;
	}
	/**
	 * Indicates the total number of sectors per track in this device.  The
	 * default is 32 sectors per track which is the maximum number of sectors
	 * that DOS supports.  Note that we do make the assumption that the disk
	 * geometry is fixed and sectors do not vary.
	 */
	default public int getSectorsPerTrack() {
		return 32;
	}
	/** The name is information used in various toString methods.  This ensures it is populated. */
	public String getName();
}