package com.webcodepro.shrinkit;

import java.io.IOException;
import java.util.Date;

import com.webcodepro.shrinkit.io.ByteConstants;
import com.webcodepro.shrinkit.io.LittleEndianByteInputStream;

/**
 * The Master Header Block contains information about the entire
 * ShrinkIt archive.
 * <p>
 * Note that we need to support multiple versions of the NuFX
 * archive format.  Some details may be invalid, depending on
 * version, and those are documented in the getter methods.
 *  
 * @author robgreene@users.sourceforge.net
 * @see http://www.nulib.com/library/FTN.e08002.htm
 */
public class MasterHeaderBlock {
	private static final int MASTER_HEADER_LENGTH = 48;
	private int masterCrc;
	private boolean validCrc;
	private long totalRecords;
	private Date archiveCreateWhen;
	private Date archiveModWhen;
	private int masterVersion;
	private long masterEof;

	/**
	 * Create the Master Header Block, based on the LittleEndianByteInputStream.
	 */
	public MasterHeaderBlock(LittleEndianByteInputStream bs) throws IOException {
		int fileType = 0, headerOffset = 0;
		fileType = bs.seekFileType();
		
		if (fileType == NuFileArchive.BXY_ARCHIVE) {
			bs.readBytes(127 - ByteConstants.NUFILE_ID.length);
			headerOffset = 128;
			int count = bs.read();
			if (count != 0)
				throw new IOException("This is actually a Binary II archive with multiple files in it."); // FIXME - NLS
			fileType = bs.seekFileType();
		}
		if (!(fileType == NuFileArchive.NUFILE_ARCHIVE)) {
			throw new IOException("Unable to decode this archive."); // FIXME - NLS
		}
		masterCrc = bs.readWord();
		bs.resetCrc();	// CRC is computed from this point to the end of the header
		totalRecords = bs.readLong();
		archiveCreateWhen = bs.readDate();
		archiveModWhen = bs.readDate();
		masterVersion = bs.readWord();
		if (masterVersion > 0) {
			bs.readBytes(8);		// documented to be null, but we don't care
			masterEof = bs.readLong();
		} else {
			masterEof = -1;
		}
		// Read whatever remains of the fixed size header
		while (bs.getTotalBytesRead() < MASTER_HEADER_LENGTH + headerOffset) {
			bs.readByte();
		}
		validCrc = (masterCrc == bs.getCrcValue());
	}
	
	// GENERATED CODE

	public int getMasterCrc() {
		return masterCrc;
	}
	public void setMasterCrc(int masterCrc) {
		this.masterCrc = masterCrc;
	}
	public long getTotalRecords() {
		return totalRecords;
	}
	public void setTotalRecords(long totalRecords) {
		this.totalRecords = totalRecords;
	}
	public Date getArchiveCreateWhen() {
		return archiveCreateWhen;
	}
	public void setArchiveCreateWhen(Date archiveCreateWhen) {
		this.archiveCreateWhen = archiveCreateWhen;
	}
	public Date getArchiveModWhen() {
		return archiveModWhen;
	}
	public void setArchiveModWhen(Date archiveModWhen) {
		this.archiveModWhen = archiveModWhen;
	}
	public int getMasterVersion() {
		return masterVersion;
	}
	public void setMasterVersion(int masterVersion) {
		this.masterVersion = masterVersion;
	}
	public long getMasterEof() {
		return masterEof;
	}
	public void setMasterEof(long masterEof) {
		this.masterEof = masterEof;
	}
	public boolean isValidCrc() {
		return validCrc;
	}
}
