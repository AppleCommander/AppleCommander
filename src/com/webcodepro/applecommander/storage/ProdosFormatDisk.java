/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-3 by Robert Greene
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

import com.webcodepro.applecommander.util.AppleUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Manages a disk that is in the ProDOS format.
 * <p>
 * Date created: Oct 3, 2002 11:45:25 PM
 * @author: Rob Greene
 */
public class ProdosFormatDisk extends FormattedDisk {
	/**
	 * The location of the "next block" pointer in a directory entry.
	 * This is a 2-byte word (lo/hi) format.  $0000 is end of directory.
	 */
	private static final int NEXT_BLOCK_POINTER = 2;
	/**
	 * The location of the "previous block" pointer in a directory entry.
	 * This is a 2-byte word (lo/hi) format.  $0000 is start of directory.
	 */
	private static final int PREV_BLOCK_POINTER = 0;
	/**
	 * The Volume Directory block number.
	 */
	private static final int VOLUME_DIRECTORY_BLOCK = 2;
	/**
	 * A complete list of all known ProDOS filetypes.  Note that this
	 * list really cannot be complete, as there are multiple mappings per
	 * identifier in some cases - differentiated by AUXTYPE.  This is
	 * loaded via initialize when the first instance of ProdosFormatDisk
	 * is created.
	 */
	private static ProdosFileType[] fileTypes;
	/**
	 * This array of strings conains all filetypes.  This is lazy 
	 * initialized by getFiletypes.
	 */
	private static String[] filetypeStrings;
	/**
	 * Hold on to the volume directory header.
	 */
	private ProdosVolumeDirectoryHeader volumeHeader;

	/**
	 * This class holds filetype mappings.
	 */
	private class ProdosFileType {
		private byte type;
		private String string;
		private boolean addressRequired;
		private boolean canCompile;
		public ProdosFileType(byte type, String string, boolean addressRequired, boolean canCompile) {
			this.type = type;
			this.string = string;
			this.addressRequired = addressRequired;
			this.canCompile = canCompile;
		}
		public byte getType() {
			return type;
		}
		public String getString() {
			return string;
		}
		public boolean needsAddress() {
			return addressRequired;
		}
		public boolean canCompile() {
			return canCompile;
		}
	}

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
		initialize();
	}
	
	/**
	 * Initialize all file types.
	 */
	protected void initialize() {
		if (fileTypes != null) return;
		
		fileTypes = new ProdosFileType[256];
		InputStream inputStream = 
			getClass().getResourceAsStream("ProdosFileTypes.properties");
		Properties properties = new Properties();
		try {
			properties.load(inputStream);
			for (int i=0; i<256; i++) {
				String byt = AppleUtil.getFormattedByte(i).toLowerCase();
				String string = (String) properties.get("filetype." + byt);
				if (string == null || string.length() == 0) {
					string = "$" + byt.toUpperCase();
				}
				boolean addressRequired = Boolean.valueOf((String) properties.get(
					"filetype." + byt + ".address")).booleanValue();
				boolean canCompile = Boolean.valueOf((String) properties.get(
					"filetype." + byt + ".compile")).booleanValue();
				fileTypes[i] = new ProdosFileType((byte)i, string, addressRequired, canCompile);
			}
		} catch (IOException ignored) {
		}
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
	 * Create a FileEntry in the Volume Directory.
	 */
	public FileEntry createFile() throws DiskFullException {
		return createFile(volumeHeader);
	}
	
	/**
	 * Create a FileEntry in the given directory.
	 */
	public FileEntry createFile(ProdosCommonDirectoryHeader directory) 
		throws DiskFullException {
			
		int blockNumber = directory.getFileEntryBlock();
		int headerBlock = blockNumber;
		while (blockNumber != 0) {
			byte[] block = readBlock(blockNumber);
			int offset = 4;
			while (offset+ProdosCommonEntry.ENTRY_LENGTH < BLOCK_SIZE) {
				int value = AppleUtil.getUnsignedByte(block[offset]);
				if ((value & 0xf0) == 0) {
					ProdosFileEntry fileEntry = 
						new ProdosFileEntry(this, blockNumber, offset);
					fileEntry.setKeyPointer(0); //may have been recycled
					fileEntry.setCreationDate(new Date());
					fileEntry.setProdosVersion(0);
					fileEntry.setMinimumProdosVersion(0);
					fileEntry.setCanDestroy(true);
					fileEntry.setCanRead(true);
					fileEntry.setCanRename(true);
					fileEntry.setCanWrite(true);
					fileEntry.setSeedlingFile();
					fileEntry.setHeaderPointer(headerBlock);
					fileEntry.setFilename("BLANK");
					directory.incrementFileCount();
					return fileEntry;
				}
				offset+= ProdosCommonEntry.ENTRY_LENGTH;
			}
			int nextBlockNumber = AppleUtil.getWordValue(block, NEXT_BLOCK_POINTER);
			if (nextBlockNumber == 0 && directory instanceof ProdosSubdirectoryHeader) {
				byte[] volumeBitmap = readVolumeBitMap();
				nextBlockNumber = findFreeBlock(volumeBitmap);
				setBlockUsed(volumeBitmap, nextBlockNumber);
				writeVolumeBitMap(volumeBitmap);
				byte[] oldBlock = readBlock(blockNumber);
				AppleUtil.setWordValue(oldBlock, NEXT_BLOCK_POINTER, nextBlockNumber);
				writeBlock(blockNumber, oldBlock);
				byte[] nextBlock = new byte[BLOCK_SIZE];
				AppleUtil.setWordValue(nextBlock, PREV_BLOCK_POINTER, blockNumber);
				writeBlock(nextBlockNumber, nextBlock);
				ProdosSubdirectoryHeader header = (ProdosSubdirectoryHeader) directory;
				int blockCount = header.getProdosDirectoryEntry().getBlocksUsed();
				blockCount++;
				header.getProdosDirectoryEntry().setBlocksUsed(blockCount);
				header.getProdosDirectoryEntry().setEofPosition(blockCount * BLOCK_SIZE);
			}
			blockNumber = nextBlockNumber;
		}
		throw new DiskFullException("Unable to allocate more space for another file!");
	}

	/**
	 * Retrieve a list of files.
	 * @see com.webcodepro.applecommander.storage.Disk#getFiles()
	 */
	public List getFiles() {
		return getFiles(VOLUME_DIRECTORY_BLOCK);
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
				} else if (!tester.isEmpty()) {
					ProdosFileEntry fileEntry = 
						new ProdosFileEntry(this, blockNumber, offset);
					if (fileEntry.isDirectory()) {
						int keyPointer = fileEntry.getKeyPointer();
						ProdosDirectoryEntry directoryEntry =
							new ProdosDirectoryEntry(this, blockNumber, offset,
								new ProdosSubdirectoryHeader(this, keyPointer));
						files.add(directoryEntry);
					} else {
						files.add(fileEntry);
					}
				}
				offset+= ProdosCommonEntry.ENTRY_LENGTH;
			}
			blockNumber = AppleUtil.getWordValue(block, NEXT_BLOCK_POINTER);
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
	 * Identify if this disk format is capable of having directories.
	 * @see com.webcodepro.applecommander.storage.Disk#canHaveDirectories()
	 */
	public boolean canHaveDirectories() {
		return true;
	}

	/**
	 * Indicates if this disk image can write data to a file.
	 */
	public boolean canWriteFileData() {
		return true;
	}
	
	/**
	 * Indicates if this disk image can delete a file.
	 */
	public boolean canDeleteFile() {
		return true;
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
				if (blockNumber > 0) {
					// FIXME - this may break sparse files!
					byte[] indexBlock = readBlock(blockNumber);
					offset= getIndexBlockData(fileData, indexBlock, offset);
				}
			}
		} else {
			throw new IllegalArgumentException("Unknown ProDOS storage type!");
		}
		return fileData;
	}

	/**
	 * Free blocks used by a DosFileEntry.
	 */
	protected void freeBlocks(ProdosFileEntry prodosFileEntry) {
		byte[] bitmap = readVolumeBitMap();
		int block = prodosFileEntry.getKeyPointer();
		if (block == 0) return;	// new entry
		setBlockFree(bitmap,block);
		if (prodosFileEntry.isSaplingFile()) {
			freeBlocksInIndex(bitmap,block);
		} else if (prodosFileEntry.isTreeFile()) {
			byte[] masterIndexBlock = readBlock(block);
			for (int i=0; i<0x100; i++) {
				int indexBlockNumber = AppleUtil.getWordValue(
					masterIndexBlock[i], masterIndexBlock[i+0x100]);
				if (indexBlockNumber > 0) freeBlocksInIndex(bitmap,indexBlockNumber);
			}
		}
		writeVolumeBitMap(bitmap);
	}
	
	/**
	 * Free the given index block and the data blocks it points to.
	 */
	private void freeBlocksInIndex(byte[] bitmap, int indexBlockNumber) {
		setBlockFree(bitmap, indexBlockNumber);
		byte[] indexBlock = readBlock(indexBlockNumber);
		for (int i=0; i<0x100; i++) {
			int blockNumber = AppleUtil.getWordValue(indexBlock[i], indexBlock[i+0x100]);
			if (blockNumber > 0) setBlockFree(bitmap, blockNumber);
		}
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
				break;
			} else {
				if (blockNumber != 0) System.arraycopy(blockData, 0, fileData, offset, blockData.length);
				offset+= blockData.length;
			}
		}
		return offset;
	}

	/**
	 * Set the data associated with the specified ProdosFileEntry into sectors
	 * on the disk.
	 */
	protected void setFileData(ProdosFileEntry fileEntry, byte[] fileData) 
		throws DiskFullException {
			
		// compute free space and see if the data will fit!
		int numberOfDataBlocks = (fileData.length + BLOCK_SIZE - 1) / BLOCK_SIZE;
		int numberOfBlocks = numberOfDataBlocks;
		if (numberOfBlocks > 1) {
			numberOfBlocks+= ((numberOfDataBlocks-1) / 256) + 1;	// that's 128K
			if (numberOfDataBlocks > 256) {
				numberOfBlocks++;
			}
		}
		if (numberOfBlocks > getFreeBlocks() + fileEntry.getBlocksUsed()) {
			throw new DiskFullException("This file requires " + numberOfBlocks
				+ " blocks but there are only " + getFreeBlocks() + " blocks"
				+ " available on the disk.");
		}
		// free "old" data and just rewrite stuff...
		freeBlocks(fileEntry);
		byte[] bitmap = readVolumeBitMap();
		int blockNumber = fileEntry.getKeyPointer();
		if (blockNumber == 0) {
			blockNumber = findFreeBlock(bitmap);
		}
		int indexBlockNumber = 0;
		byte[] indexBlockData = null;
		int masterIndexBlockNumber = 0;
		byte[] masterIndexBlockData = new byte[BLOCK_SIZE];
		int offset = 0;
		int blockCount = 0;
		while (offset < fileData.length) {
			if (blockCount > 0) blockNumber = findFreeBlock(bitmap);
			setBlockUsed(bitmap, blockNumber);
			blockCount++;
			byte[] blockData = new byte[BLOCK_SIZE];
			int length = Math.min(BLOCK_SIZE, fileData.length - offset);
			System.arraycopy(fileData,offset,blockData,0,length);
			writeBlock(blockNumber, blockData);
			if (numberOfDataBlocks > 1) {
				// growing to a tree file
				if (offset > 0 && (offset / BLOCK_SIZE) % 256 == 0) {
					if (masterIndexBlockNumber == 0) {
						masterIndexBlockNumber = findFreeBlock(bitmap);
						setBlockUsed(bitmap, masterIndexBlockNumber);
						blockCount++;
					}
					writeBlock(indexBlockNumber, indexBlockData);
					indexBlockData = null;
					indexBlockNumber = 0;
				}
				// new index block
				if (indexBlockData == null) {	// sapling files
					indexBlockNumber = findFreeBlock(bitmap);
					indexBlockData = new byte[BLOCK_SIZE];
					setBlockUsed(bitmap, indexBlockNumber);
					blockCount++;
					// This is only used for Tree files (but we always record it):
					int position = (offset / (BLOCK_SIZE * 256));
					byte low = (byte)(indexBlockNumber % 256);
					byte high = (byte)(indexBlockNumber / 256);
					masterIndexBlockData[position] = low;
					masterIndexBlockData[position + 0x100] = high;
				}
				// record last block position in index block
				int position = (offset / BLOCK_SIZE) % 256;
				byte low = (byte)(blockNumber % 256);
				byte high = (byte)(blockNumber / 256);
				indexBlockData[position] = low;
				indexBlockData[position + 0x100] = high;
			}
			offset+= BLOCK_SIZE;
		}
		if (numberOfDataBlocks == 1) {
			fileEntry.setKeyPointer(blockNumber);
			fileEntry.setSeedlingFile();
		} else if (numberOfDataBlocks <= 256) {
			writeBlock(indexBlockNumber, indexBlockData);
			fileEntry.setKeyPointer(indexBlockNumber);
			fileEntry.setSaplingFile();
		} else {
			writeBlock(indexBlockNumber, indexBlockData);
			writeBlock(masterIndexBlockNumber, masterIndexBlockData);
			fileEntry.setKeyPointer(masterIndexBlockNumber);
			fileEntry.setTreeFile();
		}
		fileEntry.setBlocksUsed(blockCount);
		fileEntry.setEofPosition(fileData.length);
		fileEntry.setLastModificationDate(new Date());
		writeVolumeBitMap(bitmap);
	}
	
	/**
	 * Locate a free block in the Volume Bitmap.
	 */
	protected int findFreeBlock(byte[] volumeBitmap) throws DiskFullException {
		int block = 1;
		int blocksOnDisk = getBitmapLength();
		while (block < blocksOnDisk) {
			if (isBlockFree(volumeBitmap,block)) {
				if ((block+1) * BLOCK_SIZE < getDiskImage().length) {
					return block;
				}
				throw new ProdosDiskSizeDoesNotMatchException(
					"The ProDOS physical disk size does not match the formatted size.");
			}
			block++;
		}
		throw new DiskFullException(
			"Unable to locate a free block in the Volume Bitmap!");
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
		volumeHeader.setHasChanged(true);
		volumeHeader.setCanDestroy(true);
		volumeHeader.setCanRead(true);
		volumeHeader.setCanRename(true);
		volumeHeader.setCanWrite(true);
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

	/**
	 * Returns a valid filename for the given filename.  ProDOS filenames
	 * have a maximum length of 15 characters, must start with a character
	 * and may contain characters (A-Z), digits (0-9), or the period (.).
	 */
	public String getSuggestedFilename(String filename) {
		StringBuffer newName = new StringBuffer();
		if (!Character.isLetter(filename.charAt(0))) {
			newName.append('A');
		}
		int i=0;
		while (newName.length() < 15 && i<filename.length()) {
			char ch = filename.charAt(i);
			if (Character.isLetterOrDigit(ch) || ch == '.') {
				newName.append(ch);
			}
			i++;
		}
		return newName.toString().toUpperCase().trim();
	}

	/**
	 * Returns a valid filetype for the given filename.  The most simple
	 * format will just assume a filetype of binary.  This method is
	 * available for the interface to make an intelligent first guess
	 * as to the filetype.
	 */
	public String getSuggestedFiletype(String filename) {
		String filetype = "BIN";
		int pos = filename.lastIndexOf(".");
		if (pos > 0) {
			String what = filename.substring(pos+1);
			ProdosFileType type = findFileType(what);
			if (type != null) {
				filetype = type.getString();
			}
		}
		return filetype;
	}

	/**
	 * Return the filetype of this file.  This will be three characters,
	 * according to ProDOS - a "$xx" if unknown.
	 */
	public String getFiletype(int filetype) {
		ProdosFileType prodostype = fileTypes[filetype];
		return prodostype.getString();
	}

	/**
	 * Get the numerical filetype.
	 */
	public byte getFiletype(String filetype) {
		ProdosFileType type = findFileType(filetype);
		if (type != null) {
			return type.getType();
		}
		return 0x00;
	}
	
	/**
	 * Locate the associated ProdosFileType.
	 */
	public ProdosFileType findFileType(String filetype) {
		for (int i=0; i<fileTypes.length; i++) {
			if (filetype.equalsIgnoreCase(fileTypes[i].getString())) {
				return fileTypes[i];
			}
		}
		return null;
	}

	/**
	 * Returns a list of possible file types.  Since the filetype is
	 * specific to each operating system, a simple String is used.
	 */
	public String[] getFiletypes() {
		if (filetypeStrings == null) {
			filetypeStrings = new String[fileTypes.length];
			for (int i = 0; i < fileTypes.length; i++) {
				filetypeStrings[i] = fileTypes[i].getString();
			}
		}
		return filetypeStrings;
	}

	/**
	 * Indicates if this filetype requires an address component.
	 */
	public boolean needsAddress(String filetype) {
		ProdosFileType fileType = findFileType(filetype);
		if (fileType != null) {
			return fileType.needsAddress();
		}
		return false;
	}

	/**
	 * Indicates if this filetype can be compiled.
	 */
	public boolean canCompile(String filetype) {
		ProdosFileType fileType = findFileType(filetype);
		if (fileType != null) {
			return fileType.canCompile();
		}
		return false;
	}
}
