package com.webcodepro.applecommander.storage.os.cpm;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.StorageBundle;
import com.webcodepro.applecommander.storage.filters.BinaryFileFilter;
import com.webcodepro.applecommander.storage.filters.TextFileFilter;
import com.webcodepro.applecommander.util.AppleUtil;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Support the CP/M file entry.  Note that this may actually contain references
 * to multiple file entries via the extent counter.
 * <p>
 * @author Rob Greene
 */
public class CpmFileEntry implements FileEntry {
	private TextBundle textBundle = StorageBundle.getInstance();
	/**
	 * The standard CP/M file entry length.
	 */
	public static final int ENTRY_LENGTH = 0x20;
	/**
	 * The maximum number of extents per file entry record.
	 */
	public static final int MAX_EXTENTS_PER_ENTRY = 0x80;
	/**
	 * The number of bytes used if all records in an extent are filled.
	 * (MAX_EXTENTS_PER_ENTRY * CPM_SECTOR_SIZE)
	 */
	public static final int ALL_RECORDS_FILLED_SIZE = 16384;
	/**
	 * The user number (UU) field is to distinguish multiple files with the
	 * same filename.  This appears to be primarily with deleted files?
	 */
	public static final int USER_NUMBER_OFFSET = 0;
	/**
	 * Offset to beginning of the filename.
	 */
	public static final int FILENAME_OFFSET = 1;
	/**
	 * Filename length (excluding extension).
	 */
	public static final int FILENAME_LENGTH = 8;
	/**
	 * Offset to beginning of the filetype.
	 */
	public static final int FILETYPE_OFFSET = 9;
	/**
	 * Filetype length.
	 */
	public static final int FILETYPE_LENGTH = 3;
	/**
	 * Offset to the filetype "T1" entry.
	 * Indicates read-only.
	 */
	public static final int FILETYPE_T1_OFFSET = FILETYPE_OFFSET;
	/**
	 * Offset to the filetype "T2" entry.
	 * Indicates system or hidden file.
	 */
	public static final int FILETYPE_T2_OFFSET = FILETYPE_OFFSET+1;
	/**
	 * Offset to the filetype "T3" entry.
	 * Backup bit (CP/M 3.1 and later).
	 */
	public static final int FILETYPE_T3_OFFSET = FILETYPE_OFFSET+2;
	/**
	 * Offset to the extent counter (EX) field.
	 */
	public static final int EXTENT_COUNTER_OFFSET = 0xc;
	/**
	 * Offset to the record count (RC) field.
	 */
	public static final int RECORD_COUNT_OFFSET = 0xf;
	/**
	 * Beginning of block allocations.
	 */
	public static final int ALLOCATION_OFFSET = 0x10;
	/**
	 * A short collection of known text-type files.
	 */
	public static final String[] TEXT_FILETYPES = {
		"TXT", "ASM", "MAC", "DOC", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		"PRN", "PAS", "ME",  "INC", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		"HLP" //$NON-NLS-1$
	};
	/**
	 * Reference to the disk this FileEntry is attached to.
	 */
	private CpmFormatDisk disk;
	/**
	 * The offset(s) into the block that the FileEntry is at.
	 */
	private List offsets = new ArrayList();
	
	/**
	 * Construct a CP/M file entry.
	 */
	public CpmFileEntry(CpmFormatDisk disk, int offset) {
		this.disk = disk;
		addOffset(offset);
	}
	
	/**
	 * Add another directory offset to this file entry.
	 */
	public void addOffset(int offset) {
		offsets.add(new Integer(offset));
	}

	/**
	 * Read the fileEntry bytes from the disk image.
	 */
	protected byte[] readFileEntry(int number) {
		byte[] data = new byte[2 * CpmFormatDisk.CPM_BLOCKSIZE];
		System.arraycopy(disk.readCpmBlock(0), 0, data, 
			0, CpmFormatDisk.CPM_BLOCKSIZE);
		System.arraycopy(disk.readCpmBlock(1), 0, data, 
			CpmFormatDisk.CPM_BLOCKSIZE, CpmFormatDisk.CPM_BLOCKSIZE);
		byte[] entry = new byte[ENTRY_LENGTH];
		int offset = ((Integer)offsets.get(number)).intValue();
		System.arraycopy(data, offset, entry, 0, ENTRY_LENGTH);
		return entry;
	}
	
	/**
	 * Write the fileEntry bytes back to the disk image.
	 */
	protected void writeFileEntry(int number, byte[] data) {
		byte[] block = new byte[CpmFormatDisk.CPM_BLOCKSIZE];
		System.arraycopy(data, 0, block, 
			0, CpmFormatDisk.CPM_BLOCKSIZE);
		disk.writeCpmBlock(0, block);
		System.arraycopy(data, 0, block, 
			CpmFormatDisk.CPM_BLOCKSIZE, CpmFormatDisk.CPM_BLOCKSIZE);
		disk.writeCpmBlock(1, block);
	}

	/**
	 * Answer with the name of the file.
	 * @see com.webcodepro.applecommander.storage.FileEntry#getFilename()
	 */
	public String getFilename() {
		return AppleUtil.getString(readFileEntry(0), 
			FILENAME_OFFSET, FILENAME_LENGTH).trim();
	}

	/**
	 * Set the filename.  Note that this assumes the file extension
	 * is completely separate and does not validate characters that
	 * are being set!
	 * @see com.webcodepro.applecommander.storage.FileEntry#setFilename(java.lang.String)
	 */
	public void setFilename(String filename) {
		for (int i=0; i<offsets.size(); i++) {
			byte[] data = readFileEntry(i);
			AppleUtil.setString(data, FILENAME_OFFSET, filename, 
				FILENAME_LENGTH, false);
			writeFileEntry(i, data);
		}
	}

	/**
	 * Answer with the filetype.
	 * @see com.webcodepro.applecommander.storage.FileEntry#getFiletype()
	 */
	public String getFiletype() {
		return AppleUtil.getString(readFileEntry(0), 
			FILETYPE_OFFSET, FILETYPE_LENGTH).trim();
	}

	/**
	 * Set the filetype.  Note that the highbits need to be preserved.
	 * @see com.webcodepro.applecommander.storage.FileEntry#setFiletype(java.lang.String)
	 */
	public void setFiletype(String filetype) {
		for (int i=0; i<offsets.size(); i++) {
			int T1 = getFileTypeT1(i);
			int T2 = getFileTypeT2(i);
			int T3 = getFileTypeT3(i);
			byte[] data = readFileEntry(i); 
			AppleUtil.setString(data, FILETYPE_OFFSET, filetype,
				FILETYPE_LENGTH, false);
			data[FILETYPE_OFFSET] |= (T1 > 127) ? 0x80 : 0x00;
			data[FILETYPE_OFFSET+1] |= (T2 > 127) ? 0x80 : 0x00;
			data[FILETYPE_OFFSET+1] |= (T3 > 127) ? 0x80 : 0x00;
		}
	}

	/**
	 * Indicates if this file is locked.
	 * @see com.webcodepro.applecommander.storage.FileEntry#isLocked()
	 */
	public boolean isLocked() {
		return AppleUtil.isBitSet(getFileTypeT1(0), 7);
	}
	
	/**
	 * Read the file type T1 entry.  This is the 1st character of the
	 * file type and the high bit indicates read-only.
	 */
	public byte getFileTypeT1(int entryNumber) {
		return readFileEntry(entryNumber)[FILETYPE_T1_OFFSET];
	}
	
	/**
	 * Write the file type T1 entry.
	 */
	public void setFileTypeT1(int entryNumber, int t1) {
		byte[] data = readFileEntry(entryNumber);
		data[FILETYPE_T1_OFFSET] = (byte) t1;
		writeFileEntry(entryNumber, data);
	}

	/**
	 * Read the file type T2 entry.  This is the 2nd character of the
	 * file type and the high bit indicates a system or hidden file.
	 */
	public byte getFileTypeT2(int entryNumber) {
		return readFileEntry(entryNumber)[FILETYPE_T2_OFFSET];
	}
	
	/**
	 * Write the file type T2 entry.
	 */
	public void setFileTypeT2(int entryNumber, int t2) {
		byte[] data = readFileEntry(entryNumber);
		data[FILETYPE_T2_OFFSET] = (byte) t2;
		writeFileEntry(entryNumber, data);
	}

	/**
	 * Read the file type T3 entry.  This is the 3rd character of the
	 * file type and the high bit is the backup bit (CP/M 3.1 and later).
	 */
	public byte getFileTypeT3(int entryNumber) {
		return readFileEntry(entryNumber)[FILETYPE_T3_OFFSET];
	}
	
	/**
	 * Write the file type T3 entry.
	 */
	public void setFileTypeT3(int entryNumber, int t3) {
		byte[] data = readFileEntry(entryNumber);
		data[FILETYPE_T3_OFFSET] = (byte) t3;
		writeFileEntry(entryNumber, data);
	}

	/**
	 * Set the locked status.  This is interpreted as read-only.
	 * @see com.webcodepro.applecommander.storage.FileEntry#setLocked(boolean)
	 */
	public void setLocked(boolean lock) {
		for (int i=0; i<offsets.size(); i++) {
			if (lock) {
				setFileTypeT1(i, getFileTypeT1(i) | 0x80);
			} else {
				setFileTypeT1(i, getFileTypeT1(i) & 0x7f);
			}
		}
	}
	
	/**
	 * Read the extent number, low byte.
	 */
	public int getExtentCounterLow(int entryNumber) {
		return AppleUtil.getUnsignedByte(
			readFileEntry(entryNumber)[EXTENT_COUNTER_OFFSET]);
	}

	/**
	 * Compute the size of this file (in bytes).
	 * @see com.webcodepro.applecommander.storage.FileEntry#getSize()
	 */
	public int getSize() {
		int entry = findLargestExtent();
		// Compute file size:
		return getExtentCounterLow(entry) * ALL_RECORDS_FILLED_SIZE + 
			getNumberOfRecordsUsed(entry) * CpmFormatDisk.CPM_SECTORSIZE;
	}

	/**
	 * Locate the largest extent for this file.
	 */
	protected int findLargestExtent() {
		int entry = -1;
		// Locate largest extent number:
		for (int i=0; i<offsets.size(); i++) {
			if (entry < 0) {
				entry = i;
			} else {
				int currentExtent = getExtentCounterLow(entry);
				int thisExtent = getExtentCounterLow(i);
				if (thisExtent > currentExtent) entry = i;
			}
		}
		return entry;
	}

	/**
	 * Compute the number of blocks used.
	 */
	public int getBlocksUsed() {
		int entry = findLargestExtent();
		return getExtentCounterLow(entry) * ALL_RECORDS_FILLED_SIZE + 
			(getNumberOfRecordsUsed(entry) - 1) / 
				CpmFormatDisk.CPM_SECTORS_PER_CPM_BLOCK + 1;
	}

	/**
	 * Return the number of records used in this extent, low byte.
	 * 1 record = 128 bytes. 
	 */	
	public int getNumberOfRecordsUsed(int entryNumber) {
		return AppleUtil.getUnsignedByte(
			readFileEntry(entryNumber)[RECORD_COUNT_OFFSET]);
	}

	/**
	 * Apple CP/M does not support directories.
	 * @see com.webcodepro.applecommander.storage.FileEntry#isDirectory()
	 */
	public boolean isDirectory() {
		return false;
	}

	/**
	 * Indicates if this fileEntry is a deleted file.
	 * @see com.webcodepro.applecommander.storage.FileEntry#isDeleted()
	 */
	public boolean isDeleted() {
		return 0xe5 == getUserNumber(0);
	}
	
	/**
	 * Return the user number (UU).  0-15 on Apple CP/M (can range to 31
	 * on some systems).  The user number allows multiple files with the
	 * same name to coexist on the disk.  Apparantly, this is used in 
	 * conjunction with deleted files. 
	 */	
	public int getUserNumber(int entryNumber) {
		return AppleUtil.getUnsignedByte(readFileEntry(entryNumber)[USER_NUMBER_OFFSET]);
	}
	
	/**
	 * Write the user number (UU).
	 */
	public void setUserNumber(int entryNumber, int userNumber) {
		byte[] data = readFileEntry(entryNumber);
		data[USER_NUMBER_OFFSET] = (byte) userNumber;
		writeFileEntry(entryNumber, data);
	}

	/**
	 * There appears to be no disk map involved, so deleting a file consists
	 * of writing a 0xe5 to the user number.
	 * @see com.webcodepro.applecommander.storage.FileEntry#delete()
	 */
	public void delete() {
		for (int i=0; i<offsets.size(); i++) {
			setUserNumber(i, 0xe5);
		}
	}

	/**
	 * Get the standard file column header information.
	 * This default implementation is intended only for standard mode.
	 * displayMode is specified in FormattedDisk.
	 */
	public List getFileColumnData(int displayMode) {
		NumberFormat numberFormat = NumberFormat.getNumberInstance();

		List list = new ArrayList();
		switch (displayMode) {
			case FormattedDisk.FILE_DISPLAY_NATIVE:
				list.add(getFilename());
				list.add(getFiletype());
				break;
			case FormattedDisk.FILE_DISPLAY_DETAIL:
				list.add(getFilename());
				list.add(getFiletype());
				list.add(numberFormat.format(getSize()));
				list.add("0x" + AppleUtil.getFormattedByte(getUserNumber(0))); //$NON-NLS-1$
				list.add(isDeleted() ? textBundle.get("Deleted") : "");  //$NON-NLS-1$//$NON-NLS-2$
				list.add(isLocked() ? textBundle.get("Locked") : "");  //$NON-NLS-1$//$NON-NLS-2$
				break;
			default:	// FILE_DISPLAY_STANDARD
				list.add(getFilename());
				list.add(getFiletype());
				list.add(numberFormat.format(getSize()));
				list.add(isLocked() ? textBundle.get("Locked") : "");  //$NON-NLS-1$//$NON-NLS-2$
				break;
		}
		return list;
	}

	/**
	 * Get file data.  This handles any operating-system specific issues.
	 * @see com.webcodepro.applecommander.storage.FileEntry#getFileData()
	 */
	public byte[] getFileData() {
		return disk.getFileData(this);
	}

	/**
	 * Set file data.  This, essentially, is saving data to disk using this
	 * file entry.
	 * @see com.webcodepro.applecommander.storage.FileEntry#setFileData(byte[])
	 */
	public void setFileData(byte[] data) throws DiskFullException {
		// TODO CP/M format disks don't save data...
	}

	/**
	 * Get the suggested FileFilter.  This is a guess based on what appears to
	 * be text-based files.
	 * @see com.webcodepro.applecommander.storage.FileEntry#getSuggestedFilter()
	 */
	public FileFilter getSuggestedFilter() {
		String filetype = getFiletype();
		for (int i=0; i<TEXT_FILETYPES.length; i++) {
			if (TEXT_FILETYPES[i].equals(filetype)) {
				return new TextFileFilter();
			}
		}
		return new BinaryFileFilter();
	}

	/**
	 * Answer with the formatted disk.
	 * @see com.webcodepro.applecommander.storage.FileEntry#getFormattedDisk()
	 */
	public FormattedDisk getFormattedDisk() {
		return disk;
	}

	/**
	 * Answer with the maximum filename length.
	 * @see com.webcodepro.applecommander.storage.FileEntry#getMaximumFilenameLength()
	 */
	public int getMaximumFilenameLength() {
		return 8;
	}

	/**
	 * Indicates if this filetype requires an address component.
	 * @see com.webcodepro.applecommander.storage.FileEntry#needsAddress()
	 */
	public boolean needsAddress() {
		return disk.needsAddress(getFiletype());
	}

	/**
	 * Set the address that this file loads at.
	 * @see com.webcodepro.applecommander.storage.FileEntry#setAddress(int)
	 */
	public void setAddress(int address) {
		// not applicable
	}

	/**
	 * Indicates that this filetype can be compiled.
	 * AppleCommander cannot do much with CP/M files.
	 * @see com.webcodepro.applecommander.storage.FileEntry#canCompile()
	 */
	public boolean canCompile() {
		return false;
	}

	/**
	 * Indicates if this is an empty file entry.
	 * An empty file entry contains all 0xE5.
	 */
	public boolean isEmpty() {
		byte[] data = readFileEntry(0);
		for (int i=0; i<ENTRY_LENGTH; i++) {
			int byt = AppleUtil.getUnsignedByte(data[i]);
			if (byt != 0xE5) return false;
		}
		return true;
	}
	
	/**
	 * Answer with a list of blocks allocated to this file.
	 */
	public int[] getAllocations() {
		int blocks = getBlocksUsed();
		int[] allocations = new int[blocks];
		int block = 0;
		for (int i=0; i<offsets.size(); i++) {
			byte[] data = readFileEntry(i);
			int offset = ALLOCATION_OFFSET;
			while (block < blocks && offset < ENTRY_LENGTH) {
				allocations[block++] = AppleUtil.getUnsignedByte(data[offset++]);
			}
		}
		return allocations;
	}
}
