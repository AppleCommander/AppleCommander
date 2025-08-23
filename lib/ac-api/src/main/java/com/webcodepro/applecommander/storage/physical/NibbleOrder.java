/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2003-2022 by Robert Greene
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
package com.webcodepro.applecommander.storage.physical;

import org.applecommander.hint.Hint;
import org.applecommander.source.Source;
import org.applecommander.util.DataBuffer;

import java.util.Arrays;

import static com.webcodepro.applecommander.storage.physical.NibbleCodec.*;

/**
 * Supports disk images stored in nibbilized DOS physical order.
 * <p>
 * @author Rob Greene (RobGreene@users.sourceforge.net)
 */
public class NibbleOrder extends DosOrder {
	/**
	 * This maps a DOS 3.3 sector to a physical sector.
	 * (readSector and writeSector work off of the DOS 3.3
	 * sector numbering.)
	 */
	public static final int[] DOS_SECTOR_SKEW = {
			0x0, 0xd, 0xb, 0x9, 0x7, 0x5, 0x3, 0x1,
			0xe, 0xc, 0xa, 0x8, 0x6, 0x4, 0x2, 0xf
	};

	private int sectorsPerTrack = 16;

	/**
	 * Construct a NibbleOrder.
	 */
	public NibbleOrder(Source source) {
		super(source);
		// Identify 13-sector vs 16-sector
		byte[] trackData = readTrackData(0);
		sectorsPerTrack = identifySectorsPerTrack(trackData);
	}

	/**
	 * Read nibbilized track data.
	 */
	protected byte[] readTrackData(int track) {
		int trackSize = getPhysicalSize() / getTracksPerDisk();
		return readBytes(track * trackSize, trackSize);
	}

	/**
	 * Write nibbilized track data.
	 */
	protected void writeTrackData(int track, byte[] trackData) {
		int trackSize = getPhysicalSize() / getTracksPerDisk();
		writeBytes(track * trackSize, trackData);
	}

	/**
	 * Retrieve the specified sector.  The primary source of information
	 * for this process is directly from Beneath Apple DOS, chapter 3.
	 */
	public byte[] readSector(int track, int dosSector) throws IllegalArgumentException {
		if (sectorsPerTrack == 16) {
			int sector = DOS_SECTOR_SKEW[dosSector];
			byte[] trackData = readTrackData(track);
			return readSectorFromTrack62(trackData, track, sector, getSectorsPerTrack());
		} else {
			byte[] trackData = readTrackData(track);
			return readSectorFromTrack53(trackData, track, dosSector, getSectorsPerTrack());
		}
	}

	/**
	 * Write the specified sector.
	 */
	public void writeSector(int track, int dosSector, byte[] sectorData) throws IllegalArgumentException {
		if (sectorsPerTrack == 13) {
			throw new RuntimeException("writing to 13-sector disks not supported");
		}
		int sector = DOS_SECTOR_SKEW[dosSector];
		byte[] trackData = readTrackData(track);
		writeSectorToTrack(trackData, sectorData, track, sector, getSectorsPerTrack());
		writeTrackData(track, trackData);
	}

	/**
	 * Answer with the number of tracks on this device.
	 */
	public int getTracksPerDisk() {
		return 35;
	}

	/**
	 * Answer with the number of sectors per track on this device.
	 */
	public int getSectorsPerTrack() {
		return sectorsPerTrack;
	}

	/**
	 * Answer with the number of blocks on this device.
	 * This cannot be computed since the physical size relates to disk
	 * bytes (6+2 encoded) instead of a full 8-bit byte.
	 */
	public int getBlocksOnDevice() {
		return 280;	// Note: Only relevant to DOS 3.3 disks; irrelevant for DOS 3.2. (Right?)
	}

    @Override
    public boolean is(Hint hint) {
        return hint == Hint.NIBBLE_SECTOR_ORDER;
    }

    /**
	 * Format the media.  Formatting at the ImageOrder level deals with
	 * low-level issues.  A typical ordering just needs to have the image
	 * "wiped," and that is the assumed implementation.  However, specialized
	 * orders - such as a nibbilized disk - need to lay down track and
	 * sector markers. 
	 */
	public void format() {
		if (sectorsPerTrack == 13) {
			throw new RuntimeException("formatting 13-sector disks not supported");
		}
		// pre-fill entire disk with 0xff
		byte[] diskImage = new byte[232960];	// 6656 bytes per track
		Arrays.fill(diskImage, (byte)0xff);
		getSource().writeBytes(0, DataBuffer.wrap(diskImage));
		// create initial address and data fields
		byte[] addressField = new byte[14];
		byte[] dataField = new byte[349];
		Arrays.fill(dataField, (byte)0x96);	// decodes to zeros
		byte[] addressPrologue = new byte[] { (byte)0xd5, (byte)0xaa, (byte)0x96 };
		byte[] dataPrologue = new byte[] { (byte)0xd5, (byte)0xaa, (byte)0xad };
		byte[] epilogue = new byte[] { (byte)0xde, (byte)0xaa, (byte)0xeb };
		System.arraycopy(addressPrologue, 0, addressField, 0, 3);
		System.arraycopy(epilogue, 0, addressField, 11, 3);
		System.arraycopy(dataPrologue, 0, dataField, 0, 3);
		System.arraycopy(epilogue, 0, dataField, 346, 3);
		// lay out track with address and data fields
		int addressSync = 43;	// number of sync bytes before address field
		int dataSync = 10;		// number of sync bytes before data field
		int volume = 254;		// disk volume# is always 254
		for (int track=0; track < getTracksPerDisk(); track++) {
			byte[] trackData = readTrackData(track);
			int offset = 0;
			for (int sector=0; sector < getSectorsPerTrack(); sector++) {
				// fill in address field:
				encodeOddEven(addressField, 3, volume);
				encodeOddEven(addressField, 5, track);
				encodeOddEven(addressField, 7, sector);
				encodeOddEven(addressField, 9, volume ^ track ^ sector);
				// write out sector data:
				offset+= addressSync;
				System.arraycopy(addressField, 0, trackData, offset, addressField.length);
				offset+= addressField.length;
				offset+= dataSync;
				System.arraycopy(dataField, 0, trackData, offset, dataField.length);
				offset+= dataField.length;
			}
			writeTrackData(track, trackData);
		}
	}
}
