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
import java.util.Iterator;
import java.util.List;

/**
 * Manages a disk that is in the RDOS format.
 * <p>
 * Note that the RDOS block interleave is different than the standard DOS 3.3 format.
 * Thus, when the image is made, the sectors are skewed differently - use readRdosBlock
 * to read the appropriate block number.
 * <p>
 * Also note that the operating system is itself the first file.  Block #0 is really 
 * track 0, sector 0 - meaning that the first file should not (cannot) be deleted.
 * <p>
 * RDOS appears to have been placed on 13 sector disks.  This limits the number of blocks
 * to 455.  It also may also cause incompatibilities with other formats and other cracks.
 * <p>
 * Date created: Oct 7, 2002 2:03:58 PM
 * @author: Rob Greene
 */
public class RdosFormatDisk extends FormattedDisk {
	/**
	 * Specifies the length of a file entry.
	 */
	public static final int ENTRY_LENGTH = 0x20;
	/**
	 * Specifies the number of blocks on the disk.  
	 * RDOS apparantly only worked on 5.25" disks.
	 */
	public static final int BLOCKS_ON_DISK = 455;

	/**
	 * Use this inner interface for managing the disk usage data.
	 * This offloads format-specific implementation to the implementing class.
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
			return location == -1 || location < BLOCKS_ON_DISK - 1;
		}
		public void next() {
			if (bitmap == null) {
				bitmap = new BitSet(BLOCKS_ON_DISK);
				// mark all blocks as unused
				for (int b=0; b<BLOCKS_ON_DISK; b++) {
					bitmap.set(b);
				}
				// for each file, mark the blocks used
				Iterator files = getFiles().iterator();
				while (files.hasNext()) {
					RdosFileEntry fileEntry = (RdosFileEntry) files.next();
					if (!fileEntry.isDeleted()) {
						for (int b=0; b<fileEntry.getSizeInBlocks(); b++) {
							bitmap.clear(fileEntry.getStartingBlock()+b);
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
	 * @param filename
	 * @param diskImage
	 */
	public RdosFormatDisk(String filename, byte[] diskImage) {
		super(filename, diskImage);
	}
	
	/**
	 * Read an RDOS block.  The sector skewing for RDOS seems to be different.
	 * This routine will convert the block number to a DOS track and sector,
	 * handling the sector change-over.  The readSector method then should
	 * take care of various image formats.
	 * <p>
	 * Note that sectorSkew has the full 16 sectors, even though RDOS
	 * itself is a 13 sector format.
	 */
	public byte[] readRdosBlock(int block) {
		int sectorSkew[] = { 0, 7, 0x0e, 6, 0x0d, 5, 0x0c, 4,
							0x0b, 3, 0x0a, 2, 9, 1, 8, 0x0f };
		int track = block / 13;
		int sector = sectorSkew[block % 13];
		return readSector(track, sector);
	}

	/**
	 * RDOS dos not support directories.
	 */
	public boolean canHaveDirectories() {
		return false;
	}

	/**
	 * RDOS really does not have a disk name.  Fake one.
	 */
	public String getDiskName() {
		byte[] block = readRdosBlock(4);
		return AppleUtil.getString(block, 0xe0, 0x20);
	}

	/**
	 * Retrieve a list of files.
	 */
	public List getFiles() {
		List files = new ArrayList();
		for (int b=13; b<23; b++) {
			byte[] data = readRdosBlock(b);
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
	 * Identify the operating system format of this disk.
	 */
	public String getFormat() {
		return "RDOS 2.1";
	}
	
	/**
	 * Return the number of free blocks.
	 */
	public int getFreeBlocks() {
		return BLOCKS_ON_DISK - getUsedBlocks();
	}
	
	/**
	 * Return the number of used blocks.
	 */
	public int getUsedBlocks() {
		int used = 0;
		Iterator files = getFiles().iterator();
		while (files.hasNext()) {
			RdosFileEntry fileEntry = (RdosFileEntry) files.next();
			if (!fileEntry.isDeleted()) used+= fileEntry.getSizeInBlocks();
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
	 * Since RDOS uses blocks, a null is returned indicating no suggsetions.
	 */
	public int[] getBitmapDimensions() {
		return null;
	}

	/**
	 * Get the length of the bitmap.
	 */
	public int getBitmapLength() {
		return BLOCKS_ON_DISK;
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
		return new String[] { "Block" };
	}
	
	/**
	 * Get Pascal-specific disk information.
	 */
	public List getDiskInformation() {
		List list = super.getDiskInformation();
		list.add(new DiskInformation("Total Blocks", BLOCKS_ON_DISK));
		list.add(new DiskInformation("Free Blocks", getFreeBlocks()));
		list.add(new DiskInformation("Used Blocks", getUsedBlocks()));
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
				list.add(new FileColumnHeader("Type", 1, FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader("Blocks", 3, FileColumnHeader.ALIGN_RIGHT));
				list.add(new FileColumnHeader("Name", 24, FileColumnHeader.ALIGN_LEFT));
				list.add(new FileColumnHeader("Size", 6, FileColumnHeader.ALIGN_RIGHT));
				list.add(new FileColumnHeader("Starting Block", 3, FileColumnHeader.ALIGN_RIGHT));
				break;
			case FILE_DISPLAY_DETAIL:
				list.add(new FileColumnHeader("Type", 1, FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader("Blocks", 3, FileColumnHeader.ALIGN_RIGHT));
				list.add(new FileColumnHeader("Name", 24, FileColumnHeader.ALIGN_LEFT));
				list.add(new FileColumnHeader("Size", 6, FileColumnHeader.ALIGN_RIGHT));
				list.add(new FileColumnHeader("Starting Block", 3, FileColumnHeader.ALIGN_RIGHT));
				list.add(new FileColumnHeader("Address", 5, FileColumnHeader.ALIGN_RIGHT));
				list.add(new FileColumnHeader("Deleted?", 7, FileColumnHeader.ALIGN_CENTER));
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
		if ( !(fileEntry instanceof RdosFileEntry)) {
			throw new IllegalArgumentException("Most have a RDOS file entry!");
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
}
