/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002 by Robert Greene
 * robgreene at users.sourceforge.net
 * Copyright (C) 2004 by John B. Matthews
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

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.StorageBundle;
import com.webcodepro.applecommander.storage.filters.BinaryFileFilter;
import com.webcodepro.applecommander.storage.filters.GraphicsFileFilter;
import com.webcodepro.applecommander.storage.filters.PascalTextFileFilter;
import com.webcodepro.applecommander.storage.filters.TextFileFilter;
import com.webcodepro.applecommander.util.AppleUtil;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Represents a Pascal file entry on disk.
 * <p>
 * Date created: Oct 5, 2002 12:22:34 AM
 * @author Rob Greene
 * @author John B. Matthews
 */
public class PascalFileEntry implements FileEntry {
	private TextBundle textBundle = StorageBundle.getInstance();
	private byte[] fileEntry;
	private PascalFormatDisk disk;
	private int index = 0;
	private boolean deleted = false;

	/**
	 * Constructor for PascalFileEntry.
	 */
	public PascalFileEntry(byte[] fileEntry, PascalFormatDisk disk) {
		super();
		this.fileEntry = fileEntry;
		this.disk = disk;
	}

	/**
	 * Get the block number of the file's 1st block.
	 */
	public int getFirstBlock() {
		return AppleUtil.getWordValue(fileEntry, 0);
	}

 	/**
	 * Set the block number of the file's 1st block.
	 */
	public void setFirstBlock(int first) {
		AppleUtil.setWordValue(fileEntry, 0, first);
	}

	/**
	 * Get the block number of the file's last block + 1.
	 */
	public int getLastBlock() {
		return AppleUtil.getWordValue(fileEntry, 2);
	}

 	/**
	 * Set the block number of the file's last block + 1.
	 */
	public void setLastBlock(int last) {
		AppleUtil.setWordValue(fileEntry, 2, last);
	}

	/**
	 * Return the name of this file.
	 */
	public String getFilename() {
		return AppleUtil.getPascalString(fileEntry, 6);
	}

	/**
	 * Set the name of this file.
	 */
	public void setFilename(String filename) {
		AppleUtil.setPascalString(fileEntry, 6, filename.toUpperCase(), 15);
	}

	/**
	 * Return the maximum filename length.
	 */
	public int getMaximumFilenameLength() {
		return 15;
	}

	/**
	 * Return the filetype of this file.
	 */
	public String getFiletype() {
		String[] filetypes = disk.getFiletypes();
		int filetype = fileEntry[4] & 0x0f;
		if (filetype == 0 || filetype > filetypes.length) {
			return textBundle.format("PascalFileEntry.UnknownFiletype", filetype); //$NON-NLS-1$
		}
		return filetypes[filetype-1];
	}

	/**
	 * Set the filetype.
	 */
	public void setFiletype(String filetype) {
		if ("bad".equalsIgnoreCase(filetype)) { //$NON-NLS-1$
			AppleUtil.setWordValue(fileEntry, 4, 1);
		} else if ("code".equalsIgnoreCase(filetype)) { //$NON-NLS-1$
			AppleUtil.setWordValue(fileEntry, 4, 2);
		} else if ("text".equalsIgnoreCase(filetype)) { //$NON-NLS-1$
			AppleUtil.setWordValue(fileEntry, 4, 3);
		} else if ("info".equalsIgnoreCase(filetype)) { //$NON-NLS-1$
			AppleUtil.setWordValue(fileEntry, 4, 4);
		} else if ("data".equalsIgnoreCase(filetype)) { //$NON-NLS-1$
			AppleUtil.setWordValue(fileEntry, 4, 5);
		} else if ("graf".equalsIgnoreCase(filetype)) { //$NON-NLS-1$
			AppleUtil.setWordValue(fileEntry, 4, 6);
		} else if ("foto".equalsIgnoreCase(filetype)) { //$NON-NLS-1$
			AppleUtil.setWordValue(fileEntry, 4, 7);
		} else {
			AppleUtil.setWordValue(fileEntry, 4, 0);
		}
	}

	/**
	 * Identify if this file is locked
	 */
	public boolean isLocked() {
		return false; // Not applicable to UCSD file system
	}

	/**
	 * Set the lock indicator.
	 */
	public void setLocked(boolean lock) {
		// Not applicable to UCSD file system
	}

	/**
	 * Get the number of bytes used in files last block.
	 */
	public int getBytesUsedInLastBlock() {
		return AppleUtil.getWordValue(fileEntry, 22);
	}

	/**
	 * Set the number of bytes used in files last block.
	 */
	public void setBytesUsedInLastBlock(int value) {
		AppleUtil.setWordValue(fileEntry, 22, value);
	}

	/**
	 * Compute the size of this file (in bytes).
	 */
	public int getSize() {
		int blocks = getBlocksUsed() - 1;
		return blocks*Disk.BLOCK_SIZE + getBytesUsedInLastBlock();
	}

	/**
	 * Compute the blocks used.
	 */
	public int getBlocksUsed() {
		return AppleUtil.getWordValue(fileEntry, 2) - AppleUtil.getWordValue(fileEntry, 0);
	}

	/**
	 * Pascal does not support directories.
	 */
	public boolean isDirectory() {
		return false;
	}

	/**
	 * Retrieve the list of files in this directory.
	 * Always returns null, as Pascal does not support directories.
	 */
	public List getFiles() {
		return null;
	}

	/**
	 * Pascal file entries are removed upon deletion,
	 * so a file entry need not be marked as deleted.
	 * But the GUI still has a copy of the file list in
	 * memory, so we mark it deleted in delete(). 
	 */
	public boolean isDeleted() {
		return deleted;
	}

	/**
	 * Delete the file.
	 */
	public void delete() {
		int index = 0;
		String dname = this.getFilename();
		List dir = disk.getDirectory();
		int count = dir.size();
		// find the index of the matching entry
		for (int i = 1; i < count; i++) {
			String fname = ((PascalFileEntry) dir.get(i)).getFilename();
			if (dname.equals(fname)) {
				index = i;
			}
		}
		if (index != 0) {
			dir.remove(index);
			PascalFileEntry volEntry = (PascalFileEntry) dir.get(0);
			volEntry.setFileCount(count - 2); // inlcudes the volume entry
			dir.set(0, volEntry);
			disk.putDirectory(dir);
			deleted = true;
		}
	}

	/**
	 * Get the file modification date.
	 */
	public Date getModificationDate() {
		return AppleUtil.getPascalDate(fileEntry, 24);
	}

	/**
	 * Set the file modification date.
	 */
	public void setModificationDate(Date date) {
		AppleUtil.setPascalDate(fileEntry, 24, date);
	}

	/**
	 * Get the standard file column header information.
	 * This default implementation is intended only for standard mode.
	 * displayMode is specified in FormattedDisk.
	 */
	public List getFileColumnData(int displayMode) {
		NumberFormat numberFormat = NumberFormat.getNumberInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				textBundle.get("PascalFileEntry.PascalDateFormat")); //$NON-NLS-1$

		List list = new ArrayList();
		switch (displayMode) {
			case FormattedDisk.FILE_DISPLAY_NATIVE:
				list.add(dateFormat.format(getModificationDate()));
				numberFormat.setMinimumIntegerDigits(3);
				list.add(numberFormat.format(getBlocksUsed()));
				list.add(getFiletype());
				list.add(getFilename());
				break;
			case FormattedDisk.FILE_DISPLAY_DETAIL:
				list.add(dateFormat.format(getModificationDate()));
				numberFormat.setMinimumIntegerDigits(3);
				list.add(numberFormat.format(getBlocksUsed()));
				numberFormat.setMinimumIntegerDigits(1);
				list.add(numberFormat.format(getBytesUsedInLastBlock()));
				list.add(numberFormat.format(getSize()));
				list.add(getFiletype());
				list.add(getFilename());
				numberFormat.setMinimumIntegerDigits(3);
				list.add(numberFormat.format(getFirstBlock()));
				list.add(numberFormat.format(getLastBlock()-1));
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
	 * Currently, the disk itself handles this.
	 */
	public byte[] getFileData() {
		return disk.getFileData(this);
	}

	/**
	 * Filter text: change CR/LF to CR; compress leading SP.
	 * author John B. Matthews
	 */
	private byte[] filterText(byte[] data)  {
		final byte LF  = 0x0a; final byte CR = 0x0d;
		final byte DLE = 0x10; final byte SP = 0x20;
		ByteArrayOutputStream buf = new ByteArrayOutputStream(data.length);
		int index = 0;
		while (index < data.length) {
			byte b = data[index];
			if (b == CR || b == LF) {
				buf.write(CR);
				index++;
				if (b == CR && index < data.length && data[index] == LF) index++;
				byte spaceCount = SP;
				while (index < data.length && data[index] == SP) {
					spaceCount++; index++;
				}
				if (spaceCount > SP) {
					buf.write(DLE);
					buf.write(spaceCount);
				}
			} else {
				buf.write(b);
				index++;
			}
		}
		return buf.toByteArray();
	}

	/**
	 * Delete this temporary entry, inserted by PascalFormatDisk.createFile(),
	 * and exit via DiskFullException.
	 * @author John B. Matthews
	 */
	private void storageError(String s) throws DiskFullException {
		if (this.index > 0) {
			List dir = disk.getDirectory();
			int count = dir.size();
			dir.remove(this.index);
			PascalFileEntry volEntry = (PascalFileEntry) dir.get(0);
			volEntry.setFileCount(count - 2);
			dir.set(0, volEntry);
			disk.putDirectory(dir);
			throw new DiskFullException(s);
		}
	}

	/**
	 * Find index of last CR < 1023 bytes from offset.
	 * @author John B. Matthews
	 */
	private int findEOL(byte[] data, int offset) throws DiskFullException  {
		int i = offset + 1022;
		while (i > offset) {
			if (data[i] == 13) {
				return i;
			}
			i--;
		}
		storageError(textBundle.get("PascalFileEntry.LineLengthError")); //$NON-NLS-1$
		return 0;
	}

	/**
	 * Set file data for this file entry. Because the directory entry may
	 * have been changed, use this.index to determine which entry to update.
	 * author John B. Matthews.
	 * @see #setEntryIndex
	 * @see PascalFormatDisk#createFile
	 */
	public void setFileData(byte[] data) throws DiskFullException {
		int first = getFirstBlock();
		int last = getLastBlock();
		if (fileEntry[4] == 3) { // text
			data = filterText(data);
			byte[] buf1 = new byte[512];
			byte[] buf2 = new byte[512];
			int offset = 0;
			int pages = 0;
			disk.writeBlock(first, buf1);  // First two blocks (first page) is ignored by Pascal text
			disk.writeBlock(first+1,buf2);  // ...so write a page of zeroes
			pages++;
			while (offset + 1023 < data.length) {  // We have at least one full page of data (1024 bytes)
				if ((pages * 2) > (last - first - 2)) {
					storageError(textBundle.get("PascalFileEntry.NotEnoughRoom")); //$NON-NLS-1$
				}
				int crPtr = findEOL(data, offset);
				System.arraycopy(data, offset, buf1, 0, 512);
				System.arraycopy(data, offset+512, buf2, 0, crPtr - offset + 1 - 512);
				disk.writeBlock(first + (pages * 2), buf1);
				disk.writeBlock(first + (pages * 2) + 1, buf2);
				pages++;
				Arrays.fill(buf1, (byte) 0);
				Arrays.fill(buf2, (byte) 0);
				offset = crPtr + 1;
			}
			if (offset < data.length) {  // We have less than a full page of data left over
				int len1 = data.length - offset;
				int len2 = 0;
				if (len1 > 512) {  // That final page spans both blocks
					len2 = len1 - 512;  // Second block gets the remainder of the partial page length minus 512 bytes 
					len1 = 512;  // The first block will write the first 512 bytes
				}
				System.arraycopy(data, offset, buf1, 0, len1);
				disk.writeBlock(first + (pages * 2), buf1);  // Copy out the first block
				if (len2 > 0) { 
					System.arraycopy(data, offset+512, buf2, 0, len2);
					disk.writeBlock(first + (pages * 2) + 1, buf2);  // Copy out second block
					setBytesUsedInLastBlock(len2);  // The second block holds the last byte
					setLastBlock(first + (pages * 2) + 2);  // Final block +1... i.e. pages++
				}
				else {  // The first block holds the last byte
					setBytesUsedInLastBlock(len1);
					setLastBlock(first + pages * 2 + 1);  // Final block +1... i.e. pages++ -1
				}
			}
			else {  // The last page was completely full, so the last byte used in the last block is 512
				setLastBlock(first + pages * 2);
				setBytesUsedInLastBlock(512);
			}
		} else { // data or code
			if (data.length > (last - first) * 512) {
				storageError(textBundle.get("PascalFileEntry.NotEnoughRoom")); //$NON-NLS-1$
			}
			byte[] buf = new byte[512];
			int blocks = data.length / 512;
			int bytes = data.length % 512;
			for (int i = 0; i < blocks; i++) {
				System.arraycopy(data, i * 512, buf, 0, 512);
				disk.writeBlock(first + i, buf);
			}
			if (bytes > 0) {
				Arrays.fill(buf, (byte) 0);
				System.arraycopy(data, blocks * 512, buf, 0, bytes);
				disk.writeBlock(first + blocks, buf);
				setLastBlock(first + blocks + 1);
				setBytesUsedInLastBlock(bytes);
			} else {
				setLastBlock(first + blocks);
				setBytesUsedInLastBlock(512);
			}
		}
		// update this directory entry
		if (this.index > 0) {
			List dir = disk.getDirectory();
			dir.set(this.index, this);
			disk.putDirectory(dir);
		}
	}

	/**
	 * Get the suggested FileFilter.  This appears to be operating system
	 * specific, so each operating system needs to implement some manner
	 * of guessing the appropriate filter.
	 */
	public FileFilter getSuggestedFilter() {
		if ("TEXT".equals(getFiletype())) { //$NON-NLS-1$
			if (getFilename().toLowerCase().endsWith(".text")) { //$NON-NLS-1$
				return new PascalTextFileFilter();
			}
			return new TextFileFilter();
		} else if ("DATA".equals(getFiletype()) && getSize() >= 8184 && getSize() <= 8192) { //$NON-NLS-1$
			GraphicsFileFilter filter = new GraphicsFileFilter();
			filter.setMode(GraphicsFileFilter.MODE_HGR_COLOR);
			return filter;
		}
		return new BinaryFileFilter();
	}

	/**
	 * Get the FormattedDisk associated with this FileEntry.
	 * This is useful to interfaces that need to retrieve the associated
	 * disk.
	 */
	public FormattedDisk getFormattedDisk() {
		return disk;
	}

	/**
	 * Get the byte[] associated with this FileEntry.
	 * This is need to manipuate the directory as a whole.
	 */
	public byte[] toBytes() {
		return fileEntry;
	}

	/**
	 * Indicates if this filetype requires an address component.
	 * Note that the FormattedDisk also has this method - normally,
	 * this will defer to the method on FormattedDisk, as it will be
	 * more generic.
	 */
	public boolean needsAddress() {
		return false;
	}

	/**
	 * Set the address that this file loads at.
	 */
	public void setAddress(int address) {
		// Does not apply.
	}

	/**
	 * Indicates that this filetype can be compiled.
	 */
	public boolean canCompile() {
		return false;
	}

	/**
	 * Remember the index of a newly created file entry.
	 * Required to update the entry after setFileData,
	 * which may change any or all of the new entry's fields.
	 * author John B. Matthews
	 */
	public void setEntryIndex(int index) {
		this.index = index;
	}

	/**
	 * Set the file count in a volume entry.
	 * Use only on the volume entry: dir.get(0).
	 * author John B. Matthews
	 */
	public void setFileCount(int count) {
		AppleUtil.setWordValue(fileEntry, 16, count);
	}
}
