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
package com.webcodepro.applecommander.storage;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a disk that is in Apple DOS 3.3 format.
 * <p>
 * Date created: Oct 4, 2002 12:29:23 AM
 * @author: Rob Greene
 */
public class DosFormatDisk extends FormattedDisk {
	/**
	 * Indicates the length in bytes of the DOS file entry field.
	 */
	public static final int FILE_DESCRIPTIVE_ENTRY_LENGTH = 35;
	/**
	 * Indicates the index of the track in the location array.
	 */	
	public static final int TRACK_LOCATION_INDEX = 0;
	/**
	 * Indicates the index of the sector in the location array.
	 */	
	public static final int SECTOR_LOCATION_INDEX = 1;
	/**
	 * The standard DOS 3.3 catalog track.
	 */
	public static final int CATALOG_TRACK = 17;
	/**
	 * The standard VTOC sector.
	 */
	public static final int VTOC_SECTOR = 0;

	/**
	 * Use this inner interface for managing the disk usage data.
	 * This offloads format-specific implementation to the implementing class.
	 */
	private class DosDiskUsage implements DiskUsage {
		private int[] location = null;
		public boolean hasNext() {
			return location == null
				|| (location[TRACK_LOCATION_INDEX] < getTracks()
				&& location[SECTOR_LOCATION_INDEX] < getSectors());
		}
		public void next() {
			if (location == null) {
				location = new int[2];
			} else {
				location[SECTOR_LOCATION_INDEX]++;
				if (location[SECTOR_LOCATION_INDEX] >= getSectors()) {
					location[SECTOR_LOCATION_INDEX] = 0;
					location[TRACK_LOCATION_INDEX]++;
				}
			}
		}
		/**
		 * Get the free setting for the bitmap at the current location.
		 */
		public boolean isFree() {
			if (location == null || location.length != 2) {
				throw new IllegalArgumentException("Invalid dimension for isFree! Did you call next first?");
			}
			return isSectorFree(location[TRACK_LOCATION_INDEX], 
				location[SECTOR_LOCATION_INDEX], readVtoc());
		}
		public boolean isUsed() {
			return !isFree();
		}
	}

	/**
	 * Constructor for DosFormatDisk.
	 * @param filename
	 * @param diskImage
	 * @param order
	 */
	public DosFormatDisk(String filename, byte[] diskImage) {
		super(filename, diskImage);
	}

	/**
	 * Constructor for DosFormatDisk.  All DOS disk images are expected to
	 * be 140K in size.
	 * @param filename
	 * @param diskImage
	 * @param order
	 */
	public DosFormatDisk(String filename) {
		super(filename, new byte[APPLE_140KB_DISK]);
	}

	/**
	 * Identify the operating system format of this disk as DOS 3.3.
	 * @see com.webcodepro.applecommander.storage.Disk#getFormat()
	 */
	public String getFormat() {
		return "DOS 3.3";
	}

	/**
	 * Retrieve a list of files.
	 * @see com.webcodepro.applecommander.storage.Disk#getFiles()
	 */
	public List getFiles() {
		List list = new ArrayList();
		byte[] vtoc = readVtoc();
		int track = AppleUtil.getUnsignedByte(vtoc[1]);
		int sector = AppleUtil.getUnsignedByte(vtoc[2]);
		while (track != 0) {	// iterate through all catalog sectors
			byte[] catalogSector = readSector(track, sector);
			int offset = 0x0b;
			while (offset < 0xff) {	// iterate through all entries
				byte[] entry = new byte[FILE_DESCRIPTIVE_ENTRY_LENGTH];
				System.arraycopy(catalogSector, offset, entry, 0, entry.length);
				if (entry[0] != 0) {
					list.add(new DosFileEntry(entry, this));
				}
				offset+= entry.length;
			}
			track = catalogSector[1];
			sector = catalogSector[2];
		}
		return list;
	}

	/**
	 * Identify if this disk format as not capable of having directories.
	 * @see com.webcodepro.applecommander.storage.Disk#hasDirectories()
	 */
	public boolean canHaveDirectories() {
		return false;
	}

	/**
	 * Compute the amount of freespace available on the disk.
	 * This algorithm completely ignores tracks and sectors by
	 * running through the entire bitmap stored on the VTOC.
	 * @see com.webcodepro.applecommander.storage.Disk#getFreeSpace()
	 */
	public int getFreeSpace() {
		return getFreeSectors() * SECTOR_SIZE;
	}
	
	/**
	 * Comput the number of free sectors available on the disk.
	 */
	public int getFreeSectors() {
		byte[] vtoc = readVtoc();
		int freeSectors = 0;
		for (int offset=0x38; offset<0xff; offset++) {
			byte bitmap = vtoc[offset];
			freeSectors+= AppleUtil.getBitCount(bitmap);
		}
		return freeSectors;
	}

	/**
	 * Return the amount of used space in bytes.
	 * @see com.webcodepro.applecommander.storage.Disk#getUsedSpace()
	 */
	public int getUsedSpace() {
		return getUsedSectors() * SECTOR_SIZE;
	}
	
	/**
	 * Compute the number of used sectors on the disk.
	 */
	public int getUsedSectors() {
		return getTotalSectors() - getFreeSectors();
	}

	/**
	 * Compute the total number of sectors available on the disk.
	 */
	public int getTotalSectors() {
		int tracks = getTracks();
		int sectors = getSectors();
		return tracks * sectors;
	}

	/**
	 * Return the DOS disk name.  Basically, the DISK VOLUME #xxx
	 * that a CATALOG command would show.  Note that Java bytes are
	 * signed, so a little mojo is in order.
	 * @see com.webcodepro.applecommander.storage.Disk#getDiskName()
	 */
	public String getDiskName() {
		int volumeNumber = AppleUtil.getUnsignedByte(readVtoc()[0x06]);
		return "DISK VOLUME #" + volumeNumber;
	}

	/**
	 * Return the VTOC (Volume Table Of Contents).
	 */
	protected byte[] readVtoc() {
		return readSector(CATALOG_TRACK, VTOC_SECTOR);
	}

	/**
	 * Get the disk usage iterator.
	 */
	public DiskUsage getDiskUsage() {
		return new DosDiskUsage();
	}

	/**
	 * Get the number of tracks on this disk.
	 */
	public int getTracks() {
		byte[] vtoc = readVtoc();
		return AppleUtil.getUnsignedByte(vtoc[0x34]);
	}

	/**
	 * Get the number of sectors on this disk.
	 */
	public int getSectors() {
		byte[] vtoc = readVtoc();
		return AppleUtil.getUnsignedByte(vtoc[0x35]);
	}

	/**
	 * Get suggested dimensions for display of bitmap. For DOS 3.3, that information
	 * is stored in the VTOC, and that information is fairly important.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getBitmapDimensions()
	 */
	public int[] getBitmapDimensions() {
		int tracks = getTracks();
		int sectors = getSectors();
		return new int[] { tracks, sectors };
	}

	/**
	 * Get the length of the bitmap.
	 */
	public int getBitmapLength() {
		return getTotalSectors();
	}

	/**
	 * Get the labels to use in the bitmap.
	 */
	public String[] getBitmapLabels() {
		return new String[] { "Track", "Sector" };
	}
	
	/**
	 * Get DOS-specific disk information.
	 */
	public List getDiskInformation() {
		List list = super.getDiskInformation();
		list.add(new DiskInformation("Total Sectors", getTotalSectors()));
		list.add(new DiskInformation("Free Sectors", getFreeSectors()));
		list.add(new DiskInformation("Used Sectors", getUsedSectors()));
		list.add(new DiskInformation("Tracks On Disk", getTracks()));
		list.add(new DiskInformation("Sectors On Disk", getSectors()));
		return list;
	}

	/**
	 * Get the standard file column header information.
	 * This default implementation is intended only for standard mode.
	 */
	public List getFileColumnHeaders(int displayMode) {
		List list = new ArrayList();
		switch (displayMode) {
			case FILE_DISPLAY_NATIVE:
				list.add(new FileColumnHeader(" ", 1, FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader("Type", 1, FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader("Size (sectors)", 3, FileColumnHeader.ALIGN_RIGHT));
				list.add(new FileColumnHeader("Name", 30, FileColumnHeader.ALIGN_LEFT));
				break;
			case FILE_DISPLAY_DETAIL:
				list.add(new FileColumnHeader(" ", 1, FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader("Type", 1, FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader("Name", 30, FileColumnHeader.ALIGN_LEFT));
				list.add(new FileColumnHeader("Size (bytes)", 6, FileColumnHeader.ALIGN_RIGHT));
				list.add(new FileColumnHeader("Size (sectors)", 3, FileColumnHeader.ALIGN_RIGHT));
				list.add(new FileColumnHeader("Deleted?", 7, FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader("Track/Sector List", 7, FileColumnHeader.ALIGN_CENTER));
				break;
			default:	// FILE_DISPLAY_STANDARD
				list.addAll(super.getFileColumnHeaders(displayMode));
				break;
		}
		return list;
	}

	/**
	 * Indicates if this disk format supports "deleted" files.
	 */
	public boolean supportsDeletedFiles() {
		return true;
	}

	/**
	 * Indicates if this disk image can read data from a file.
	 */
	public boolean canReadFileData() {
		return true;
	}
	
	/**
	 * Indicates if this disk image can write data to a file.
	 */
	public boolean canWriteFileData() {
		return false;	// FIXME - not implemented
	}
	
	/**
	 * Indicates if this disk image can create a file.
	 */
	public boolean canCreateFile() {
		return false;	// FIXME - not implemented
	}
	
	/**
	 * Indicates if this disk image can delete a file.
	 */
	public boolean canDeleteFile() {
		return false;	// FIXME - not implemented
	}

	/**
	 * Get the data associated with the specified FileEntry.
	 */
	public byte[] getFileData(FileEntry fileEntry) {
		if ( !(fileEntry instanceof DosFileEntry)) {
			throw new IllegalArgumentException("Most have a DOS 3.3 file entry!");
		}
		DosFileEntry dosEntry = (DosFileEntry) fileEntry;
		// Size is calculated by sectors used - not actual size - as size varies
		// on filetype, etc.
		byte[] fileData = new byte[(dosEntry.getSectorsUsed()-1) * SECTOR_SIZE];
		int track = dosEntry.getTrack();
		int sector = dosEntry.getSector();
		int offset = 0;
		while (track != 0) {
			byte[] trackSectorList = readSector(track, sector);
			track = AppleUtil.getUnsignedByte(trackSectorList[0x01]);
			sector = AppleUtil.getUnsignedByte(trackSectorList[0x02]);
			for (int i=0x0c; i<0x100; i+=2) {
				int t = AppleUtil.getUnsignedByte(trackSectorList[i]);
				if (t == 0) break;
				int s = AppleUtil.getUnsignedByte(trackSectorList[i+1]);
				byte[] sectorData = readSector(t,s);
				System.arraycopy(sectorData, 0, fileData, offset, sectorData.length);
				offset+= sectorData.length;
			}
		}
		return fileData;
	}

	/**
	 * Format the disk as DOS 3.3.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#format()
	 */
	public void format() {
		writeBootCode();
		// create catalog sectors
		byte[] data = new byte[SECTOR_SIZE];
		for (int sector=15; sector > 0; sector--) {
			if (sector == 0) {
				data[0x01] = CATALOG_TRACK;
				data[0x02] = (byte)(sector-1);
			} else {
				data[0x01] = 0;
				data[0x02] = 0;
			}
			writeSector(CATALOG_TRACK, sector, data);
		}
		// create VTOC
		data[0x01] = CATALOG_TRACK;	// track# of first catalog sector
		data[0x02] = 15;			// sector# of first catalog sector
		data[0x03] = 3;				// DOS 3.3 formatted
		data[0x06] = (byte)254;	// DISK VOLUME#
		data[0x27] = 122;			// maximum # of T/S pairs in a sector
		data[0x30] = CATALOG_TRACK+1;	// last track where sectors allocated
		data[0x31] = 1;				// direction of allocation
		data[0x34] = 35;			// tracks per disk
		data[0x35] = 16;			// sectors per disk
		data[0x37] = 1;				// 36/37 are # of bytes per sector
		for (int track=0; track<35; track++) {
			for (int sector=0; sector<16; sector++) {
				if (track == 0 || track == CATALOG_TRACK) {
					setSectorUsed(track, sector, data);
				} else {
					setSectorFree(track, sector, data);
				}
			}
		}
		writeSector(CATALOG_TRACK, VTOC_SECTOR, data);
	}

	/**
	 * Indicates if a specific track/sector is free.
	 */
	public boolean isSectorFree(int track, int sector, byte[] vtoc) {
		checkRange(track, sector);
		byte byt = vtoc[getFreeMapByte(track, sector)];
		return AppleUtil.isBitSet(byt, getFreeMapBit(sector));
	}
	
	/**
	 * Indicates if a specific track/sector is used.
	 */
	public boolean isSectorUsed(int track, int sector, byte[] vtoc) {
		return !isSectorFree(track, sector, vtoc);
	}
	
	/**
	 * Sets the track/sector indicator to free.
	 */
	public void setSectorFree(int track, int sector, byte[] vtoc) {
		checkRange(track, sector);
		int offset = getFreeMapByte(track, sector);
		byte byt = vtoc[offset];
		byt = AppleUtil.setBit(byt, getFreeMapBit(sector));
		vtoc[offset] = byt;
	}

	/**
	 * Sets the track/sector indicator to used.
	 */
	public void setSectorUsed(int track, int sector, byte[] vtoc) {
		checkRange(track, sector);
		int offset = getFreeMapByte(track, sector);
		byte byt = vtoc[offset];
		byt = AppleUtil.clearBit(byt, getFreeMapBit(sector));
		vtoc[offset] = byt;
	}
	
	/**
	 * Compute the VTOC byte for the T/S map.
	 */
	protected int getFreeMapByte(int track, int sector) {
		return 0x38 + (track * 4) + (sector / 8);
	}
	
	/**
	 * Compute the VTOC bit for the T/S map.
	 */
	protected int getFreeMapBit(int sector) {
		return 7 - (sector % 8);
	}
	
	/**
	 * Validate track/sector range.
	 */
	protected void checkRange(int track, int sector) {
		if (track > 35 || sector > 32) {
			throw new IllegalArgumentException(
				"Invalid track (" + track + "), sector (" + sector
				+ ") combination.");
		}
	}
}
