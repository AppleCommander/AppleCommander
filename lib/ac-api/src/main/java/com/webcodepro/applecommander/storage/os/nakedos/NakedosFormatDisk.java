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
package com.webcodepro.applecommander.storage.os.nakedos;

import com.webcodepro.applecommander.storage.*;
import com.webcodepro.applecommander.util.AppleUtil;
import com.webcodepro.applecommander.util.TextBundle;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.source.Source;
import org.applecommander.util.Container;

import static com.webcodepro.applecommander.storage.DiskConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages a disk that is in NakedOS format.
 * <p>
 * Date created: August 5, 2010 10:23:23 AM
 * @author David Schmidt
 */
public class NakedosFormatDisk extends FormattedDisk {
	private TextBundle textBundle = StorageBundle.getInstance();
	/**
	 * Indicates the index of the track in the location array.
	 */	
	public static final int TRACK_LOCATION_INDEX = 0;
	/**
	 * Indicates the index of the sector in the location array.
	 */	
	public static final int SECTOR_LOCATION_INDEX = 1;
	/**
	 * The catalog track.
	 */
	public static final int CATALOG_TRACK = 0;
	/**
	 * The VTOC sector.
     * NOTE: Documented as sectors A..C but is really 9..B (https://bitbucket.org/martin.haye/super-mon/src/master/)
	 */
	public static final int VTOC_SECTOR = 9;
	/**
	 * The list of filetypes available.
	 */
	private static final String[] filetypes = { 
			"B" //$NON-NLS-1$
		};
	/**
	 * The number of sectors used on the disk
	 */
	private int usedSectors = 0;

	/**
	 * Use this inner interface for managing the disk usage data.
	 * This off-loads format-specific implementation to the implementing class.
	 */
	private class NakedosDiskUsage implements DiskUsage {
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
		 * I don't think there is a map stored on disk, however.
		 */
		public boolean isFree() {
			if (location == null || location.length != 2) {
				throw new IllegalArgumentException(StorageBundle.getInstance()
						.get("DosFormatDisk.InvalidDimensionError")); //$NON-NLS-1$
			}
			return false;
		}
		public boolean isUsed() {
			return !isFree();
		}
	}

    private TrackSectorDevice device;

	/**)
	 * Constructor for NakedosFormatDisk.
	 */
	public NakedosFormatDisk(String filename, TrackSectorDevice device) {
		super(filename, device.get(Source.class).orElseThrow());
        this.device = device;
	}

	/**
	 * Create a NakedosFormatDisk.  All DOS disk images are expected to
	 * be 140K in size.
	 */
	public static NakedosFormatDisk[] create(String filename, TrackSectorDevice device) {
		NakedosFormatDisk disk = new NakedosFormatDisk(filename, device);
		disk.format();
		return new NakedosFormatDisk[] { disk };
	}

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, device);
    }

    /**
	 * Identify the operating system format of this disk as Nakedos.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getFormat()
	 */
	public String getFormat() {
		return textBundle.get("NakedOS"); //$NON-NLS-1$
	}

	/**
	 * Retrieve a list of files.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getFiles()
	 */
	public List<FileEntry> getFiles() {
		ArrayList<FileEntry> list = new ArrayList<>();
		int totalUsed = 0;
		int i;
		int[] fileSizes = new int[256];
		byte[] catalogSector1 = device.readSector(CATALOG_TRACK, VTOC_SECTOR).asBytes();
		byte[] catalogSector2 = device.readSector(CATALOG_TRACK, VTOC_SECTOR+1).asBytes();
		byte[] catalogSector3 = device.readSector(CATALOG_TRACK, VTOC_SECTOR+2).asBytes();
		for (i = 0;i<48;i++) {
			if ((catalogSector1[i+0xd0] != -2) && (catalogSector1[i+0xd0] != -1))
				fileSizes[AppleUtil.getUnsignedByte(catalogSector1[i+0xd0])]+=1;
			if (catalogSector1[i+0xd0] != -1)
				totalUsed++;
		}
		for (i = 0;i<256;i++) {
			if ((catalogSector2[i] != -2) && (catalogSector2[i] != -1))
				fileSizes[AppleUtil.getUnsignedByte(catalogSector2[i])]+=1;
			if ((catalogSector3[i] != -2) && (catalogSector3[i] != -1))
				fileSizes[AppleUtil.getUnsignedByte(catalogSector3[i])]+=1;
			if (catalogSector2[i] != -1)
				totalUsed++;
			if (catalogSector3[i] != -1)
				totalUsed++;
		}
		for (i = 0;i<256;i++) {
			if (fileSizes[i] != 0) {
				list.add(new NakedosFileEntry(this, i,fileSizes[i]));
			}
		}
		usedSectors = totalUsed;
		return list;
	}

	/**
	 * Create a FileEntry.
	 */
	public FileEntry createFile() throws DiskFullException {
		return new NakedosFileEntry(this, -2, 0);
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
	 * If not, the reason may be as simple as it has not been implemented
	 * to something specific about the disk.
	 */
	public boolean canCreateFile() {
		return false;
	}

	/**
	 * Compute the amount of freespace available on the disk.
	 * This algorithm completely ignores tracks and sectors by
	 * running through the entire bitmap stored on the VTOC.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getFreeSpace()
	 */
	public int getFreeSpace() {
		return getFreeSectors() * SECTOR_SIZE;
	}
	
	/**
	 * Compute the number of free sectors available on the disk.
	 */
	public int getFreeSectors() {
		return getTotalSectors() - getUsedSectors();
	}

	/**
	 * Return the amount of used space in bytes.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getUsedSpace()
	 */
	public int getUsedSpace() {
		return usedSectors * 256;
	}
	
	/**
	 * Compute the number of used sectors on the disk.
	 */
	public int getUsedSectors() {
		return usedSectors;
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
	 * Return the disk name.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getDiskName()
	 */
	public String getDiskName() {
	    // Pull the disk name out...
		return "";
	}

	/**
	 * Get the disk usage iterator.
	 */
	public DiskUsage getDiskUsage() {
		return new NakedosDiskUsage();
	}

	/**
	 * Get the number of tracks on this disk.
	 */
	public int getTracks() {
		return 35;
	}

	/**
	 * Get the number of sectors on this disk.
	 */
	public int getSectors() {
		return 16;
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
		return new String[] { textBundle.get("DosFormatDisk.Track"), textBundle.get("DosFormatDisk.Sector") }; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * Get NakedOS-specific disk information.
	 */
	public List<DiskInformation> getDiskInformation() {
		getFiles();
		List<DiskInformation> list = super.getDiskInformation();
		return list;
	}

	/**
	 * Get the standard file column header information.
	 * This default implementation is intended only for standard mode.
	 */
	public List<FileColumnHeader> getFileColumnHeaders(int displayMode) {
		List<FileColumnHeader> list = new ArrayList<>();
		switch (displayMode) {
			case FILE_DISPLAY_NATIVE:
				list.add(new FileColumnHeader(" ", 1, FileColumnHeader.ALIGN_CENTER, "locked"));
				list.add(new FileColumnHeader(textBundle.get("DosFormatDisk.Type"), 1,
                        FileColumnHeader.ALIGN_CENTER, "type"));
				list.add(new FileColumnHeader(textBundle.get("DosFormatDisk.SizeInSectors"), 3,
                        FileColumnHeader.ALIGN_RIGHT, "sectors"));
				list.add(new FileColumnHeader(textBundle.get("Name"), 30,
                        FileColumnHeader.ALIGN_LEFT, "name"));
				break;
			case FILE_DISPLAY_DETAIL:
				list.add(new FileColumnHeader(" ", 1, FileColumnHeader.ALIGN_CENTER, "locked"));
				list.add(new FileColumnHeader(textBundle.get("DosFormatDisk.Type"), 1,
                        FileColumnHeader.ALIGN_CENTER, "type"));
				list.add(new FileColumnHeader(textBundle.get("Name"), 30,
                        FileColumnHeader.ALIGN_LEFT, "name"));
				list.add(new FileColumnHeader(textBundle.get("SizeInBytes"), 6,
                        FileColumnHeader.ALIGN_RIGHT, "size"));
				list.add(new FileColumnHeader(textBundle.get("DosFormatDisk.SizeInSectors"), 3,
                        FileColumnHeader.ALIGN_RIGHT, "sectors"));
				list.add(new FileColumnHeader(textBundle.get("DeletedQ"), 7,
                        FileColumnHeader.ALIGN_CENTER, "deleted"));
				list.add(new FileColumnHeader(textBundle.get("DosFormatDisk.TrackAndSectorList"), 7,
                        FileColumnHeader.ALIGN_CENTER, "trackAndSectorList"));
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
		return false;
	}
	
	/**
	 * Identify if this disk format as not capable of having directories.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#canHaveDirectories()
	 */
	public boolean canHaveDirectories() {
		return false;
	}
	
	/**
	 * Indicates if this disk image can delete a file.
	 */
	public boolean canDeleteFile() {
		return false;
	}

	/**
	 * Get the data associated with the specified FileEntry.
	 */
	public byte[] getFileData(FileEntry fileEntry) {
		if ( !(fileEntry instanceof NakedosFileEntry)) {
			throw new IllegalArgumentException(textBundle.get("DosFormatDisk.InvalidFileEntryError")); //$NON-NLS-1$
		}
		int offset = 0;
		byte[] catalogSector1 = device.readSector(CATALOG_TRACK, VTOC_SECTOR).asBytes();
		byte[] catalogSector2 = device.readSector(CATALOG_TRACK, VTOC_SECTOR+1).asBytes();
		byte[] catalogSector3 = device.readSector(CATALOG_TRACK, VTOC_SECTOR+2).asBytes();
		NakedosFileEntry entry = (NakedosFileEntry) fileEntry;
		byte[] fileData = new byte[entry.getSize()];
		for (int i = 0;i<48;i++) {
			if (AppleUtil.getUnsignedByte(catalogSector1[i+0xd0]) == entry.getFileNumber()) {
				byte[] fileData1 = device.readSector(i/16,i%16).asBytes();
				System.arraycopy(fileData1, 0, fileData, offset, fileData1.length);
				offset+=fileData1.length;
			}
		}
		for (int i = 0;i<256;i++) {
			if (AppleUtil.getUnsignedByte(catalogSector2[i]) == entry.getFileNumber()) {
				byte[] fileData1 = device.readSector((i+48)/16,(i+48)%16).asBytes();
				System.arraycopy(fileData1, 0, fileData, offset, fileData1.length);
				offset+=fileData1.length;
			}
		}
		for (int i = 0;i<256;i++) {
			if (AppleUtil.getUnsignedByte(catalogSector3[i]) == entry.getFileNumber()) {
				byte[] fileData1 = device.readSector((i+48+256)/16,(i+48+256)%16).asBytes();
				System.arraycopy(fileData1, 0, fileData, offset, fileData1.length);
				offset+=fileData1.length;
			}
		}
		return fileData;
	}

	/**
	 * Writes the raw bytes into the file.  This bypasses any special formatting
	 * of the data (such as prepending the data with a length and/or an address).
	 * Typically, the FileEntry.setFileData method should be used. 
	 */
	public void setFileData(FileEntry fileEntry, byte[] fileData) throws DiskFullException {
		setFileData((NakedosFileEntry)fileEntry, fileData);
	}
	
	/**
	 * Set the data associated with the specified NakedosFileEntry into sectors
	 * on the disk.
	 */
	protected void setFileData(NakedosFileEntry fileEntry, byte[] data) throws DiskFullException {
		// compute free space and see if the data will fit!
	}
	
	/**
	 * Format the disk as DOS 3.3.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#format()
	 */
	public void format() {
        // TODO
	}

	/**
	 * Format the disk as NakedOS.
	 */
	protected void format(int tracksPerDisk, int sectorsPerTrack) {
		// TODO: get an image of a "blank" NakedOS disk and capture the boot code
	}

	/**
	 * Returns the logical disk number.  Returns a 0 to indicate no numbering.
	 */
	public int getLogicalDiskNumber() {
		return 0;
	}

	/**
	 * Returns a valid filename for the given filename.
	 */
	public String getSuggestedFilename(String filename) {
		int len = Math.min(filename.length(), 2);
		return filename.toUpperCase().substring(0, len).trim();
	}

	/**
	 * Returns a valid filetype for the given filename.  The most simple
	 * format will just assume a filetype of binary.  This method is
	 * available for the interface to make an intelligent first guess
	 * as to the filetype.
	 */
	public String getSuggestedFiletype(String filename) {
		return "B"; //$NON-NLS-1$
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
		return "B".equals(filetype); //$NON-NLS-1$
	}

	/**
	 * Indicates if this FormattedDisk supports a disk map.
	 */	
	public boolean supportsDiskMap() {
		return true;
	}

	/**
	 * Create a new DirectoryEntry.
	 * @see com.webcodepro.applecommander.storage.DirectoryEntry#createDirectory(String)
	 */
	public DirectoryEntry createDirectory(String name) throws DiskFullException	{
		throw new UnsupportedOperationException(textBundle.get("DirectoryCreationNotSupported")); //$NON-NLS-1$
	}

    /**
     * Gives an indication on how this disk's geometry should be handled.
     */
    public DiskGeometry getDiskGeometry() {
        return DiskGeometry.TRACK_SECTOR;
    }

	/**
	 * Provides conversation from a given ProDOS file type since as it is common across
	 * many archiving tools.
	 */
	@Override
	public String toNativeFiletype(String prodosFiletype) {
		return "B";
	}
	/**
	 * Provides conversation to a given ProDOS file type since as it is common across
	 * many archiving tools.
	 */
	@Override
	public String toProdosFiletype(String nativeFiletype) {
		return "BIN";
	}
}
