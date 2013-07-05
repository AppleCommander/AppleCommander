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
package com.webcodepro.applecommander.storage.os.prodos;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.StorageBundle;
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import com.webcodepro.applecommander.util.AppleUtil;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Manages a disk that is in the ProDOS format.
 * <p>
 * Date created: Oct 3, 2002 11:45:25 PM
 * @author Rob Greene
 */
public class ProdosFormatDisk extends FormattedDisk {
	private TextBundle textBundle = StorageBundle.getInstance();
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
			return location == -1 || location < getVolumeHeader().getTotalBlocks() - 1;
		}
		public void next() {
			location++;
		}
		/**
		 * Get the free setting for the bitmap at the current location.
		 */
		public boolean isFree() {
			if (location == -1) {
				throw new IllegalArgumentException(StorageBundle.getInstance()
						.get("ProdosFormatDisk.InvalidDimensionError")); //$NON-NLS-1$
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
	 */
	public ProdosFormatDisk(String filename, ImageOrder imageOrder) {
		super(filename, imageOrder);
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
			getClass().getResourceAsStream("ProdosFileTypes.properties"); //$NON-NLS-1$
		Properties properties = new Properties();
		try {
			properties.load(inputStream);
			for (int i=0; i<256; i++) {
				String byt = AppleUtil.getFormattedByte(i).toLowerCase();
				String string = (String) properties.get("filetype." + byt); //$NON-NLS-1$
				if (string == null || string.length() == 0) {
					string = "$" + byt.toUpperCase(); //$NON-NLS-1$
				}
				boolean addressRequired = Boolean.valueOf((String) properties.get(
					"filetype." + byt + ".address")).booleanValue(); //$NON-NLS-1$ //$NON-NLS-2$
				boolean canCompile = Boolean.valueOf((String) properties.get(
					"filetype." + byt + ".compile")).booleanValue(); //$NON-NLS-1$ //$NON-NLS-2$
				fileTypes[i] = new ProdosFileType((byte)i, string, addressRequired, canCompile);
			}
		} catch (IOException ignored) {
			// Ignored
		}
	}

	/**
	 * Create a ProdosFormatDisk.
	 */
	public static ProdosFormatDisk[] create(String filename, String diskName, ImageOrder imageOrder) {
		ProdosFormatDisk disk = new ProdosFormatDisk(filename, imageOrder);
		disk.format();
		disk.setDiskName(diskName);
		return new ProdosFormatDisk[] { disk };
	}

	/**
	 * Identify the operating system format of this disk.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getFormat()
	 */
	public String getFormat() {
		return textBundle.get("Prodos"); //$NON-NLS-1$
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
					fileEntry.setFilename(textBundle.get("ProdosFormatDisk.Blank")); //$NON-NLS-1$
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
		throw new DiskFullException(textBundle.get("ProdosFormatDisk.UnableToAllocateSpaceError")); //$NON-NLS-1$
	}

	/**
	 * Retrieve a list of files.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getFiles()
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
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getFreeSpace()
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
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getUsedSpace()
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
		return true;
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
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getDiskName()
	 */
	public String getDiskName() {
		return "/" + volumeHeader.getVolumeName() + "/"; //$NON-NLS-1$ //$NON-NLS-2$
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
		return new String[] { textBundle.get("Block") }; //$NON-NLS-1$
	}
	
	/**
	 * Get Pascal-specific disk information.
	 */
	public List getDiskInformation() {
		List list = super.getDiskInformation();
		list.add(new DiskInformation(textBundle.get("TotalBlocks"),  //$NON-NLS-1$
				volumeHeader.getTotalBlocks()));
		list.add(new DiskInformation(textBundle.get("FreeBlocks"),  //$NON-NLS-1$
				getFreeBlocks()));
		list.add(new DiskInformation(textBundle.get("UsedBlocks"),  //$NON-NLS-1$
				getUsedBlocks()));
		list.add(new DiskInformation(textBundle.get("ProdosFormatDisk.VolumeAccess"),  //$NON-NLS-1$
			(volumeHeader.canDestroy() ? textBundle.get("Destroy") : "") +  //$NON-NLS-1$//$NON-NLS-2$
			(volumeHeader.canRead() ? textBundle.get("Read") : "") +  //$NON-NLS-1$//$NON-NLS-2$
			(volumeHeader.canRename() ? textBundle.get("Rename") : "") +  //$NON-NLS-1$//$NON-NLS-2$
			(volumeHeader.canWrite() ? textBundle.get("Write") : "")));  //$NON-NLS-1$//$NON-NLS-2$
		list.add(new DiskInformation(textBundle.get("ProdosFormatDisk.BitmapBlockNumber"), volumeHeader.getBitMapPointer())); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("ProdosFormatDisk.CreationDate"), volumeHeader.getCreationDate())); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("ProdosFormatDisk.FileEntriesPerBlock"), volumeHeader.getEntriesPerBlock())); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("ProdosFormatDisk.FileEntryLength"), volumeHeader.getEntryLength())); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("ProdosFormatDisk.ActiveFilesInRootDirectory"), volumeHeader.getFileCount())); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("ProdosFormatDisk.MinimumVersionProdos"),  //$NON-NLS-1$
			volumeHeader.getMinimumProdosVersion()));
		list.add(new DiskInformation(textBundle.get("ProdosFormatDisk.CreationVersionProdos"), volumeHeader.getProdosVersion())); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("ProdosFormatDisk.VolumeName"), volumeHeader.getVolumeName())); //$NON-NLS-1$
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
				list.add(new FileColumnHeader(" ", 1, FileColumnHeader.ALIGN_CENTER)); //$NON-NLS-1$
				list.add(new FileColumnHeader(textBundle.get("Name"), 15,  //$NON-NLS-1$
						FileColumnHeader.ALIGN_LEFT));
				list.add(new FileColumnHeader(textBundle.get("Filetype"), 8, //$NON-NLS-1$ 
						FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader(textBundle.get("Blocks"), 3,  //$NON-NLS-1$
						FileColumnHeader.ALIGN_RIGHT));
				list.add(new FileColumnHeader(textBundle.get("Modified"), 10, //$NON-NLS-1$
						FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader(
						textBundle.get("ProdosFormatDisk.Created"), 10, //$NON-NLS-1$
						FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader(
						textBundle.get("ProdosFormatDisk.Length"), 10, //$NON-NLS-1$
						FileColumnHeader.ALIGN_RIGHT));
				list.add(new FileColumnHeader(
						textBundle.get("ProdosFormatDisk.AuxType"), 8, //$NON-NLS-1$
						FileColumnHeader.ALIGN_LEFT));
				break;
			case FILE_DISPLAY_DETAIL:
				list.add(new FileColumnHeader(" ", 1, FileColumnHeader.ALIGN_CENTER)); //$NON-NLS-1$
				list.add(new FileColumnHeader(textBundle.get("Name"), 15,  //$NON-NLS-1$
						FileColumnHeader.ALIGN_LEFT));
				list.add(new FileColumnHeader(textBundle.get("DeletedQ"), 7,  //$NON-NLS-1$
						FileColumnHeader.ALIGN_CENTER)); //$NON-NLS-1$
				list.add(new FileColumnHeader(textBundle.get("ProdosFormatDisk.Permissions"), 8, FileColumnHeader.ALIGN_LEFT)); //$NON-NLS-1$
				list.add(new FileColumnHeader(textBundle.get("Filetype"), 8, FileColumnHeader.ALIGN_CENTER)); //$NON-NLS-1$
				list.add(new FileColumnHeader(textBundle.get("ProdosFormatDisk.DirectoryQ"), 9, FileColumnHeader.ALIGN_CENTER)); //$NON-NLS-1$
				list.add(new FileColumnHeader(textBundle.get("Blocks"), 3,  //$NON-NLS-1$
						FileColumnHeader.ALIGN_RIGHT));
				list.add(new FileColumnHeader(textBundle.get("Modified"), 10, FileColumnHeader.ALIGN_CENTER)); //$NON-NLS-1$
				list.add(new FileColumnHeader(textBundle.get("ProdosFormatDisk.Created"), 10, FileColumnHeader.ALIGN_CENTER)); //$NON-NLS-1$
				list.add(new FileColumnHeader(textBundle.get("ProdosFormatDisk.Length"), 10, FileColumnHeader.ALIGN_RIGHT)); //$NON-NLS-1$
				list.add(new FileColumnHeader(textBundle.get("ProdosFormatDisk.AuxType"), 8, FileColumnHeader.ALIGN_LEFT)); //$NON-NLS-1$
				list.add(new FileColumnHeader(textBundle.get("ProdosFormatDisk.DirectoryHeader"), 5, FileColumnHeader.ALIGN_RIGHT)); //$NON-NLS-1$
				list.add(new FileColumnHeader(textBundle.get("ProdosFormatDisk.KeyBlock"), 5, FileColumnHeader.ALIGN_RIGHT)); //$NON-NLS-1$
				list.add(new FileColumnHeader(textBundle.get("ProdosFormatDisk.KeyType"), 8, FileColumnHeader.ALIGN_LEFT)); //$NON-NLS-1$
				list.add(new FileColumnHeader(textBundle.get("ProdosFormatDisk.Changed"), 5, FileColumnHeader.ALIGN_CENTER)); //$NON-NLS-1$
				list.add(new FileColumnHeader(textBundle.get("ProdosFormatDisk.MinimumProdosVersion"), 2, FileColumnHeader.ALIGN_CENTER)); //$NON-NLS-1$
				list.add(new FileColumnHeader(textBundle.get("ProdosFormatDisk.ProdosVersion"), 2, FileColumnHeader.ALIGN_CENTER)); //$NON-NLS-1$
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
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#canHaveDirectories()
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
			throw new IllegalArgumentException(textBundle.get("ProdosFormatDisk.MustHaveEntry")); //$NON-NLS-1$
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
			throw new IllegalArgumentException(textBundle.get("ProdosFormatDisk.UnknownStorageType")); //$NON-NLS-1$
		}
		return fileData;
	}

	/**
	 * Free blocks used by a ProdosFileEntry.
	 */
	protected void freeBlocks(ProdosFileEntry prodosFileEntry) {
		byte[] bitmap = readVolumeBitMap();
		int block = prodosFileEntry.getKeyPointer();
		if (block == 0) return;	// new entry
		if (prodosFileEntry.isGEOSFile()) {
			// A GEOS file allocates another block, pointed to by the aux bytes.
			setBlockFree(bitmap,prodosFileEntry.getAuxiliaryType());
		}
		setBlockFree(bitmap,block);
		if (prodosFileEntry.isSaplingFile()) {
			freeBlocksInIndex(bitmap,block,false);
		} else if (prodosFileEntry.isTreeFile()) {
			byte[] masterIndexBlock = readBlock(block);
			for (int i=0; i<0x100; i++) {
				if (!prodosFileEntry.isGEOSFile() ||
				(prodosFileEntry.isGEOSFile() && (i < 0xfe)))
				{
					// As long as we're not deleting a GEOS file, delete all index entries.
					// GEOS uses records 0xfe and 0xff for space calculations, not pointers.
					int indexBlockNumber = AppleUtil.getWordValue(
							masterIndexBlock[i], masterIndexBlock[i+0x100]);
					if (indexBlockNumber > 0) freeBlocksInIndex(bitmap,indexBlockNumber,prodosFileEntry.isGEOSFile());
				}
			}
		}
		writeVolumeBitMap(bitmap);
	}

	/**
	 * Free the given index block and the data blocks it points to.
	 */
	private void freeBlocksInIndex(byte[] bitmap, int indexBlockNumber, boolean isGEOS) {
		setBlockFree(bitmap, indexBlockNumber);
		byte[] indexBlock = readBlock(indexBlockNumber);
		for (int i=0; i<0x100; i++) {
			if (!isGEOS ||
					(isGEOS && (i < 0xfe))) {
				// As long as we're not deleting a GEOS file, delete all entries.
				// GEOS uses records 0xfe and 0xff for space calculations, not pointers.
				int blockNumber = AppleUtil.getWordValue(indexBlock[i], indexBlock[i+0x100]);
				if (blockNumber > 0) setBlockFree(bitmap, blockNumber);
			}
		}
	}

	/**
	 * Read file data from the given index block.
	 * Note that block number 0 is an unused block.
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
			}
			if (blockNumber != 0) System.arraycopy(blockData, 0, fileData, offset, blockData.length);
			offset+= blockData.length;
		}
		return offset;
	}

	/**
	 * Set the data associated with the specified ProdosFileEntry into sectors
	 * on the disk.  Automatically grows the filesystem structures from seedling
	 * to sapling to tree. 
	 */
	protected void setFileData(ProdosFileEntry fileEntry, byte[] fileData) 
		throws DiskFullException {

		if (fileEntry.isGEOSFile()) {
			// If this is a GEOS file, things are a bit different.
			setGEOSFileData(fileEntry, fileData);
		} else {
			// compute free space and see if the data will fit!
			int numberOfDataBlocks = (fileData.length + BLOCK_SIZE - 1) / BLOCK_SIZE;
			if (fileData.length == 0) numberOfDataBlocks = 1;
			int numberOfBlocks = numberOfDataBlocks;
			if (numberOfBlocks > 1) {
				numberOfBlocks+= ((numberOfDataBlocks-1) / 256) + 1;	// that's 128K
				if (numberOfDataBlocks > 256) {
					numberOfBlocks++;
				}
			}
			if (numberOfBlocks > getFreeBlocks() + fileEntry.getBlocksUsed()) {
				throw new DiskFullException(textBundle.
						format("ProdosFormatDisk.NotEnoughSpaceOnDiskError", //$NON-NLS-1$
								numberOfBlocks, getFreeBlocks()));
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
			// Need to let a file length go through once
			while ((offset < fileData.length) || ((fileData.length == 0) && (offset == 0))){
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
	}
	
	/**
	 * Set the data associated with the specified ProdosFileEntry into sectors
	 * on the disk.  Automatically grows the filesystem structures from seedling
	 * to sapling to tree. 
	 */
	// TODO: the writing of a single fork can be factored out... it is very common to routines nearby.
	protected void setFileData(ProdosFileEntry fileEntry, byte[] dataFork, byte[] resourceFork) 
		throws DiskFullException {

		// compute free space and see if the data will fit!
		int numberOfDataBlocks = (dataFork.length + BLOCK_SIZE - 1) / BLOCK_SIZE +
			(resourceFork.length + BLOCK_SIZE - 1) / BLOCK_SIZE;
		int numberOfBlocks = numberOfDataBlocks;
		if (numberOfBlocks > 1) {
			numberOfBlocks+= ((numberOfDataBlocks-1) / 256) + 1;	// that's 128K
			if (numberOfDataBlocks > 256) {
				numberOfBlocks++;
			}
		}
		if (numberOfBlocks > getFreeBlocks() + fileEntry.getBlocksUsed()) {
			throw new DiskFullException(textBundle.
					format("ProdosFormatDisk.NotEnoughSpaceOnDiskError", //$NON-NLS-1$
							numberOfBlocks, getFreeBlocks()));
		}
		// free "old" data and just rewrite stuff...
		freeBlocks(fileEntry);
		byte[] bitmap = readVolumeBitMap();
		int blockNumber = fileEntry.getKeyPointer();
		if (blockNumber == 0) {
			blockNumber = findFreeBlock(bitmap);
		}
		int blockCount = 0;
		int extendedKeyBlockNumber = findFreeBlock(bitmap);
		setBlockUsed(bitmap, extendedKeyBlockNumber);
		byte[] extendedKeyBlockData = new byte[BLOCK_SIZE];
		int indexBlockNumber = 0;
		byte[] indexBlockData = null;
		int masterIndexBlockNumber = 0;
		byte[] masterIndexBlockData = new byte[BLOCK_SIZE];

		int offset = 0;
		numberOfDataBlocks = (dataFork.length + BLOCK_SIZE - 1) / BLOCK_SIZE ;
		while (offset < dataFork.length) {
			blockNumber = findFreeBlock(bitmap);
			setBlockUsed(bitmap, blockNumber);
			blockCount++;
			byte[] blockData = new byte[BLOCK_SIZE];
			int length = Math.min(BLOCK_SIZE, dataFork.length - offset);
			System.arraycopy(dataFork,offset,blockData,0,length);
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
		AppleUtil.setWordValue(extendedKeyBlockData,0x03,numberOfDataBlocks); // Set the number of blocks used
		AppleUtil.set3ByteValue(extendedKeyBlockData,0x05,	dataFork.length); // Set the number of bytes used
		if (numberOfDataBlocks == 1) {
			extendedKeyBlockData[0] = 1; // Set the seedling ProDOS storage type
			AppleUtil.setWordValue(extendedKeyBlockData,1,blockNumber); // Set the master block number
		} else if (numberOfDataBlocks <= 256) {
			writeBlock(indexBlockNumber, indexBlockData);
			AppleUtil.setWordValue(extendedKeyBlockData,1,indexBlockNumber); // Set the master block number
			extendedKeyBlockData[0] = 2; // Set the sapling ProDOS storage type
		} else {
			writeBlock(indexBlockNumber, indexBlockData);
			writeBlock(masterIndexBlockNumber, masterIndexBlockData);
			AppleUtil.setWordValue(extendedKeyBlockData,1,masterIndexBlockNumber); // Set the master block number
			extendedKeyBlockData[0] = 3; // Set the tree ProDOS storage type
		}
		blockCount++; // To account for extendedKeyBlock 

		offset = 0;
		indexBlockNumber = 0;
		indexBlockData = null;
		masterIndexBlockNumber = 0;
		numberOfDataBlocks = (resourceFork.length + BLOCK_SIZE - 1) / BLOCK_SIZE;
		while (offset < resourceFork.length) {
			if (blockCount > 0) blockNumber = findFreeBlock(bitmap);
			setBlockUsed(bitmap, blockNumber);
			blockCount++;
			byte[] blockData = new byte[BLOCK_SIZE];
			int length = Math.min(BLOCK_SIZE, resourceFork.length - offset);
			System.arraycopy(resourceFork,offset,blockData,0,length);
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
		AppleUtil.setWordValue(extendedKeyBlockData,0x103,numberOfDataBlocks); // Set the number of blocks used
		AppleUtil.set3ByteValue(extendedKeyBlockData,0x105,	resourceFork.length); // Set the number of bytes used
		if (numberOfDataBlocks == 1) {
			extendedKeyBlockData[0x100] = 1; // Set the seedling ProDOS storage type
			AppleUtil.setWordValue(extendedKeyBlockData,0x101,blockNumber); // Set the master block number
		} else if (numberOfDataBlocks <= 256) {
			writeBlock(indexBlockNumber, indexBlockData);
			AppleUtil.setWordValue(extendedKeyBlockData,0x101,indexBlockNumber); // Set the master block number
			extendedKeyBlockData[0x100] = 2; // Set the sapling ProDOS storage type
		} else {
			writeBlock(indexBlockNumber, indexBlockData);
			writeBlock(masterIndexBlockNumber, masterIndexBlockData);
			AppleUtil.setWordValue(extendedKeyBlockData,0x101,masterIndexBlockNumber); // Set the master block number
			extendedKeyBlockData[0x100] = 3; // Set the tree ProDOS storage type
		}
		writeBlock(extendedKeyBlockNumber, extendedKeyBlockData);

		fileEntry.setKeyPointer(extendedKeyBlockNumber);
		fileEntry.setBlocksUsed(blockCount);
		fileEntry.setEofPosition(dataFork.length+resourceFork.length);
		fileEntry.setLastModificationDate(new Date());
		writeVolumeBitMap(bitmap);
	}
	
	/**
	 * Set the data associated with the specified ProdosFileEntry into sectors
	 * on the disk.  Take GEOS file structures into account.
	 */
	protected void setGEOSFileData(ProdosFileEntry fileEntry, byte[] fileData) 
		throws DiskFullException {

		// compute free space and see if the data will fit!
		int numberOfDataBlocks = (fileData.length - 1) / BLOCK_SIZE;
		int numberOfBlocks = numberOfDataBlocks;
		numberOfBlocks+= ((numberOfDataBlocks-1) / 254) + 1;	// GEOS uses the last two blocks for eof calculations
		if (numberOfDataBlocks > 254) {
			numberOfBlocks++;
		}
		if (numberOfBlocks > getFreeBlocks() + fileEntry.getBlocksUsed()) {
			throw new DiskFullException(textBundle.
					format("ProdosFormatDisk.NotEnoughSpaceOnDiskError", //$NON-NLS-1$
					numberOfBlocks, getFreeBlocks()));
		}
		// free "old" data and just rewrite stuff...
		freeBlocks(fileEntry);
		byte[] bitmap = readVolumeBitMap();

		// Place the first BLOCK_SIZE bytes of data in a block pointed to by the aux address.
		int headerBlockNumber = findFreeBlock(bitmap);
		byte[] headerData = new byte[BLOCK_SIZE];
		setBlockUsed(bitmap, headerBlockNumber);
		System.arraycopy(fileData,0,headerData,0,BLOCK_SIZE);
		writeBlock(headerBlockNumber, headerData);
		fileEntry.setAddress(headerBlockNumber);

		if (AppleUtil.getUnsignedByte(fileData[0x180]) >> 4 == 2) {
			setGEOSSaplingData(bitmap, fileEntry, fileData);
		} else {
			setGEOSTreeData(bitmap, fileEntry, fileData);
		}
		fileEntry.setGEOSMeta(headerData);
	}

	/**
	 * Set the GEOS "sapling" file data.
	 */
	protected void setGEOSSaplingData(byte[] bitmap, ProdosFileEntry fileEntry, byte[] fileData)
		throws DiskFullException {

		int indexBlockNumber = findFreeBlock(bitmap);
		setBlockUsed(bitmap, indexBlockNumber);
		byte[] indexBlockData = new byte[BLOCK_SIZE];
		int offset = BLOCK_SIZE;
		int blockNumber = 0;
		int blockCount = 1; // The header block counts for one
		while (offset < fileData.length) {
			blockNumber = findFreeBlock(bitmap);
			setBlockUsed(bitmap, blockNumber);
			blockCount++;
			byte[] blockData = new byte[BLOCK_SIZE];
			int length = Math.min(BLOCK_SIZE, fileData.length - offset);
			System.arraycopy(fileData,offset,blockData,0,length);
			writeBlock(blockNumber, blockData);
			// record last block position in index block
			int position = ((offset - BLOCK_SIZE) / BLOCK_SIZE) % 256;
			byte low = (byte)(blockNumber % 256);
			byte high = (byte)(blockNumber / 256);
			indexBlockData[position] = low;
			indexBlockData[position + 0x100] = high;
			offset+= BLOCK_SIZE;
		}
		indexBlockData[255] = (byte)((fileData.length - BLOCK_SIZE) % 256); // Lo file eof
		indexBlockData[511] = (byte)((fileData.length - BLOCK_SIZE) / 256); // Med file eof
		writeBlock(indexBlockNumber, indexBlockData);
		fileEntry.setKeyPointer(indexBlockNumber);
		fileEntry.setSaplingFile();
		fileEntry.setBlocksUsed(blockCount);
		fileEntry.setEofPosition(fileData.length - BLOCK_SIZE);
		fileEntry.setLastModificationDate(new Date());
		writeVolumeBitMap(bitmap);
	}

	/**
	 * Set the GEOS "tree" file data.
	 */
	protected void setGEOSTreeData(byte[] bitmap, ProdosFileEntry fileEntry, byte[] fileData)
		throws DiskFullException {

		int masterIndexBlockNumber = findFreeBlock(bitmap);
		setBlockUsed(bitmap, masterIndexBlockNumber);
		byte[] masterIndexBlockData = new byte[BLOCK_SIZE];
		int offset = BLOCK_SIZE;
		int blockCount = 2; // Start by counting the header block and master index
		int eofCount = 0;
		for (int masterIterator = 0; masterIterator < 254; masterIterator++) {
			if ((fileData[0x100+masterIterator] != 0xff) && (offset < fileData.length)){
				byte[] lengthData = new byte[BLOCK_SIZE];
				System.arraycopy(fileData,offset,lengthData,0,BLOCK_SIZE);
				offset += BLOCK_SIZE;
				int recordLength = AppleUtil.getUnsignedByte(lengthData[0xff]) 
				+ AppleUtil.getUnsignedByte(lengthData[0x1ff])*256;
				int indexBlockNumber = findFreeBlock(bitmap);
				setBlockUsed(bitmap, indexBlockNumber);
				blockCount +=1;
				byte[] indexBlockData = new byte[BLOCK_SIZE];
				int blockNumber = 0;
				int startingPoint = offset;
				while (offset < startingPoint + recordLength) {
					blockNumber = findFreeBlock(bitmap);
					setBlockUsed(bitmap, blockNumber);
					blockCount +=1;
					byte[] blockData = new byte[BLOCK_SIZE];
					int length = Math.min(BLOCK_SIZE, recordLength - offset + startingPoint);
					System.arraycopy(fileData,offset,blockData,0,length);
					writeBlock(blockNumber, blockData);
					eofCount += length;
					// record last block position in index block
					int position = ((offset-startingPoint) / BLOCK_SIZE) % 256;
					byte low = (byte)(blockNumber % 256);
					byte high = (byte)(blockNumber / 256);
					indexBlockData[position] = low;
					indexBlockData[position + 0x100] = high;
					offset+= BLOCK_SIZE;
				}
				indexBlockData[0xff] =   (byte) (recordLength & 0x0000ff);
				indexBlockData[0x1ff] = (byte)((recordLength & 0x00ff00) >> 8);
				indexBlockData[0x1fe] = (byte)((recordLength & 0xff0000) >> 16);
				writeBlock(indexBlockNumber, indexBlockData);
				byte low = (byte)(indexBlockNumber % 256);
				byte high = (byte)(indexBlockNumber / 256);
				masterIndexBlockData[masterIterator] = low;
				masterIndexBlockData[masterIterator + 0x100] = high;

			} else if (fileData[0x100+masterIterator] == 0xff) {
				masterIndexBlockData[masterIterator] = (byte)0xff;
				masterIndexBlockData[masterIterator+0x100] = (byte)0xff;
			}
		}
		masterIndexBlockData[0x0ff] = (byte) (eofCount & 0x0000ff);
		masterIndexBlockData[0x1ff] = (byte)((eofCount & 0x00ff00) >> 8);
		masterIndexBlockData[0x1fe] = (byte)((eofCount & 0xff0000) >> 16);
		writeBlock(masterIndexBlockNumber, masterIndexBlockData);
		fileEntry.setKeyPointer(masterIndexBlockNumber);
		fileEntry.setTreeFile();
		fileEntry.setBlocksUsed(blockCount);
		fileEntry.setEofPosition(eofCount);
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
				if ((block+1) * BLOCK_SIZE < getPhysicalSize()) {
					return block;
				}
				throw new ProdosDiskSizeDoesNotMatchException(
					textBundle.get("ProdosFormatDisk.ProdosDiskSizeDoesNotMatchError")); //$NON-NLS-1$
			}
			block++;
		}
		throw new DiskFullException(
			textBundle.get("ProdosFormatDisk.NoFreeBlockAvailableError")); //$NON-NLS-1$
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
				textBundle.get("ProdosFormatDisk.UnexpectedVolumeBitMapSizeError")); //$NON-NLS-1$
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
		getImageOrder().format();
		writeBootCode();
		String volumeName = volumeHeader.getVolumeName();
		int totalBlocks = getPhysicalSize() / BLOCK_SIZE;
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
		String filetype = "BIN"; //$NON-NLS-1$
		int pos = filename.lastIndexOf("."); //$NON-NLS-1$
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

	/**
	 * Indicates if this FormattedDisk supports a disk map.
	 */	
	public boolean supportsDiskMap() {
		return true;
	}

	/**
	 * Change to a different ImageOrder.  Remains in ProDOS format but
	 * the underlying order can chage.
	 * @see ImageOrder
	 */
	public void changeImageOrder(ImageOrder imageOrder) {
		AppleUtil.changeImageOrderByBlock(getImageOrder(), imageOrder);
		setImageOrder(imageOrder);
	}

	/**
	 * Writes the raw bytes into the file.  This bypasses any special formatting
	 * of the data (such as prepending the data with a length and/or an address).
	 * Typically, the FileEntry.setFileData method should be used. 
	 */
	public void setFileData(FileEntry fileEntry, byte[] fileData) throws DiskFullException {
		setFileData((ProdosFileEntry)fileEntry, fileData);
	}

	protected ProdosVolumeDirectoryHeader getVolumeHeader() {
		return volumeHeader;
	}

	/**
	 * Create a new DirectoryEntry.
	 * @see com.webcodepro.applecommander.storage.DirectoryEntry#createDirectory()
	 */
	public DirectoryEntry createDirectory(String name) throws DiskFullException {
		return createDirectory(getVolumeHeader(), name);
	}

	/**
	 * Create a new DirectoryEntry.
	 * @see com.webcodepro.applecommander.storage.DirectoryEntry#createDirectory()
	 */
	public DirectoryEntry createDirectory(ProdosCommonDirectoryHeader directory, String name) throws DiskFullException {
		int blockNumber = directory.getFileEntryBlock();
		while (blockNumber != 0) {
			byte[] block = readBlock(blockNumber);
			int entryNum = 0;
			int offset = 4;
			while (offset+ProdosCommonEntry.ENTRY_LENGTH < BLOCK_SIZE) {
				int value = AppleUtil.getUnsignedByte(block[offset]);
				if ((value & 0xf0) == 0) {
					// First, create a new block to contain our subdirectory
					byte[] volumeBitmap = readVolumeBitMap();
					int newDirBlockNumber = findFreeBlock(volumeBitmap);
					setBlockUsed(volumeBitmap, newDirBlockNumber);
					// Clean out the block - it may have been recycled, and control structures need to be gone
					byte[] cleanBlock = new byte[512];
					for (int i = 0;i<512;i++)
						cleanBlock[i] = 0;
					writeBlock(newDirBlockNumber, cleanBlock);
					writeVolumeBitMap(volumeBitmap);
					ProdosSubdirectoryHeader newHeader = new ProdosSubdirectoryHeader(this, newDirBlockNumber);
					ProdosFileEntry subdirEntry = (ProdosFileEntry)createFile(newHeader);
					subdirEntry.setFilename(name);
					newHeader.setHousekeeping();
					newHeader.setCreationDate(new Date());
					newHeader.setParentPointer(blockNumber);
					newHeader.setParentEntry(entryNum);
					newHeader.setParentEntryLength(ProdosCommonEntry.ENTRY_LENGTH);
					// Now, add an entry for this subdirectory 
					ProdosDirectoryEntry fileEntry = 
						new ProdosDirectoryEntry(this, blockNumber, offset, newHeader);
					fileEntry.setBlocksUsed(1); // Mark ourselves as the one block in use in this new subdirectory
					fileEntry.setEofPosition(BLOCK_SIZE);
					fileEntry.setKeyPointer(newDirBlockNumber);
					fileEntry.setCreationDate(new Date());
					fileEntry.setLastModificationDate(new Date());
					fileEntry.setProdosVersion(0);
					fileEntry.setMinimumProdosVersion(0);
					fileEntry.setCanDestroy(true);
					fileEntry.setCanRead(true);
					fileEntry.setCanRename(true);
					fileEntry.setCanWrite(true);
					fileEntry.setSubdirectory();
					fileEntry.setHeaderPointer(blockNumber);
					fileEntry.setFilename(name);
					fileEntry.setFiletype(0x0f); // Filetype = subdirectory
					directory.incrementFileCount();
					return fileEntry;
				}
				offset+= ProdosCommonEntry.ENTRY_LENGTH;
				entryNum++;
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
		throw new DiskFullException(textBundle.get("ProdosFormatDisk.UnableToAllocateSpaceError")); //$NON-NLS-1$
	}
}
