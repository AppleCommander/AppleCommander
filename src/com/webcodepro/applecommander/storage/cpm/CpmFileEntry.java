package com.webcodepro.applecommander.storage.cpm;

import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.util.AppleUtil;

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
	 * The offset into the block that the FileEntry is at.
	 */
	private int offset;
	
	/**
	 * Construct a CP/M file entry.
	 */
	public CpmFileEntry(CpmFormatDisk disk, int offset) {
		this.disk = disk;
		this.offset = offset;
	}

	/**
	 * Read the fileEntry bytes from the disk image.
	 */
	protected byte[] readFileEntry() {
		byte[] data = new byte[2048];
		System.arraycopy(disk.readCpmBlock(0), 0, data, 0, 1024);
		System.arraycopy(disk.readCpmBlock(1), 0, data, 1024, 1024);
		byte[] entry = new byte[ENTRY_LENGTH];
		System.arraycopy(data, offset, entry, 0, ENTRY_LENGTH);
		return entry;
	}

	/**
	 * Answer with the name of the file.
	 * @see com.webcodepro.applecommander.storage.FileEntry#getFilename()
	 */
	public String getFilename() {
		return AppleUtil.getString(readFileEntry(), 1, 8).trim();
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
		return AppleUtil.getString(readFileEntry(), 9, 3).trim();
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
		return AppleUtil.isBitSet(readFileEntry()[0x9], 8);
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FileEntry#setLocked(boolean)
	 */
	public void setLocked(boolean lock) {
		// TODO Auto-generated method stub

	}

	/**
	 * @see com.webcodepro.applecommander.storage.FileEntry#getSize()
	 */
	public int getSize() {
		// TODO Auto-generated method stub
		return 0;
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
		return 0xe5 == AppleUtil.getUnsignedByte(readFileEntry()[0]);
	}

	/**
	 * @see com.webcodepro.applecommander.storage.FileEntry#delete()
	 */
	public void delete() {
		// TODO Auto-generated method stub

	}

	/**
	 * @see com.webcodepro.applecommander.storage.FileEntry#getFileColumnData(int)
	 */
	public List getFileColumnData(int displayMode) {
		// TODO Auto-generated method stub
		return null;
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
		byte[] data = readFileEntry();
		for (int i=0; i<ENTRY_LENGTH; i++) {
			int byt = AppleUtil.getUnsignedByte(data[i]);
			if (byt != 0xE5) return false;
		}
		return true;
	}
}
