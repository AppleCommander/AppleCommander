/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2022 by Robert Greene
 * robgreene at users.sourceforge.net
 * Copyright (C) 2004-2022 by John B.  Matthews
 * matthewsj at users.sourceforge.net
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
package com.webcodepro.applecommander.storage.os.pascal;

import com.webcodepro.applecommander.storage.*;
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import com.webcodepro.applecommander.util.AppleUtil;
import com.webcodepro.applecommander.util.TextBundle;

import java.util.*;

/**
 * Manages a disk that is in the Pascal format.
 * <p>
 * Date created: Oct 4, 2002 11:56:50 PM
 * @author Rob Greene
 * @author John B. Matthews [getFiles(), get/putDirectory(), createFile()]
 */
public class PascalFormatDisk extends FormattedDisk {
	private TextBundle textBundle = StorageBundle.getInstance();
	/**
	 * The size of the Pascal file entry.
	 */
	public static final int ENTRY_SIZE = 26;
	
	// filetypes used elsewhere in the code:
	private static final String TEXTFILE = "TEXT"; //$NON-NLS-1$
	private static final String CODEFILE = "CODE"; //$NON-NLS-1$
	private static final String DATAFILE = "DATA"; //$NON-NLS-1$
	
	/**
	 * The known filetypes for a Pascal disk.
	 */
	private static final String[] FILE_TYPES = {
			"xdskfile",
			CODEFILE,
			TEXTFILE,
			"INFO",
			DATAFILE,
			"GRAF",
			"FOTO",
			"securedir"
		};
	private static final Map<String,String> FILE_TYPE_MAP = Map.of(
			// Pascal => Prodos
			"xdskfile", "BAD",
			CODEFILE, "BIN",
			TEXTFILE, "TXT",
			"INFO", "TXT",
			DATAFILE, "BIN",
			"GRAF", "BIN",
			"FOTO", "BIN",
			"securedir", "BIN",

			// Prodos => Pascal
			"BIN", DATAFILE,
			"TXT", TEXTFILE
		);

	/**
	 * Use this inner interface for managing the disk usage data.
	 * This off-loads format-specific implementation to the implementing class.
	 * A BitSet is used to track all blocks, as Pascal disks do not have a
	 * bitmap stored on the disk. This is safe since we know the number of blocks
	 * that exist. (BitSet length is of last set bit - unset bits at the end are
	 * "lost".)
	 */
	private class PascalDiskUsage implements DiskUsage {
		private int location = -1;
		private BitSet bitmap = null;
		public boolean hasNext() {
			return location == -1 || location < getBlocksOnDisk() - 1;
		}
		public void next() {
			if (bitmap == null) {
				bitmap = new BitSet(getBlocksOnDisk());
				// assume all blocks are unused
				for (int block=6; block<getBlocksOnDisk(); block++) {
					bitmap.set(block);
				}
				// process through all files and mark those blocks as used
				for (FileEntry fileEntry : getFiles()) {
				    PascalFileEntry entry = (PascalFileEntry) fileEntry;
					for (int block=entry.getFirstBlock(); block<entry.getLastBlock(); block++) {
						bitmap.clear(block);
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
	 * Constructor for PascalFormatDisk.
	 */
	public PascalFormatDisk(String filename, ImageOrder imageOrder) {
		super(filename, imageOrder);
	}

	/**
	 * Create a PascalFormatDisk.
	 */
	public static PascalFormatDisk[] create(String filename, String volumeName, ImageOrder imageOrder) {
		PascalFormatDisk disk = new PascalFormatDisk(filename, imageOrder);
		disk.format();
		disk.setDiskName(volumeName);
		return new PascalFormatDisk[] { disk };
	}

	/**
	 * Identify the operating system format of this disk.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getFormat()
	 */
	public String getFormat() {
		return textBundle.get("PascalFormatDisk.Pascal"); //$NON-NLS-1$
	}

	/**
	 * Retrieve a list of files.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getFiles()
	 */
	public List<FileEntry> getFiles() {
		List<FileEntry> list = new ArrayList<>();
		byte[] directory = readDirectory();
		// process directory blocks:
		int entrySize = ENTRY_SIZE;
		int count = AppleUtil.getWordValue(directory, 16);
		int offset = entrySize;
		for (int i=0; i<count; i++) {
			byte[] entry = new byte[entrySize];
			System.arraycopy(directory, offset, entry, 0, entry.length);
			list.add(new PascalFileEntry(entry, this));
			offset+= entrySize;
		}
		return list;
	}

 	/**
	 * Retrieve the entire directory.
	 */
	public List<PascalFileEntry> getDirectory() {
		List<PascalFileEntry> list = new ArrayList<>();
		byte[] directory = readDirectory();
		int count = AppleUtil.getWordValue(directory, 16);
		int offset = 0;
		for (int i = 0; i <= count; i++) {
			byte[] entry = new byte[ENTRY_SIZE];
			System.arraycopy(directory, offset, entry, 0, entry.length);
			list.add(new PascalFileEntry(entry, this));
			offset += ENTRY_SIZE;
		}
		return list;
	}

	/**
	 * Write the revised directory.
	 */
	public void putDirectory(List<PascalFileEntry> files) {
		byte[] directory = new byte[2048];
		int offset = 0;
		for (PascalFileEntry fileEntry : files) {
			byte[] entry = fileEntry.toBytes();
			System.arraycopy(entry, 0, directory, offset, entry.length);
			offset += ENTRY_SIZE;
		}
		writeDirectory(directory);
	}

	/**
	 * Create a new FileEntry.
	 */
 	public PascalFileEntry createFile() throws DiskFullException {
		// find index of largest free space
		int count = 0; int index = 0; int max = 0;
		int last = 0; int first = 0; int free = 0;
		List<PascalFileEntry> dir = getDirectory();
		count = dir.size();
		for (int i = 1; i < count; i++) {
			last = ((PascalFileEntry) dir.get(i - 1)).getLastBlock();
			first = ((PascalFileEntry) dir.get(i)).getFirstBlock();
			free = first - last;
			if (free > max) {
				max = free; index = i;
			}
		}
		// check after last entry, too
		last = ((PascalFileEntry) dir.get(count - 1)).getLastBlock();
		first = getBlocksOnDisk();
		free = first - last;
		if (free > max) {
			max = free; index = count;
		}
		if (free > 0 && count < 78) {
			// update file count in the volume entry
			PascalFileEntry volEntry = (PascalFileEntry) dir.get(0);
			volEntry.setFileCount(count);
			dir.set(0, volEntry);
			// add new entry to list
			dir.add(index, new PascalFileEntry(new byte[ENTRY_SIZE], this));
			PascalFileEntry entry = (PascalFileEntry) dir.get(index);
			// fill in plausible values; will rely index, first and last
			first = ((PascalFileEntry) dir.get(index - 1)).getLastBlock();
			entry.setFirstBlock(first);
			entry.setLastBlock(first + max);
			entry.setFiletype("data"); //$NON-NLS-1$
			entry.setFilename("x"); //$NON-NLS-1$
			entry.setBytesUsedInLastBlock(512);
			entry.setModificationDate(new Date());
			entry.setEntryIndex(index);
			dir.set(index, entry);
			// write it back to disk
			putDirectory(dir);
			return entry;
		} else {
			throw new DiskFullException(
					textBundle.get("PascalFormatDisk.DiskFull") //$NON-NLS-1$
					, this.getFilename());
		}
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
	 * Read directory blocks.  These are always in blocks 2 - 5 and
	 * are treated as a 2048 byte array.
	 */
	public byte[] readDirectory() {
		byte[] directory = new byte[4 * BLOCK_SIZE];
		for (int i=0; i<4; i++) {
			System.arraycopy(readBlock(2+i), 0, directory, i*BLOCK_SIZE, BLOCK_SIZE);
		}
		return directory;
	}
	
	/**
	 * Write directory blocks.
	 */
	public void writeDirectory(byte[] directory) {
		if (directory == null || directory.length != 2048) {
			throw new IllegalArgumentException(textBundle.get("PascalFormatDisk.InvalidPascalDirectory")); //$NON-NLS-1$
		}
		for (int i=0; i<4; i++) {
			byte[] block = new byte[BLOCK_SIZE];
			System.arraycopy(directory, i*BLOCK_SIZE, block, 0, BLOCK_SIZE);
			writeBlock(2+i, block);
		}
	}

	/**
	 * Identify if this disk format is capable of having directories.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#canHaveDirectories()
	 */
	public boolean canHaveDirectories() {
		return false;
	}

	/**
	 * Return the amount of free space in bytes.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getFreeSpace()
	 */
	public int getFreeSpace() {
		return getFreeBlocks() * BLOCK_SIZE;
	}
	
	/**
	 * Return the number of free blocks.
	 */
	public int getFreeBlocks() {
		List<FileEntry> files = getFiles();
		int blocksFree = getBlocksOnDisk() - 6;
		if (files != null) {
		    for (FileEntry fileEntry : files) {
		        PascalFileEntry entry = (PascalFileEntry) fileEntry;
				blocksFree-= entry.getBlocksUsed();
			}
		}
		return blocksFree;
	}
	
	/**
	 * Return the volume entry.
	 */
	protected byte[] getVolumeEntry() {
		byte[] block = readBlock(2);
		byte[] entry = new byte[ENTRY_SIZE];
		System.arraycopy(block, 0, entry, 0, entry.length);
		return entry;
	}
	
	/**
	 * Return the number of blocks on disk.
	 */
	public int getBlocksOnDisk() {
		return AppleUtil.getWordValue(getVolumeEntry(), 14);
	}

	/**
	 * Return the number of files on disk.
	 */
	public int getFilesOnDisk() {
		return AppleUtil.getWordValue(getVolumeEntry(), 16);
	}

 	/**
 	 * Return the first block.
 	 */
	public int getFirstBlock() {
		return AppleUtil.getWordValue(getVolumeEntry(), 18);
 	}

	/**
	 * Return the last access date.
	 */
	public Date getLastAccessDate() {
		return AppleUtil.getPascalDate(getVolumeEntry(), 18);
	}

	/**
	 * Return the most recent date setting.  Huh?
	 */
	public Date getMostRecentDateSetting() {
		return AppleUtil.getPascalDate(getVolumeEntry(), 20);
	}

	/**
	 * Return the amount of used space in bytes.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getUsedSpace()
	 */
	public int getUsedSpace() {
		return getUsedBlocks() * BLOCK_SIZE;
	}
	
	/**
	 * Return the number of used blocks.
	 */
	public int getUsedBlocks() {
		List<FileEntry> files = getFiles();
		int blocksUsed = 6;
		if (files != null) {
		    for (FileEntry fileEntry : files) {
		        PascalFileEntry entry = (PascalFileEntry) fileEntry;
				blocksUsed+= entry.getBlocksUsed();
			}
		}
		return blocksUsed;
	}

	/**
	 * Return the name of the disk.  This is stored on block #2
	 * offset +6 (string[7]).
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#getDiskName()
	 */
	public String getDiskName() {
		return AppleUtil.getPascalString(readBlock(2), 6) + ":"; //$NON-NLS-1$
	}
	
	/**
	 * Set the name of the disk.  The Pascal parlance is "volume name"
	 * whereas AppleCommander uses disk name.  Max length is 7.
	 */
	public void setDiskName(String volumeName) {
		byte[] directory = readDirectory();
		AppleUtil.setPascalString(directory, 6, volumeName.toUpperCase(), 7);
		writeDirectory(directory);
	}

	/**
	 * Get suggested dimensions for display of bitmap.  Since Pascal disks are
	 * a block device, no suggestion is given.
	 */
	public int[] getBitmapDimensions() {
		return null;
	}

	/**
	 * Get the length of the bitmap.
	 */
	public int getBitmapLength() {
		return getBlocksOnDisk();
	}

	/**
	 * Get the disk usage iterator.
	 */
	public DiskUsage getDiskUsage() {
		return new PascalDiskUsage();
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
		list.add(new DiskInformation(textBundle.get("TotalBlocks"), getBlocksOnDisk())); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("FreeBlocks"), getFreeBlocks())); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("UsedBlocks"), getUsedBlocks())); //$NON-NLS-1$
		list.add(new DiskInformation(
				textBundle.get("PascalFormatDisk.FilesOnDisk"), getFilesOnDisk())); //$NON-NLS-1$
		list.add(new DiskInformation(
				textBundle.get("PascalFormatDisk.LastAccessDate"), getLastAccessDate())); //$NON-NLS-1$
		list.add(new DiskInformation(
				textBundle.get("PascalFormatDisk.MostRecentDateSetting"), getMostRecentDateSetting())); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("PascalFormatDisk.FirstBlock"), getFirstBlock())); //$NON-NLS-1$
		list.add(new DiskInformation(textBundle.get("PascalFormatDisk.VolumeDate"), getLastAccessDate())); //$NON-NLS-1$
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
				list.add(new FileColumnHeader(textBundle.get("Modified"), 8,
						FileColumnHeader.ALIGN_CENTER, "modified"));
				list.add(new FileColumnHeader(textBundle.get("Blocks"), 3,
						FileColumnHeader.ALIGN_RIGHT, "blocks"));
				list.add(new FileColumnHeader(textBundle.get("Filetype"), 8,
						FileColumnHeader.ALIGN_CENTER, "filetype"));
				list.add(new FileColumnHeader(textBundle.get("Name"), 15,
						FileColumnHeader.ALIGN_LEFT, "name"));
				break;
			case FILE_DISPLAY_DETAIL:
				list.add(new FileColumnHeader(textBundle.get("Modified"), 8,
						FileColumnHeader.ALIGN_CENTER, "modified"));
				list.add(new FileColumnHeader(textBundle.get("Blocks"), 3,
						FileColumnHeader.ALIGN_RIGHT, "blocks"));
				list.add(new FileColumnHeader(
						textBundle.get("PascalFormatDisk.BytesInLastBlock"), 3,
						FileColumnHeader.ALIGN_RIGHT, "bytesInLastBlock"));
				list.add(new FileColumnHeader(textBundle.get("SizeInBytes"), 6,
						FileColumnHeader.ALIGN_RIGHT, "size"));
				list.add(new FileColumnHeader(textBundle.get("Filetype"), 8,
						FileColumnHeader.ALIGN_CENTER, "filetype"));
				list.add(new FileColumnHeader(textBundle.get("Name"), 15,
						FileColumnHeader.ALIGN_LEFT, "name"));
				list.add(new FileColumnHeader(
						textBundle.get("PascalFormatDisk.FirstBlock"), 3, 
						FileColumnHeader.ALIGN_RIGHT, "firstBlock"));
				list.add(new FileColumnHeader(
						textBundle.get("PascalFormatDisk.LastBlock"), 3, 
						FileColumnHeader.ALIGN_RIGHT, "lastBlock"));
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
		return false;
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
	 * Indicates if this disk image can delete a file.
	 */
	public boolean canDeleteFile() {
		return true;
	}

	/**
	 * Get the data associated with the specified FileEntry.
	 */
	public byte[] getFileData(FileEntry fileEntry) {
		if ( !(fileEntry instanceof PascalFileEntry)) {
			throw new IllegalArgumentException(textBundle.get("PascalFormatDisk.IncorrectFileEntryError")); //$NON-NLS-1$
		}
		PascalFileEntry pascalEntry = (PascalFileEntry) fileEntry;
		int firstBlock = pascalEntry.getFirstBlock();
		int lastBlock = pascalEntry.getLastBlock();
		byte[] fileData = new byte[pascalEntry.getSize()];
		int offset = 0;
		for (int block = firstBlock; block < lastBlock; block++) {
			byte[] blockData = readBlock(block);
			if (block == lastBlock-1) {
				System.arraycopy(blockData, 0, fileData, offset, 
						pascalEntry.getBytesUsedInLastBlock());
			} else {
				System.arraycopy(blockData, 0, fileData, offset, blockData.length);
			}
			offset+= blockData.length;
		}
		return fileData;
	}
	
	/**
	 * Format the disk as an Apple Pascal disk.
	 * @see com.webcodepro.applecommander.storage.FormattedDisk#format()
	 */
	public void format() {
		getImageOrder().format();
		writeBootCode();
		// Create volume name
		byte[] directory = readDirectory();
		AppleUtil.setWordValue(directory, 0, 0);	// always 0
		AppleUtil.setWordValue(directory, 2, 6);	// last directory block
		AppleUtil.setWordValue(directory, 4, 0);	// entry type (0=vol header)
			// volume name should have been set in constructor!
		int blocks = getImageOrder().getBlocksOnDevice();
		AppleUtil.setWordValue(directory, 14, blocks);
		AppleUtil.setWordValue(directory, 16, 0);	// no files
		AppleUtil.setWordValue(directory, 18, 0);	// first block
		AppleUtil.setPascalDate(directory, 20, new Date());	// most recent date setting
		writeDirectory(directory);
	}

	/**
	 * Returns the logical disk number.  Returns a 0 to indicate no numbering.
	 */
	public int getLogicalDiskNumber() {
		return 0;
	}

	/**
	 * Returns a valid filename for the given filename.
	 * <p/> 
     * Summary taken from the filename description in "Apple Pascal:
     * Operating System Reference Manual."
     * <p/>
     * A legal diskette filename can consist of up to 15 characters.  
     * Lower- case letters typed into a filename are translated to upper-case, 
     * and spaces and non-printing characters are removed from the filename.
     * All characters are legal in filenames. However, from the keyboard you should 
     * not type filenames that include the following characters:
     * dollar sign ($), left square bracket ([), equals sign (=) question mark, (?), 
     * RETURN, and the CTRL characters C, F, M, S, U, and @.
     * <p/>
     * WARNING: The Filer will not be able to access filenames containing the 
     * characters dollar sign ($), equals sign (=), question mark (?),
     * or comma (,)
	 */
	public String getSuggestedFilename(String filename) {
        String name = filename.toUpperCase()
                              .replaceAll("[ \t\r\n]", "")
                              .replaceAll("[^A-Z0-9.]", ".");
        return name.substring(0, Math.min(name.length(),15));
	}

	/**
	 * Returns a valid filetype for the given filename.  The most simple
	 * format will just assume a filetype of binary.  This method is
	 * available for the interface to make an intelligent first guess
	 * as to the filetype.
	 */
	public String getSuggestedFiletype(String filename) {
		String filetype = DATAFILE;
		int pos = filename.lastIndexOf("."); //$NON-NLS-1$
		if (pos > 0) {
			String what = filename.substring(pos+1);
			if ("txt".equalsIgnoreCase(what)) { //$NON-NLS-1$
				filetype = TEXTFILE;
			} else if ("pas".equalsIgnoreCase(what)) { //$NON-NLS-1$
				filetype = CODEFILE;
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
	 * No Pascal filetypes require or support an address.
	 */
	public boolean needsAddress(String filetype) {
		return false;
	}

	/**
	 * Indicates if this FormattedDisk supports a disk map.
	 */	
	public boolean supportsDiskMap() {
		return true;
	}

	/**
	 * Change to a different ImageOrder.  Remains in Pascal format but
	 * the underlying order can change.
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
		// TODO implement setFileData
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
        return DiskGeometry.BLOCK;
    }

	/**
	 * Provides conversation from a given ProDOS file type since as it is common across
	 * many archiving tools.
	 */
	@Override
	public String fromProdosFiletype(String prodosFiletype) {
		return FILE_TYPE_MAP.getOrDefault(prodosFiletype, DATAFILE);
	}
	/**
	 * Provides conversation to a given ProDOS file type since as it is common across
	 * many archiving tools.
	 */
	@Override
	public String toProdosFiletype(String nativeFiletype) {
		return FILE_TYPE_MAP.getOrDefault(nativeFiletype, "BIN");
	}
}
