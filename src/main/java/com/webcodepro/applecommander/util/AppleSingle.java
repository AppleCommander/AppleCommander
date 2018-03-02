package com.webcodepro.applecommander.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Support reading of data from and AppleSingle source.
 * Does not implement all components at this time, extend as required.
 * 
 * @see https://github.com/AppleCommander/AppleCommander/issues/20
 */
public class AppleSingle {
	public static final int MAGIC_NUMBER = 0x0051600;
	public static final int VERSION_NUMBER = 0x00020000;
	
	private byte[] dataFork;
	private byte[] resourceFork;
	private String realName;
	private ProdosFileInfo prodosFileInfo;
	
	public AppleSingle(String filename) throws IOException {
		byte[] fileData = Files.readAllBytes(Paths.get(filename));
		load(fileData);
	}
	public AppleSingle(InputStream stream) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		StreamUtil.copy(stream, os);
		os.flush();
		load(os.toByteArray());
	}
	
	private void load(byte[] fileData) throws IOException {
		ByteBuffer buffer = ByteBuffer.wrap(fileData)
				.order(ByteOrder.BIG_ENDIAN)
				.asReadOnlyBuffer();
		required(buffer, MAGIC_NUMBER, "Not an AppleSingle file - magic number does not match.");
		required(buffer, VERSION_NUMBER, "Only AppleSingle version 2 supported.");
		buffer.position(buffer.position() + 16);	// Skip filler
		int entries = buffer.getShort();
		for (int i = 0; i < entries; i++) {
			int entryId = buffer.getInt();
			int offset = buffer.getInt();
			int length = buffer.getInt();
			buffer.mark();
			buffer.position(offset);
			byte[] entryData = new byte[length];
			buffer.get(entryData);
			if (entryId == 1) {
				dataFork = entryData;
			} else if (entryId == 2) {
				resourceFork = entryData;
			} else if (entryId == 11) {
				ByteBuffer infoData = ByteBuffer.wrap(entryData)
						.order(ByteOrder.BIG_ENDIAN)
						.asReadOnlyBuffer();
				int access = infoData.getShort();
				int fileType = infoData.getShort();
				int auxType = infoData.getInt();
				prodosFileInfo = new ProdosFileInfo(access, fileType, auxType);
			} else {
				throw new IOException(String.format("Unknown entry type of %04x", entryId));
			}
			buffer.reset();
		}
	}
	private void required(ByteBuffer buffer, int expected, String message) throws IOException {
		int actual = buffer.getInt();
		if (actual != expected) {
			throw new IOException(String.format("%s  Expected 0x%08x but read 0x%08x.", message, expected, actual));
		}
	}
	
	public byte[] getDataFork() {
		return dataFork;
	}
	public byte[] getResourceFork() {
		return resourceFork;
	}
	public String getRealName() {
		return realName;
	}
	public ProdosFileInfo getProdosFileInfo() {
		return prodosFileInfo;
	}
	
	public class ProdosFileInfo {
		private int access;
		private int fileType;
		private int auxType;
		
		public ProdosFileInfo(int access, int fileType, int auxType) {
			this.access = access;
			this.fileType = fileType;
			this.auxType = auxType;
		}
		
		public int getAccess() {
			return access;
		}
		public int getFileType() {
			return fileType;
		}
		public int getAuxType() {
			return auxType;
		}
	}
}
