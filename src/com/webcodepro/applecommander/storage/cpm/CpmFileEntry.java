package com.webcodepro.applecommander.storage.cpm;

import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.util.AppleUtil;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Rob
 */
public class CpmFileEntry implements FileEntry {
	/**
	 * The standard CP/M file entry length.
	 */
	public static final int ENTRY_LENGTH = 0x20;
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
		byte[] data = new byte[2048];
		System.arraycopy(disk.readCpmBlock(0), 0, data, 0, 1024);
		System.arraycopy(disk.readCpmBlock(1), 0, data, 1024, 1024);
		byte[] entry = new byte[ENTRY_LENGTH];
		int offset = ((Integer)offsets.get(number)).intValue();
		System.arraycopy(data, offset, entry, 0, ENTRY_LENGTH);
		return entry;
	}

	/**
	 * Answer with the name of the file.
	 * @see com.webcodepro.applecommander.storage.FileEntry#getFilename()
	 */
	public String getFilename() {
		return AppleUtil.getString(readFileEntry(0), 1, 8).trim();
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FileEntry#setFilename(java.lang.String)
	 */
	public void setFilename(String filename) {
		// TODO Auto-generated method stub

	}

	/**
	 * Answer with the filetype.
	 * @see com.webcodepro.applecommander.storage.FileEntry#getFiletype()
	 */
	public String getFiletype() {
		return AppleUtil.getString(readFileEntry(0), 9, 3).trim();
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FileEntry#setFiletype(java.lang.String)
	 */
	public void setFiletype(String filetype) {
		// TODO Auto-generated method stub

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
		return readFileEntry(entryNumber)[0x9];
	}

	/**
	 * Read the file type T2 entry.  This is the 2nd character of the
	 * file type and the high bit indicates a system or hidden file.
	 */
	public byte getFileTypeT2(int entryNumber) {
		return readFileEntry(entryNumber)[0xa];
	}

	/**
	 * Read the file type T3 entry.  This is the 3rd character of the
	 * file type and the high bit is the backup bit (CP/M 3.1 and later).
	 */
	public byte getFileTypeT3(int entryNumber) {
		return readFileEntry(entryNumber)[0xb];
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FileEntry#setLocked(boolean)
	 */
	public void setLocked(boolean lock) {
		// TODO Auto-generated method stub

	}
	
	/**
	 * Read the extent number, low byte.
	 */
	public int getExtentCounterLow(int entryNumber) {
		return AppleUtil.getUnsignedByte(readFileEntry(entryNumber)[0xc]);
	}

	/**
	 * Compute the size of this file (in bytes).
	 * @see com.webcodepro.applecommander.storage.FileEntry#getSize()
	 */
	public int getSize() {
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
		// Compute file size:
		return getExtentCounterLow(entry) * 16384 + 
			getNumberOfRecordsUsed(entry) * 128;
	}

	/**
	 * Return the number of records used in this extent, low byte.
	 * 1 record = 128 bytes. 
	 */	
	public int getNumberOfRecordsUsed(int entryNumber) {
		return AppleUtil.getUnsignedByte(readFileEntry(entryNumber)[0xf]);
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FileEntry#isDirectory()
	 */
	public boolean isDirectory() {
		// TODO Auto-generated method stub
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
		return AppleUtil.getUnsignedByte(readFileEntry(entryNumber)[0x0]);
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FileEntry#delete()
	 */
	public void delete() {
		// TODO Auto-generated method stub

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
				list.add("0x" + AppleUtil.getFormattedByte(getUserNumber(0)));
				list.add(isDeleted() ? "Deleted" : "");
				list.add(isLocked() ? "Locked" : "");
				break;
			default:	// FILE_DISPLAY_STANDARD
				list.add(getFilename());
				list.add(getFiletype());
				list.add(numberFormat.format(getSize()));
				list.add(isLocked() ? "Locked" : "");
				break;
		}
		return list;
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FileEntry#getFileData()
	 */
	public byte[] getFileData() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FileEntry#setFileData(byte[])
	 */
	public void setFileData(byte[] data) throws DiskFullException {
		// TODO Auto-generated method stub

	}

	/**
	 * @see com.webcodepro.applecommander.storage.FileEntry#getSuggestedFilter()
	 */
	public FileFilter getSuggestedFilter() {
		// TODO Auto-generated method stub
		return null;
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
	 * @see com.webcodepro.applecommander.storage.FileEntry#needsAddress()
	 */
	public boolean needsAddress() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FileEntry#setAddress(int)
	 */
	public void setAddress(int address) {
		// TODO Auto-generated method stub

	}

	/**
	 * @see com.webcodepro.applecommander.storage.FileEntry#canCompile()
	 */
	public boolean canCompile() {
		// TODO Auto-generated method stub
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
}
