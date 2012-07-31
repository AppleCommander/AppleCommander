package com.webcodepro.shrinkit;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.webcodepro.shrinkit.io.LittleEndianByteInputStream;

/**
 * Basic reading of a NuFX archive.
 * 
 * @author robgreene@users.sourceforge.net
 */
public class NuFileArchive {
	private MasterHeaderBlock master;
	private List<HeaderBlock> headers;
	private long totalSize = 0;

	/**
	 * Need to enumerate some basic sub-types of archives.
	 */
	public static final int NUFILE_ARCHIVE = 1;
	public static final int NUFX_ARCHIVE = 2;
	public static final int BXY_ARCHIVE = 3;

	/**
	 * Read in the NuFile/NuFX/Shrinkit archive.
	 */
	public NuFileArchive(InputStream inputStream) throws IOException {
		LittleEndianByteInputStream bs = new LittleEndianByteInputStream(inputStream);
		master = new MasterHeaderBlock(bs);
		headers = new ArrayList<HeaderBlock>();
		for (int i=0; i<master.getTotalRecords(); i++) {
			HeaderBlock header = new HeaderBlock(bs);
			header.readThreads(bs);
			headers.add(header);
			totalSize += header.getHeaderSize();
		}
	}

	/**
	 * @return long size in bytes of the archive
	 */
	public long getArchiveSize() {
		return totalSize;
	}

	public MasterHeaderBlock getMasterHeaderBlock() {
		return master;
	}
	public List<HeaderBlock> getHeaderBlocks() {
		return headers;
	}}
