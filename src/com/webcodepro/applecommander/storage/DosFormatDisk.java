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
import java.util.BitSet;
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
			byte[] vtoc = getVtoc();
			byte byt = vtoc[0x38 + (location[TRACK_LOCATION_INDEX] * 4) 
				+ (location[SECTOR_LOCATION_INDEX] / 8)];
			boolean free = AppleUtil.isBitSet(byt, 7 - (location[SECTOR_LOCATION_INDEX] % 8));
			return free;
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
		byte[] vtoc = getVtoc();
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
		byte[] vtoc = getVtoc();
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
		int volumeNumber = AppleUtil.getUnsignedByte(getVtoc()[0x06]);
		return "DISK VOLUME #" + volumeNumber;
	}

	/**
	 * Return the VTOC (Volume Table Of Contents).
	 */
	protected byte[] getVtoc() {
		return readSector(0x11, 0);
	}

	/**
	 * Get the disk usage bitmap.  The size could vary and is stored in the
	 * VTOC.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getBitmap()
	 * @deprecated DOS 3.3.po comes up with 448 entries in the bitmap?!
	 */
	public BitSet getBitmap() {
		byte[] vtoc = getVtoc();
		int tracks = getTracks();
		int sectors = getSectors();
		BitSet bitmap = new BitSet(tracks * sectors);
		// individually test each track & sector - should handle 140K or 400K disks!
		int count = 0;
		for (int t=0; t<tracks; t++) {
			for (int s=0; s<sectors; s++) {
				byte byt = vtoc[0x38 + (t * 4) + (s / 8)];
				boolean free = AppleUtil.isBitSet(byt, 7 - (s % 8));
				bitmap.set(count, free);
				count++;
			}
		}
		return bitmap;
	}

	/**
	 * Get the free setting for the bitmap at a specific location.
	 * The location is specified by an int array to support block
	 * and track/sector formatted disks.
	 * @deprecated
	 */
	public boolean isLocationFree(int[] location) {
		if (location == null || location.length != 2) {
			throw new IllegalArgumentException("Invalid dimension for isLocationFree!");
		}
		byte[] vtoc = getVtoc();
		byte byt = vtoc[0x38 + (location[TRACK_LOCATION_INDEX] * 4) 
			+ (location[SECTOR_LOCATION_INDEX] / 8)];
		boolean free = AppleUtil.isBitSet(byt, 7 - (location[SECTOR_LOCATION_INDEX] % 8));
		return free;
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
		byte[] vtoc = getVtoc();
		return AppleUtil.getUnsignedByte(vtoc[0x34]);
	}

	/**
	 * Get the number of sectors on this disk.
	 */
	public int getSectors() {
		byte[] vtoc = getVtoc();
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
}
