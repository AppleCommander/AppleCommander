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

import com.webcodepro.applecommander.storage.physical.ImageOrder;
import com.webcodepro.applecommander.util.AppleUtil;

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
	 * The standard track/sector pairs in a track/sector list.
	 */
	public static final int TRACK_SECTOR_PAIRS = 122;
	/**
	 * The list of filetypes available.
	 */
	private static final String[] filetypes = { 
			"T", "A", "I", "B", "S", "R", "a", "b" 
		};

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
	public DosFormatDisk(String filename, ImageOrder imageOrder) {
		super(filename, imageOrder);
	}

	/**
	 * Create a DosFormatDisk.  All DOS disk images are expected to
	 * be 140K in size.
	 */
	public static DosFormatDisk[] create(String filename, ImageOrder imageOrder) {
		DosFormatDisk disk = new DosFormatDisk(filename, imageOrder);
		disk.format();
		return new DosFormatDisk[] { disk };
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
				if (catalogSector[offset] != 0) {
					list.add(new DosFileEntry(this, track, sector, offset));
				}
				offset+= DosFileEntry.FILE_DESCRIPTIVE_ENTRY_LENGTH;
			}
			track = catalogSector[1];
			sector = catalogSector[2];
		}
		return list;
	}
	
	/**
	 * Create a FileEntry.
	 */
	public FileEntry createFile() throws DiskFullException {
		byte[] vtoc = readVtoc();
		int track = AppleUtil.getUnsignedByte(vtoc[1]);
		int sector = AppleUtil.getUnsignedByte(vtoc[2]);
		while (track != 0) {	// iterate through all catalog sectors
			byte[] catalogSector = readSector(track, sector);
			int offset = 0x0b;
			while (offset < 0xff) {	// iterate through all entries
				int value = AppleUtil.getUnsignedByte(catalogSector[offset]);
				if (value == 0 || value == 0xff) {
					return new DosFileEntry(this, track, sector, offset);
				}
				offset+= DosFileEntry.FILE_DESCRIPTIVE_ENTRY_LENGTH;
			}
			track = catalogSector[1];
			sector = catalogSector[2];
		}
		throw new DiskFullException("Unable to allocate more space for another file!");
	}

	/**
	 * Identify if additional directories can be created.  This
	 * may indicate that directories are not available to this
	 * operating system or simply that the disk image is "locked"
	 * to writing.
	 */
	public boolean canCreateDirectories() {
		return false;
	}
	
	/**
	 * Indicates if this disk image can create a file.
	 * If not, the reason may be as simple as it has not beem implemented
	 * to something specific about the disk.
	 */
	public boolean canCreateFile() {
		return true;
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
	 * Save the VTOC (Volume Table Of Contents) to disk.
	 */
	protected void writeVtoc(byte[] vtoc) {
		writeSector(CATALOG_TRACK, VTOC_SECTOR, vtoc);
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
		return true;
	}
	
	/**
	 * Identify if this disk format as not capable of having directories.
	 * @see com.webcodepro.applecommander.storage.Disk#hasDirectories()
	 */
	public boolean canHaveDirectories() {
		return false;
	}
	
	/**
	 * Indicates if this disk image can delete a file.
	 */
	public boolean canDeleteFile() {
		return true;
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
		int filesize = dosEntry.getSectorsUsed();
		byte[] fileData = null;
		if (filesize > 0) {
			fileData = new byte[(dosEntry.getSectorsUsed()-1) * SECTOR_SIZE];
		} else {
			fileData = new byte[0];
			// don't need to load it - also bypass potential issues
			return fileData;
		}
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
	 * Set the data associated with the specified DosFileEntry into sectors
	 * on the disk.
	 */
	protected void setFileData(DosFileEntry fileEntry, byte[] data) throws DiskFullException {
		// compute free space and see if the data will fit!
		int numberOfDataSectors = (data.length + SECTOR_SIZE - 1) / SECTOR_SIZE;
		int numberOfSectors = numberOfDataSectors + 
			(numberOfDataSectors + TRACK_SECTOR_PAIRS - 1) / TRACK_SECTOR_PAIRS;
		if (numberOfSectors > getFreeSectors() + fileEntry.getSectorsUsed()) {
			throw new DiskFullException("This file requires " + numberOfSectors
				+ " sectors but there are only " + getFreeSectors() + " sectors"
				+ " available on the disk.");
		}
		// free "old" data and just rewrite stuff...
		freeSectors(fileEntry);
		byte[] vtoc = readVtoc();
		int track = fileEntry.getTrack();
		int sector = fileEntry.getSector();
		if (track == 0 || track == 255) {
			track = 1;
			sector = 0;
			while (true) {
				if (isSectorFree(track,sector,vtoc)) {
					break;
				}
				sector++;
				if (sector >= getSectors()) {
					track++;
					sector = 0;
				}
			}
			fileEntry.setTrack(track);
			fileEntry.setSector(sector);
		}
		setSectorUsed(track, sector, vtoc);
		byte[] trackSectorList = new byte[SECTOR_SIZE];
		int offset = 0;
		int trackSectorOffset = 0x0c;
		int totalSectors = 0;
		int t=1;	// initial search for space
		int s=0;
		while (offset < data.length) {
			// locate next free sector
			while (true) {
				if (isSectorFree(t,s,vtoc)) {
					break;
				}
				s++;
				if (s >= getSectors()) {
					t++;
					s = 0;
				}
			}
			setSectorUsed(t,s,vtoc);
			if (trackSectorOffset >= 0x100) {
				// filled up the first track/sector list - save it
				trackSectorList[0x01] = (byte) t;
				trackSectorList[0x02] = (byte) s;
				writeSector(track, sector, trackSectorList);
				trackSectorList = new byte[SECTOR_SIZE];
				trackSectorOffset = 0x0c;
				track = t;
				sector = s;
			} else {
				// write out a data sector
				trackSectorList[trackSectorOffset] = (byte) t;
				trackSectorList[trackSectorOffset+1] = (byte) s;
				trackSectorOffset+= 2;
				byte[] sectorData = new byte[SECTOR_SIZE];
				int length = Math.min(SECTOR_SIZE, data.length - offset);
				System.arraycopy(data, offset, sectorData, 0, length);
				writeSector(t,s,sectorData);
				offset+= SECTOR_SIZE;
			}
			totalSectors++;
		}
		writeSector(track, sector, trackSectorList);	// last T/S list
		totalSectors++;
		fileEntry.setSectorsUsed(totalSectors);
		writeVtoc(vtoc);
	}
	
	/**
	 * Free sectors used by a DosFileEntry.
	 */
	protected void freeSectors(DosFileEntry dosFileEntry) {
		byte[] vtoc = readVtoc();
		int track = dosFileEntry.getTrack();
		if (track == 255) return;
		int sector = dosFileEntry.getSector();
		while (track != 0) {
			setSectorFree(track,sector,vtoc);
			byte[] trackSectorList = readSector(track, sector);
			track = AppleUtil.getUnsignedByte(trackSectorList[0x01]);
			sector = AppleUtil.getUnsignedByte(trackSectorList[0x02]);
			for (int i=0x0c; i<0x100; i+=2) {
				int t = AppleUtil.getUnsignedByte(trackSectorList[i]);
				if (t == 0) break;
				int s = AppleUtil.getUnsignedByte(trackSectorList[i+1]);
				setSectorFree(t,s,vtoc);
			}
		}
		writeVtoc(vtoc);
	}

	/**
	 * Format the disk as DOS 3.3.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#format()
	 */
	public void format() {
		getImageOrder().format();
		format(15, 35, 16);
	}
	
	/**
	 * Format the disk as DOS 3.3 given the dymanic parameters.
	 * (Used for UniDOS and OzDOS.)
	 */
	protected void format(int firstCatalogSector, int tracksPerDisk,
		int sectorsPerTrack) {
			
		writeBootCode();
		// create catalog sectors
		byte[] data = new byte[SECTOR_SIZE];
		for (int sector=firstCatalogSector; sector > 0; sector--) {
			if (sector > 1) {
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
		data[0x02] = (byte)firstCatalogSector;	// sector# of first catalog sector
		data[0x03] = 3;				// DOS 3.3 formatted
		data[0x06] = (byte)254;	// DISK VOLUME#
		data[0x27] = TRACK_SECTOR_PAIRS;// maximum # of T/S pairs in a sector
		data[0x30] = CATALOG_TRACK+1;	// last track where sectors allocated
		data[0x31] = 1;				// direction of allocation
		data[0x34] = (byte)tracksPerDisk;	// tracks per disk
		data[0x35] = (byte)sectorsPerTrack;// sectors per track
		data[0x37] = 1;				// 36/37 are # of bytes per sector
		for (int track=0; track<tracksPerDisk; track++) {
			for (int sector=0; sector<sectorsPerTrack; sector++) {
				if (track == 0 || track == CATALOG_TRACK) {
					setSectorUsed(track, sector, data);
				} else {
					setSectorFree(track, sector, data);
				}
			}
		}
		writeVtoc(data);
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
		int trackOffset = track * 4;
		int sectorOffset = 1 - ((sector & 0x8) >> 3);
		return 0x38 + trackOffset + sectorOffset;
	}
	
	/**
	 * Compute the VTOC bit for the T/S map.
	 */
	protected int getFreeMapBit(int sector) {
		int bit = sector & 0x7;
		return bit;
	}
	
	/**
	 * Validate track/sector range.  This just validates the
	 * maximum values allowable for track and sector. 
	 */
	protected void checkRange(int track, int sector) {
		if (track > 50 || sector > 32) {
			throw new IllegalArgumentException(
				"Invalid track (" + track + "), sector (" + sector
				+ ") combination.");
		}
	}

	/**
	 * Returns the logical disk number.  Returns a 0 to indicate no numbering.
	 */
	public int getLogicalDiskNumber() {
		return 0;
	}

	/**
	 * Returns a valid filename for the given filename.  DOS 3.3
	 * pretty much allows anything - so it is cut to 30 characters
	 * and trimmed (trailing whitespace may cause confusion).
	 */
	public String getSuggestedFilename(String filename) {
		int len = Math.min(filename.length(), 30);
		return filename.toUpperCase().substring(0, len).trim();
	}

	/**
	 * Returns a valid filetype for the given filename.  The most simple
	 * format will just assume a filetype of binary.  This method is
	 * available for the interface to make an intelligent first guess
	 * as to the filetype.
	 */
	public String getSuggestedFiletype(String filename) {
		String filetype = "B";
		int pos = filename.lastIndexOf(".");
		if (pos > 0) {
			String what = filename.substring(pos+1);
			if ("txt".equalsIgnoreCase(what)) {
				filetype = "T";
			}
		}
		return filetype;
	}

	/**
	 * Returns a list of possible file types.  Since the filetype is
	 * specific to each operating system, a simple String is used.
	 */
	public String[] getFiletypes() {
		return filetypes;
	}

	/**
	 * Indicates if this filetype requires an address component.
	 * For DOS, only the Binary type needs an address.
	 */
	public boolean needsAddress(String filetype) {
		return "B".equals(filetype);
	}

	/**
	 * Indicates if this FormattedDisk supports a disk map.
	 */	
	public boolean supportsDiskMap() {
		return true;
	}
}
