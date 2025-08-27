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
package com.webcodepro.applecommander.storage.os.rdos;

import com.webcodepro.applecommander.storage.*;
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import com.webcodepro.applecommander.util.AppleUtil;
import com.webcodepro.applecommander.util.TextBundle;
import static com.webcodepro.applecommander.storage.DiskConstants.*;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * Manages a disk that is in the RDOS format.
 * <p>
 * Note that the RDOS 2.1/3.2 block interleave is different than the standard DOS 3.3 format.
 * Thus, when the image is made, the sectors are skewed differently - use readRdosBlock
 * to read the appropriate block number.
 * <p>
 * Also note that the operating system is itself the first file.  Block #0 is really 
 * track 0, sector 0 - meaning that the first file should not (cannot) be deleted.
 * <p>
 * RDOS 2.1/3.2 was placed on 13 sector disks.  This limits the number of blocks
 * to 455.  It also may also cause incompatibilities with other formats and other cracks.
 * <p>
 * Date created: Oct 7, 2002 2:03:58 PM
 * @author Rob Greene
 */
public class RdosFormatDisk extends FormattedDiskX {
	private TextBundle textBundle = StorageBundle.getInstance();
	/**
	 * RDOS 2.1/3.2 disks are structured in a different order than DOS 3.3.
	 * This table interpolates between the RDOS ordering and the DOS
	 * ordering.  It appears that RDOS may use the physical sector number
	 * instead of the logical sector.
	 */
	public static final int[] sectorSkew = {
		0, 7, 0x0e, 6, 0x0d, 5, 0x0c, 4,
		0x0b, 3, 0x0a, 2, 9, 1, 8, 0x0f 
		};
	/**
	 * Specifies the length of a file entry.
	 */
	public static final int ENTRY_LENGTH = 0x20;
	/**
	 * Specifies the number of tracks on the disk.  
	 * RDOS apparantly only worked on 5.25" disks.
	 */
	public static final int TRACKS_ON_DISK = 35;
	/**
	 * Number of sectors used by catalog.
	 * FIXME: some sources say 10, others say 11. RDOS 3.3 may support 16.
	 */
	public static final int CATALOG_SECTORS = 10;
	/**
	 * The known filetypes for a RDOS disk.
	 */
	public static final String[] FILE_TYPES = { "B", "A", "T" };
	private static final Map<String,String> FILE_TYPE_MAPPING = Map.of(
			"T", "TXT",
			"A", "BAS",
			"B", "BIN"
		);

    private final int sectorsPerTrack;

	/**
	 * 13 sectors for RDOS 2.1/3.2, native sectoring (16) for RDOS 3.3
	 */
	private int SectorsPerTrack() {
        return sectorsPerTrack;
	}

	/**
	 * 455 blocks for RDOS 2.1/3.2, 560 for RDOS 3.3
	 */
	private int BlocksOnDisk() {
		return TRACKS_ON_DISK * SectorsPerTrack();
	}

	/**
	 * Use this inner interface for managing the disk usage data.
	 * This off-loads format-specific implementation to the implementing class.
	 * A BitSet is used to track all blocks, as RDOS disks do not have a
	 * bitmap stored on the disk. This is safe since we know the number of blocks
	 * that exist. (BitSet length is of last set bit - unset bits at the end are
	 * "lost".)
	 * <p>
	 * Note one really unique point about RDOS - the entire disk is mapped out
	 * by the file entries.  There are no blocks marked off, by default, by the
	 * operating system.  However, the first file (RDOS itself) starts on block
	 * 0 (track 0, sector 0) and runs for 26 blocks - which covers all of track 0
	 * (the operating system) and the 10 sectors used for file entries.
	 */
	private class RdosDiskUsage implements DiskUsage {
		private int location = -1;
		private BitSet bitmap = null;
		public boolean hasNext() {
			return location == -1 || location < BlocksOnDisk() - 1;
		}
		public void next() {
			if (bitmap == null) {
				bitmap = new BitSet(BlocksOnDisk());
				// mark all blocks as unused
				for (int b=0; b<BlocksOnDisk(); b++) {
					bitmap.set(b);
				}
				// for each file, mark the blocks used
				for (FileEntry fileEntry : getFiles()) {
					if (!fileEntry.isDeleted()) {
					    RdosFileEntry entry = (RdosFileEntry) fileEntry;
						for (int b=0; b<entry.getSizeInBlocks(); b++) {
							bitmap.clear(entry.getStartingBlock()+b);
						}
					}
				}
				location = 0;
			} else {
				location++;
			}
		}
		public boolean isFree() {
			return bitmap.get(location);	// true = free
		}
		public boolean isUsed() {
			return !bitmap.get(location);	// false = used
		}
	}
	
	/**
	 * Constructor for RdosFormatDisk.
	 */
	public RdosFormatDisk(String filename, ImageOrder imageOrder, int sectorsPerTrack) {
		super(filename, imageOrder);
        this.sectorsPerTrack = sectorsPerTrack;
	}
	
	/**
	 * Create a RdosFormatDisk.
	 */
	public static RdosFormatDisk[] create(String filename, ImageOrder imageOrder) {
		RdosFormatDisk disk = new RdosFormatDisk(filename, imageOrder, 16);
		disk.format();
		return new RdosFormatDisk[] { disk };
	}

	/**
	 * Read an RDOS block.  The sector skewing for RDOS 2.1/3.2 is different.
	 * This routine will convert the block number to a DOS track and sector,
	 * handling the sector change-over.  The readSector method then should
	 * take care of various image formats.
	 * <p>
	 * Note that sectorSkew has the full 16 sectors, even though RDOS 2.1/3.2
	 * itself is a 13 sector format.
	 */
	public byte[] readRdosBlock(int block) {
		int s = SectorsPerTrack();
		int track = block / s;
		int sector = block % s;
		if (s == 13) {
			sector = sectorSkew[sector];
		}
		return readSector(track, sector);
	}
	
	/**
	 * Write an RDOS block.  The sector skewing for RDOS2.1/3/2 is different.
	 * This routine will convert the block number to a DOS track and sector,
	 * handling the sector change-over.  The writeSector method then should
	 * take care of various image formats.
	 * <p>
	 * Note that sectorSkew has the full 16 sectors, even though RDOS
	 * itself is a 13 sector format.
	 */
	public void writeRdosBlock(int block, byte[] data) {
		int s = SectorsPerTrack();
		int track = block / s;
		int sector = block % s;
		if (s == 13) {
			sector = sectorSkew[sector];
		}
		writeSector(track, sector, data);
	}

	/**
	 * RDOS really does not have a disk name.  Fake one.
	 */
	public String getDiskName() {
		if (SectorsPerTrack() == 13) {
			/* Use the comment/tag added in the 13->16 sector conversion */
			byte[] block = readRdosBlock(4);
			return AppleUtil.getString(block, 0xe0, 0x20);
		} else {
			/* Use the name of the OS (catalog entry zero) */
			byte[] block = readSector(1, 0x0);
			return AppleUtil.getString(block, 0x0, 0x18);
		}
	}

	/**
	 * Retrieve a list of files.
	 */
	public List<FileEntry> getFiles() {
		List<FileEntry> files = new ArrayList<>();
		for (int b=0; b<CATALOG_SECTORS; b++) {
			byte[] data = readRdosBlock(b + SectorsPerTrack());
			for (int i=0; i<data.length; i+= ENTRY_LENGTH) {
				byte[] entry = new byte[ENTRY_LENGTH];
				System.arraycopy(data, i, entry, 0, entry.length);
				if (AppleUtil.getUnsignedByte(entry[0]) != 0) {
					RdosFileEntry fileEntry = new RdosFileEntry(entry, this);
					files.add(fileEntry);
				}
			}
		}
		return files;
	}

	/**
	 * Create a new FileEntry.
	 */
	public FileEntry createFile() throws DiskFullException {
		throw new DiskFullException(textBundle.get("FileCreationNotSupported"), this.getFilename()); //$NON-NLS-1$
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
		return false;		// FIXME - need to implement
	}

	/**
	 * Identify the operating system format of this disk.
	 */
	public String getFormat() {
		if (SectorsPerTrack() == 13) {
			return textBundle.get("RdosFormatDisk.Rdos21"); //$NON-NLS-1$
		} else {
			return textBundle.get("RdosFormatDisk.Rdos33"); //$NON-NLS-1$
		}
	}
	
	/**
	 * Return the number of free blocks.
	 */
	public int getFreeBlocks() {
		return BlocksOnDisk() - getUsedBlocks();
	}
	
	/**
	 * Return the number of used blocks.
	 */
	public int getUsedBlocks() {
		int used = 0;
		for (FileEntry fileEntry : getFiles()) {
		    RdosFileEntry entry = (RdosFileEntry) fileEntry;
			if (!fileEntry.isDeleted()) used+= entry.getSizeInBlocks();
		}
		return used;
	}

	/**
	 * Return the amount of free space in bytes.
	 */
	public int getFreeSpace() {
		return getFreeBlocks() * SECTOR_SIZE;
	}

	/**
	 * Return the amount of used space in bytes.
	 */
	public int getUsedSpace() {
		return getUsedBlocks() * SECTOR_SIZE;
	}

	/**
	 * Get suggested dimensions for display of bitmap.
	 * Since RDOS uses blocks, a null is returned indicating no suggestions.
	 */
	public int[] getBitmapDimensions() {
		return null;
	}

	/**
	 * Get the length of the bitmap.
	 */
	public int getBitmapLength() {
		return BlocksOnDisk();
	}
	
	/**
	 * Get the disk usage iterator.
	 */
	public DiskUsage getDiskUsage() {
		return new RdosDiskUsage();
	}

	/**
	 * Get the labels to use in the bitmap.
	 */
	public String[] getBitmapLabels() {
		return new String[] { textBundle.get("Block") }; //$NON-NLS-1$
	}
	
	/**
	 * Get Pascal-specific disk information.
	 */
	public List<DiskInformation> getDiskInformation() {
		List<DiskInformation> list = super.getDiskInformation();
		list.add(new DiskInformation(textBundle.get("TotalBlocks"), BlocksOnDisk())); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("FreeBlocks"), getFreeBlocks())); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("UsedBlocks"), getUsedBlocks())); //$NON-NLS-1$
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
				list.add(new FileColumnHeader(textBundle.get("Type"), 1,
                        FileColumnHeader.ALIGN_CENTER, "type"));
				list.add(new FileColumnHeader(textBundle.get("Blocks"), 3,
                        FileColumnHeader.ALIGN_RIGHT, "blocks"));
				list.add(new FileColumnHeader(textBundle.get("Name"), 24,
                        FileColumnHeader.ALIGN_LEFT, "name"));
				list.add(new FileColumnHeader(textBundle.get("RdosFormatDisk.Size"), 6,
                        FileColumnHeader.ALIGN_RIGHT, "size"));
				list.add(new FileColumnHeader(textBundle.get("RdosFormatDisk.StartingBlock"), 3,
                        FileColumnHeader.ALIGN_RIGHT, "firstBlock"));
				break;
			case FILE_DISPLAY_DETAIL:
				list.add(new FileColumnHeader(textBundle.get("Type"), 1,
                        FileColumnHeader.ALIGN_CENTER, "type"));
				list.add(new FileColumnHeader(textBundle.get("Blocks"), 3,
                        FileColumnHeader.ALIGN_RIGHT, "blocks"));
				list.add(new FileColumnHeader(textBundle.get("Name"), 24,
                        FileColumnHeader.ALIGN_LEFT, "name"));
				list.add(new FileColumnHeader(textBundle.get("RdosFormatDisk.Size"), 6,
                        FileColumnHeader.ALIGN_RIGHT, "size"));
				list.add(new FileColumnHeader(textBundle.get("RdosFormatDisk.StartingBlock"), 3,
                        FileColumnHeader.ALIGN_RIGHT, "firstBlock"));
				list.add(new FileColumnHeader(textBundle.get("RdosFormatDisk.Address"), 5,
                        FileColumnHeader.ALIGN_RIGHT, "address"));
				list.add(new FileColumnHeader(textBundle.get("DeletedQ"), 7,
                        FileColumnHeader.ALIGN_CENTER, "deleted"));
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
	 * RDOS dos not support directories.
	 */
	public boolean canHaveDirectories() {
		return false;
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
		if ( !(fileEntry instanceof RdosFileEntry)) {
			throw new IllegalArgumentException(textBundle.get("RdosFormatDisk.IncorrectFileEntryError")); //$NON-NLS-1$
		}
		RdosFileEntry rdosEntry = (RdosFileEntry) fileEntry;
		int startingBlock = rdosEntry.getStartingBlock();
		byte[] fileData = new byte[rdosEntry.getSizeInBlocks() * SECTOR_SIZE];
		int offset = 0;
		for (int blockOffset = 0; blockOffset < rdosEntry.getSizeInBlocks(); blockOffset++) {
			byte[] blockData = readRdosBlock(startingBlock + blockOffset);
			System.arraycopy(blockData, 0, fileData, offset, blockData.length);
			offset+= blockData.length;
		}
		return fileData;
	}
	
	/**
	 * Format the disk as an RDOS disk.
	 * FIXME - RDOS does not "like" an AppleCommander formatted disk.
	 *         This appears to be because the &amp;CAT command
	 *         reads from track 1 sector 9 (whatever RDOS block that
	 *         would be) and executes that code for the directory.
	 *         AppleCommander will need to either clone the code or write
	 *         its own routine.  This is RDOS block #25.
	 * FIXME - Doesn't handle native 16-sector (RDOS 3.3) format.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#format()
	 */
	public void format() {
		getImageOrder().format();
		writeBootCode();
		// minor hack - ensure that AppleCommander itself recognizes the
		// RDOS disk!
		byte[] block = readSector(0, 0x0d);
		AppleUtil.setString(block, 0xe0, textBundle.get("RdosFormatDisk.IdentifierText"), 0x20); //$NON-NLS-1$
		writeSector(0, 0x0d, block);
		// a hack - until real code goes here.
		block = new byte[256];
		block[0] = 0x60;
		writeSector(1, 9, block);
		// write the first directory entry
		// FIXME - this should use FileEntry!
		byte[] data = readRdosBlock(13);
		AppleUtil.setString(data, 0x00, textBundle.get("RdosFormatDisk.InitialSystemFile"), 0x18); //$NON-NLS-1$
		AppleUtil.setString(data, 0x18, "B", 0x01); //$NON-NLS-1$
		data[0x19] = 26;
		AppleUtil.setWordValue(data, 0x1a, 0x1000);
		AppleUtil.setWordValue(data, 0x1c, 6656);
		AppleUtil.setWordValue(data, 0x1e, 0);
		writeRdosBlock(13, data);
	}

	/**
	 * Returns the logical disk number.  Returns a 0 to indicate no numbering.
	 */
	public int getLogicalDiskNumber() {
		return 0;
	}

	/**
	 * Returns a valid filename for the given filename.  RDOS
	 * pretty much allows anything - so it is cut to 24 characters
	 * and trimmed (trailing whitespace may cause confusion).
	 */
	public String getSuggestedFilename(String filename) {
		int len = Math.min(filename.length(), 24);
		return filename.toUpperCase().substring(0, len).trim();
	}

	/**
	 * Returns a valid filetype for the given filename.  The most simple
	 * format will just assume a filetype of binary.  This method is
	 * available for the interface to make an intelligent first guess
	 * as to the filetype.
	 */
	public String getSuggestedFiletype(String filename) {
		String filetype = "B"; //$NON-NLS-1$
		int pos = filename.lastIndexOf("."); //$NON-NLS-1$
		if (pos > 0) {
			String what = filename.substring(pos+1);
			if ("txt".equalsIgnoreCase(what)) { //$NON-NLS-1$
				filetype = "T"; //$NON-NLS-1$
			}
		}
		return filetype;
	}

	/**
	 * Returns a list of possible file types.  Since the filetype is
	 * specific to each operating system, a simple String is used.
	 */
	public String[] getFiletypes() {
		return FILE_TYPES;
	}

	/**
	 * Indicates if this filetype requires an address component.
	 * Presumably, the "B" filetype is binary and will need an
	 * address.
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
	 * Change to a different ImageOrder.  Remains in RDOS format but
	 * the underlying order can change.
	 * @see ImageOrder
	 */
	public void changeImageOrder(ImageOrder imageOrder) {
		AppleUtil.changeImageOrderByTrackAndSector(getImageOrder(), imageOrder);
		setImageOrder(imageOrder);
	}

	/**
	 * Writes the raw bytes into the file.  This bypasses any special formatting
	 * of the data (such as prepending the data with a length and/or an address).
	 * Typically, the FileEntry.setFileData method should be used. 
	 */
	public void setFileData(FileEntry fileEntry, byte[] fileData) throws DiskFullException {
		// TODO implement  setFileData
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
		return FILE_TYPE_MAPPING.entrySet()
				.stream()
				.filter(e -> prodosFiletype.equalsIgnoreCase(e.getKey()) || prodosFiletype.equalsIgnoreCase(e.getValue()))
				.map(Map.Entry::getKey)
				.findFirst()
				.orElse("B");
	}
	/**
	 * Provides conversation to a given ProDOS file type since as it is common across
	 * many archiving tools.
	 */
	@Override
	public String toProdosFiletype(String nativeFiletype) {
		return FILE_TYPE_MAPPING.getOrDefault(nativeFiletype, "BIN");
	}
}
