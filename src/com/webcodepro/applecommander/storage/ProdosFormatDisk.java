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
import java.util.Date;
import java.util.List;

/**
 * Manages a disk that is in the ProDOS format.
 * <p>
 * Date created: Oct 3, 2002 11:45:25 PM
 * @author: Rob Greene
 */
public class ProdosFormatDisk extends FormattedDisk {
	/**
	 * Hold on to the volume directory header.
	 */
	private ProdosVolumeDirectoryHeader volumeHeader;

	/**
	 * Use this inner interface for managing the disk usage data.
	 * This offloads format-specific implementation to the implementing class.
	 */
	private class ProdosDiskUsage implements DiskUsage {
		private int location = -1;
		private transient byte[] data = null;
		public boolean hasNext() {
			return location == -1 || location < volumeHeader.getTotalBlocks() - 1;
		}
		public void next() {
			location++;
		}
		/**
		 * Get the free setting for the bitmap at the current location.
		 */
		public boolean isFree() {
			if (location == -1) {
				throw new IllegalArgumentException("Invalid dimension for isFree! Did you call next first?");
			}
			if (data == null) {
				data = readVolumeBitMap();
			}
			return isBlockFree(data, location);
		}
		public boolean isUsed() {
			return !isFree();
		}
	}

	/**
	 * Constructor for ProdosFormatDisk.
	 * @param filename
	 * @param diskImage
	 */
	public ProdosFormatDisk(String filename, byte[] diskImage) {
		super(filename, diskImage);
		volumeHeader = new ProdosVolumeDirectoryHeader(this);
	}

	/**
	 * Create a ProdosFormatDisk.
	 */
	public static ProdosFormatDisk[] create(String filename, String diskName, int imageSize) {
		ProdosFormatDisk disk = new ProdosFormatDisk(filename, new byte[imageSize]);
		disk.setDiskName(diskName);
		disk.format();
		return new ProdosFormatDisk[] { disk };
	}

	/**
	 * Identify the operating system format of this disk.
	 * @see com.webcodepro.applecommander.storage.Disk#getFormat()
	 */
	public String getFormat() {
		return "ProDOS";
	}

	/**
	 * Retrieve a list of files.
	 * @see com.webcodepro.applecommander.storage.Disk#getFiles()
	 */
	public List getFiles() {
		return getFiles(2);
	}

	/**
	 * Build a list of files, starting in the given block number.
	 * This works for the master as well as the subdirectories.
	 */		
	protected List getFiles(int blockNumber) {
		List files = new ArrayList();
		while (blockNumber != 0) {
			byte[] block = readBlock(blockNumber);
			int offset = 4;
			while (offset+ProdosCommonEntry.ENTRY_LENGTH < BLOCK_SIZE) {
				ProdosCommonEntry tester = 
					new ProdosCommonEntry(this, blockNumber, offset);
				if (tester.isVolumeHeader() || tester.isSubdirectoryHeader()) {
					// ignore it, we've already got it
				} else {
					ProdosFileEntry fileEntry = 
						new ProdosFileEntry(this, blockNumber, offset);
					files.add(fileEntry);
					if (fileEntry.isDirectory()) {
						int keyPointer = fileEntry.getKeyPointer();
						fileEntry.setSubdirectoryHeader(
							new ProdosSubdirectoryHeader(this, keyPointer));
						fileEntry.setFiles(getFiles(keyPointer));
					}
				}
				offset+= ProdosCommonEntry.ENTRY_LENGTH;
			}
			blockNumber = AppleUtil.getWordValue(block, 2);
		}
		return files;
	}
	
	/**
	 * Return the amount of free space in bytes.
	 * @see com.webcodepro.applecommander.storage.Disk#getFreeSpace()
	 */
	public int getFreeSpace() {
		return getFreeBlocks() * BLOCK_SIZE;
	}
	
	/**
	 * Return the number of free blocks on the disk.
	 */
	public int getFreeBlocks() {
		int freeBlocks = 0;
		int blocksToProcess = (volumeHeader.getTotalBlocks() + 4095) / 4096;
		int blockNumber = volumeHeader.getBitMapPointer();
		for (int ix=0; ix<blocksToProcess; ix++) {
			byte[] block = readBlock(blockNumber+ix);
			for (int byt=0; byt<block.length; byt++) {
				freeBlocks+= AppleUtil.getBitCount(block[byt]);
			}
		}
		return freeBlocks;
	}

	/**
	 * Return the amount of used space in bytes.
	 * @see com.webcodepro.applecommander.storage.Disk#getUsedSpace()
	 */
	public int getUsedSpace() {
		return getUsedBlocks() * BLOCK_SIZE;
	}
	
	/**
	 * Return the number of used blocks on the disk.
	 */
	public int getUsedBlocks() {
		return volumeHeader.getTotalBlocks() - getFreeBlocks();
	}

	/**
	 * Identify if this disk format is capable of having directories.
	 * @see com.webcodepro.applecommander.storage.Disk#hasDirectories()
	 */
	public boolean canHaveDirectories() {
		return true;
	}
	
	/**
	 * Return the name of the disk.
	 * @see com.webcodepro.applecommander.storage.Disk#getDiskName()
	 */
	public String getDiskName() {
		return "/" + volumeHeader.getVolumeName() + "/";
	}
	
	/**
	 * Set the name of the disk (volume name).
	 */
	public void setDiskName(String volumeName) {
		volumeHeader.setVolumeName(volumeName);
	}

	/**
	 * Get suggested dimensions for display of bitmap. There is no suggestion
	 * for a ProDOS volume - it is just a series of blocks.
	 */
	public int[] getBitmapDimensions() {
		return null;
	}

	/**
	 * Get the length of the bitmap.
	 */
	public int getBitmapLength() {
		return volumeHeader.getTotalBlocks();
	}
	
	/**
	 * Get the disk usage iterator.
	 */
	public DiskUsage getDiskUsage() {
		return new ProdosDiskUsage();
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
		list.add(new DiskInformation("Total Blocks", volumeHeader.getTotalBlocks()));
		list.add(new DiskInformation("Free Blocks", getFreeBlocks()));
		list.add(new DiskInformation("Used Blocks", getUsedBlocks()));
		list.add(new DiskInformation("Volume Access", 
			(volumeHeader.canDestroy() ? "Destroy " : "") +
			(volumeHeader.canRead() ? "Read " : "") +
			(volumeHeader.canRename() ? "Rename " : "") +
			(volumeHeader.canWrite() ? "Write" : "")));
		list.add(new DiskInformation("Block Number of Bitmap", volumeHeader.getBitMapPointer()));
		list.add(new DiskInformation("Creation Date", volumeHeader.getCreationDate()));
		list.add(new DiskInformation("File Entries Per Block", volumeHeader.getEntriesPerBlock()));
		list.add(new DiskInformation("File Entry Length (bytes)", volumeHeader.getEntryLength()));
		list.add(new DiskInformation("Active Files in Root Directory", volumeHeader.getFileCount()));
		list.add(new DiskInformation("Minimum ProDOS Version Required", 
			volumeHeader.getMinimumProdosVersion()));
		list.add(new DiskInformation("Volume Created By ProDOS Version", volumeHeader.getProdosVersion()));
		list.add(new DiskInformation("Volume Name", volumeHeader.getVolumeName()));
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
				list.add(new FileColumnHeader("Name", 15, FileColumnHeader.ALIGN_LEFT));
				list.add(new FileColumnHeader("Filetype", 8, FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader("Blocks", 3, FileColumnHeader.ALIGN_RIGHT));
				list.add(new FileColumnHeader("Modified", 10, FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader("Created", 10, FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader("Length", 10, FileColumnHeader.ALIGN_RIGHT));
				list.add(new FileColumnHeader("Aux. Type", 8, FileColumnHeader.ALIGN_LEFT));
				break;
			case FILE_DISPLAY_DETAIL:
				list.add(new FileColumnHeader(" ", 1, FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader("Name", 15, FileColumnHeader.ALIGN_LEFT));
				list.add(new FileColumnHeader("Deleted?", 7, FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader("Permissions", 8, FileColumnHeader.ALIGN_LEFT));
				list.add(new FileColumnHeader("Filetype", 8, FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader("Directory?", 9, FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader("Blocks", 3, FileColumnHeader.ALIGN_RIGHT));
				list.add(new FileColumnHeader("Modified", 10, FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader("Created", 10, FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader("Length", 10, FileColumnHeader.ALIGN_RIGHT));
				list.add(new FileColumnHeader("Aux. Type", 8, FileColumnHeader.ALIGN_LEFT));
				list.add(new FileColumnHeader("Dir. Header", 5, FileColumnHeader.ALIGN_RIGHT));
				list.add(new FileColumnHeader("Key Block", 5, FileColumnHeader.ALIGN_RIGHT));
				list.add(new FileColumnHeader("Key Type", 8, FileColumnHeader.ALIGN_LEFT));
				list.add(new FileColumnHeader("Changed", 5, FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader("Min. ProDOS Ver.", 2, FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader("ProDOS Ver.", 2, FileColumnHeader.ALIGN_CENTER));
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
	 * Note that this could return a 16MB file!  Sparse files are not treated specially.
	 */
	public byte[] getFileData(FileEntry fileEntry) {
		if ( !(fileEntry instanceof ProdosFileEntry)) {
			throw new IllegalArgumentException("Most have a ProDOS file entry!");
		}
		ProdosFileEntry prodosEntry = (ProdosFileEntry) fileEntry;
		byte[] fileData = new byte[prodosEntry.getEofPosition()];
		int indexBlocks = 0;
		if (prodosEntry.isSeedlingFile()) {
			byte[] blockData = readBlock(prodosEntry.getKeyPointer());
			System.arraycopy(blockData, 0, fileData, 0, prodosEntry.getEofPosition());
		} else if (prodosEntry.isSaplingFile()) {
			byte[] indexBlock = readBlock(prodosEntry.getKeyPointer());
			getIndexBlockData(fileData, indexBlock, 0);
		} else if (prodosEntry.isTreeFile()) {
			byte[] masterIndexBlock = readBlock(prodosEntry.getKeyPointer());
			int offset = 0;
			for (int i=0; i<0x100; i++) {
				int blockNumber = AppleUtil.getWordValue(masterIndexBlock[i], masterIndexBlock[i+0x100]);
				byte[] indexBlock = readBlock(blockNumber);
				offset+= getIndexBlockData(fileData, indexBlock, offset);
			}
		} else {
			throw new IllegalArgumentException("Unknown ProDOS storage type!");
		}
		return fileData;
	}

	/**
	 * Read file data from the given index block.
	 * Note that block number 0 is an unused block.
	 * @see #getFileData()
	 */
	protected int getIndexBlockData(byte[] fileData, byte[] indexBlock, int offset) {
		for (int i=0; i<0x100; i++) {
			int blockNumber = AppleUtil.getWordValue(indexBlock[i], indexBlock[i+0x100]);
			byte[] blockData = readBlock(blockNumber);
			if (offset + blockData.length > fileData.length) { // end of file
				int bytesToCopy = fileData.length - offset;
				if (blockNumber != 0) System.arraycopy(blockData, 0, fileData, offset, bytesToCopy);
				offset+= bytesToCopy;
			} else {
				if (blockNumber != 0) System.arraycopy(blockData, 0, fileData, offset, blockData.length);
				offset+= blockData.length;
			}
		}
		return offset;
	}
	
	/**
	 * Read the Volume Bit Map.
	 */
	public byte[] readVolumeBitMap() {
		int volumeBitmapBlock = volumeHeader.getBitMapPointer();
		int volumeBitmapBlocks = volumeHeader.getTotalBlocks();
		int blocksToRead = (volumeBitmapBlocks / 4096) + 1;
		// Read in the entire volume bitmap:
		byte[] data = new byte[blocksToRead * BLOCK_SIZE];
		for (int i=0; i<blocksToRead; i++) {
			System.arraycopy(readBlock(volumeBitmapBlock+i), 0, data, i*BLOCK_SIZE, BLOCK_SIZE);
		}
		return data;
	}
	
	/**
	 * Write the Volume Bit Map.
	 */
	public void writeVolumeBitMap(byte[] data) {
		int volumeBitmapBlock = volumeHeader.getBitMapPointer();
		int volumeBitmapBlocks = volumeHeader.getTotalBlocks();
		int blocksToWrite = (volumeBitmapBlocks / 4096) + 1;
		if (data.length != blocksToWrite * BLOCK_SIZE) {
			throw new IllegalArgumentException(
				"The ProDOS Volume Bit Map is not the correct size.");
		}
		byte[] dataBlock = new byte[BLOCK_SIZE];
		for (int i=0; i<blocksToWrite; i++) {
			System.arraycopy(data, i*BLOCK_SIZE, dataBlock, 0, BLOCK_SIZE);
			writeBlock(volumeBitmapBlock+i, dataBlock);
		}
	}
	
	/**
	 * Determine if the specified block is free.
	 */
	public boolean isBlockFree(byte[] data, int blockNumber) {
		// Locate appropriate bit and check it:
		int byt = blockNumber / 8;
		int bit = 7 - (blockNumber % 8);
		boolean free = AppleUtil.isBitSet(data[byt], bit);
		return free;
	}

	/**
	 * Set if the specified block is free.
	 */
	public void setBlockFree(byte[] data, int blockNumber) {
		// Locate appropriate bit and check it:
		int byt = blockNumber / 8;
		int bit = 7 - (blockNumber % 8);
		data[byt] = AppleUtil.setBit(data[byt], bit);
	}
	
	/**
	 * Determine if the specified block is used.
	 */
	public boolean isBlockUsed(byte[] data, int blockNumber) {
		return !isBlockFree(data, blockNumber);
	}

	/**
	 * Set if the specified block is free.
	 */
	public void setBlockUsed(byte[] data, int blockNumber) {
		// Locate appropriate bit and check it:
		int byt = blockNumber / 8;
		int bit = 7 - (blockNumber % 8);
		data[byt] = AppleUtil.clearBit(data[byt], bit);
	}
	
	/**
	 * Format the ProDOS volume.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#format()
	 */
	public void format() {
		writeBootCode();
		String volumeName = volumeHeader.getVolumeName();
		int totalBlocks = getDiskImage().length / BLOCK_SIZE;
		int usedBlocks = (totalBlocks / 4096) + 7;
		// setup volume directory
		byte[] data = new byte[BLOCK_SIZE];
		for (int block=2; block<6; block++) {
			int nextBlock = (block < 5) ? block+1 : 0;
			int prevBlock = (block > 2) ? block-1 : 0;
			AppleUtil.setWordValue(data, 0, prevBlock);
			AppleUtil.setWordValue(data, 2, nextBlock);
			writeBlock(block, data);
		}
		// setup volume header information (each set will also save data)
		volumeHeader.setVolumeHeader();
		volumeHeader.setVolumeName(volumeName);
		volumeHeader.setCreationDate(new Date());
		volumeHeader.setProdosVersion(0);
		volumeHeader.setMinimumProdosVersion(0);
		volumeHeader.setChanged(true);
		volumeHeader.setDestroy(true);
		volumeHeader.setRead(true);
		volumeHeader.setRename(true);
		volumeHeader.setWrite(true);
		volumeHeader.setEntryLength();
		volumeHeader.setEntriesPerBlock();
		volumeHeader.setFileCount(0);
		volumeHeader.setBitMapPointer(6);
		volumeHeader.setTotalBlocks(totalBlocks);
		// setup bitmap usage
		byte[] bitmap = readVolumeBitMap();
		for (int block=0; block<totalBlocks; block++) {
			if (block < usedBlocks) {
				setBlockUsed(bitmap, block);
			} else {
				setBlockFree(bitmap, block);
			}
		}
		writeVolumeBitMap(bitmap);
	}

	/**
	 * Returns the logical disk number.  Returns a 0 to indicate no numbering.
	 */
	public int getLogicalDiskNumber() {
		return 0;
	}
}
