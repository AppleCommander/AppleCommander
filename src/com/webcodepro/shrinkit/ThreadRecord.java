package com.webcodepro.shrinkit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.webcodepro.shrinkit.io.LittleEndianByteInputStream;
import com.webcodepro.shrinkit.io.NufxLzw1InputStream;
import com.webcodepro.shrinkit.io.NufxLzw2InputStream;

/**
 * This represents a single thread from the Shrinkit archive.
 * As it is constructed, the thread "header" is read.  Once all
 * threads have been constructed, use <code>readThreadData</code>
 * to load up the data.
 * <p>
 * Depending on the type of thread, the data may be text.  If so,
 * <code>isText</code> will return true and <code>getText</code>
 * will return the string. Otherwise the data should be read through
 * one of the <code>InputStream</code> options.
 * 
 * @author robgreene@users.sourceforge.net
 */
public class ThreadRecord {
	private ThreadClass threadClass;
	private ThreadFormat threadFormat;
	private ThreadKind threadKind;
	private int threadCrc;
	private long threadEof;
	private long compThreadEof;
	private byte[] threadData;

	/**
	 * Construct the ThreadRecord and read the header details with no hints
	 * from the Header Block.
	 */
	public ThreadRecord(LittleEndianByteInputStream bs) throws IOException {
		this(null, bs);
	}

	/**
	 * Construct the ThreadRecord and read the header details.
	 */
	public ThreadRecord(HeaderBlock hb, LittleEndianByteInputStream bs) throws IOException {
		threadClass = ThreadClass.find(bs.readWord());
		threadFormat = ThreadFormat.find(bs.readWord());
		threadKind = ThreadKind.find(bs.readWord(), threadClass);
		threadCrc = bs.readWord();
		threadEof = bs.readLong();
		compThreadEof = bs.readLong();
		if ((threadKind == ThreadKind.DISK_IMAGE) && (hb != null)) {
			/* If we have hints from the header block, repair some disk image related bugs. */
			if (hb.getStorageType() <= 13 ) {
				/* supposed to be block size, but SHK v3.0.1 stored it wrong */
				threadEof = hb.getExtraType() * 512;
				// System.out.println("Found erroneous storage type... fixing.");
			} else if (hb.getStorageType() == 256 &&
					hb.getExtraType() == 280 &&
					hb.getFileSysId() == 2 ) { // FileSysDOS33
				/*
				 * Fix for less-common ShrinkIt problem: looks like an old
				 * version of GS/ShrinkIt used 256 as the block size when
				 * compressing DOS 3.3 images from 5.25" disks.  If that
				 * appears to be the case here, crank up the block size.
				 */
				threadEof = hb.getExtraType() * 512;
			} else {
				threadEof = hb.getExtraType() * hb.getStorageType();
			}
		}
	}

	/**
	 * Read the raw thread data.  This must be called.
	 */
	public void readThreadData(LittleEndianByteInputStream bs) throws IOException {
		threadData = bs.readBytes((int)compThreadEof);
	}
	/**
	 * Determine if this is a text-type field.
	 */
	public boolean isText() {
		return threadKind == ThreadKind.ASCII_TEXT || threadKind == ThreadKind.FILENAME;
	}
	/**
	 * Return the text data.
	 */
	public String getText() {
		return isText() ? new String(threadData, 0, (int)threadEof) : null;
	}
	/**
	 * Get raw data bytes (compressed).
	 */
	public byte[] getBytes() {
		return threadData;
	}
	/**
	 * Get the raw data input stream.
	 */
	public InputStream getRawInputStream() {
		return new ByteArrayInputStream(threadData);
	}
	/**
	 * Get the appropriate input data stream for this thread to decompress the contents.
	 */
	public InputStream getInputStream() throws IOException {
		switch (threadFormat) {
		case UNCOMPRESSED:
			return getRawInputStream();
		case DYNAMIC_LZW1:
			return new NufxLzw1InputStream(new LittleEndianByteInputStream(getRawInputStream()));
		case DYNAMIC_LZW2:
			return new NufxLzw2InputStream(new LittleEndianByteInputStream(getRawInputStream()));
		default:
			throw new IOException("The thread format " + threadFormat + " does not have an InputStream associated with it!");
		}
	}
	
	// GENERATED CODE
	
	public ThreadClass getThreadClass() {
		return threadClass;
	}
	public void setThreadClass(ThreadClass threadClass) {
		this.threadClass = threadClass;
	}
	public ThreadFormat getThreadFormat() {
		return threadFormat;
	}
	public void setThreadFormat(ThreadFormat threadFormat) {
		this.threadFormat = threadFormat;
	}
	public ThreadKind getThreadKind() {
		return threadKind;
	}
	public void setThreadKind(ThreadKind threadKind) {
		this.threadKind = threadKind;
	}
	public int getThreadCrc() {
		return threadCrc;
	}
	public void setThreadCrc(int threadCrc) {
		this.threadCrc = threadCrc;
	}
	public long getThreadEof() {
		return threadEof;
	}
	public void setThreadEof(long threadEof) {
		this.threadEof = threadEof;
	}
	public long getCompThreadEof() {
		return compThreadEof;
	}
	public void setCompThreadEof(long compThreadEof) {
		this.compThreadEof = compThreadEof;
	}
	public byte[] getThreadData() {
		return threadData;
	}
	public void setThreadData(byte[] threadData) {
		this.threadData = threadData;
	}
}
