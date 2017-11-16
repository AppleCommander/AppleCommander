/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002 by Robert Greene
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
package com.webcodepro.applecommander.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.physical.ImageOrder;

/**
 * This class contains helper methods for dealing with Apple2 data.
 * <p>
 * Date created: Oct 5, 2002 4:16:16 PM
 * @author Rob Greene
 */
public class AppleUtil {
	private static TextBundle textBundle = TextBundle.getInstance();
	/**
	 * This is the number of bytes to display per line.
	 */
	private static final int BYTES_PER_LINE = 16;

	/**
	 * This is the ASCII space character as used by the Apple ][.
	 * The high bit is off.
	 */
	private static final int APPLE_SPACE = 0x20;

	/**
	 * Bit masks used for the bit shifting or testing operations.
	 */
	private static byte[] masks = { 
			(byte)0x01, (byte)0x02, (byte)0x04, (byte)0x08, 
			(byte)0x10, (byte)0x20, (byte)0x40, (byte)0x80 };
	/**
	 * Valid hex digits used when encoding or decoding hex.
	 */
	private static char[] hexDigits = { 
			'0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	/**
	 * Compute the value of a word.
	 * Pulls value from buffer given the offset.
	 * A word is two bytes, in standard Apple LO/HI format.
	 */
	public static int getWordValue(byte[] buffer, int offset) {
		if (offset+1 > buffer.length) {
			return 0;
		}
		return getWordValue(buffer[offset], buffer[offset+1]);
	}
	/**
	 * Compute the value of a word.
	 */
	public static int getWordValue(byte low, byte high) {
		return getUnsignedByte(low) + getUnsignedByte(high)*256;
	}
	
	/**
	 * Compute the signed value of a word.
	 */
	public static int getSignedWordValue(byte[] buffer, int offset) {
		int value = buffer[offset+1] * 256;
		return value + getUnsignedByte(buffer[offset]);
	}
	
	/**
	 * Set a word value.
	 */
	public static void setWordValue(byte[] buffer, int offset, int value) {
		buffer[offset] = (byte)(value % 256);
		buffer[offset+1] = (byte)(value / 256);
	}

	/**
	 * Compute the value of a 3 byte value. This may be ProDOS specific.
	 * Pulls value from buffer given the offset.
	 * Stored in standard Apple LO/HI format.
	 */
	public static int get3ByteValue(byte[] buffer, int offset) {
		if (offset+2 > buffer.length) {
			return 0;
		}
		return getUnsignedByte(buffer[offset]) 
			+ getUnsignedByte(buffer[offset+1])*256
			+ getUnsignedByte(buffer[offset+2])*65536;
	}
	
	/**
	 * Compute the value of a 4 byte value. This is specific to DC42 processing.
	 * Pulls value from buffer given the offset, MSB first. 
	 */
	public static long getLongValue(byte[] buffer, int offset) {
		if (offset+3 > buffer.length) {
			return 0;
		}
		return getUnsignedByte(buffer[offset+3]) 
			+ getUnsignedByte(buffer[offset+2])*256
			+ getUnsignedByte(buffer[offset+1])*65536
			+ getUnsignedByte(buffer[offset])*16777216;
	}
	
	/**
	 * Set the value of a 3 byte value.
	 */
	public static void set3ByteValue(byte[] buffer, int offset, int value) {
		buffer[offset] =   (byte) (value & 0x0000ff);
		buffer[offset+1] = (byte)((value & 0x00ff00) >> 8);
		buffer[offset+2] = (byte)((value & 0xff0000) >> 16);
	}

	/**
	 * Extract out an unsigned byte as an int.
	 * All Java bytes are signed; need to convert to an int
	 * and remove the sign.
	 */
	public static int getUnsignedByte(byte value) {
		return value & 0xff;
	}
	
	/**
	 * Extract out an unsigned byte as an int.
	 * All Java bytes are signed; need to convert to an int
	 * and remove the sign.
	 */
	public static int getUnsignedByte(byte[] buffer, int offset) {
		if (offset+1 > buffer.length) return 0;
		else return getUnsignedByte(buffer[offset]);
	}

	/**
	 * Count the number of bits set in a byte.
	 */
	public static int getBitCount(byte byt) {
		int count = 0;
		for (int ix=0; ix<8; ix++) {
			if (isBitSet(byt, ix)) count++;
		}
		return count;
	}
	
	/**
	 * Determine if a specific bit is set.
	 */
	public static boolean isBitSet(byte byt, int bit) {
		return (byt & masks[bit]) != 0;
	}
	
	/**
	 * Set a specific bit (turn it on).
	 */
	public static byte setBit(byte byt, int bit) {
		return (byte) ((byt | masks[bit]) & 0xff);
	}

	/**
	 * Clear a specific bit (turn it off).
	 */
	public static byte clearBit(byte byt, int bit) {
		return (byte) ((byt & ~masks[bit]) & 0xff);
	}

	/**
	 * Extract a string from the buffer.
	 */
	public static String getString(byte[] buffer, int offset, int length) {
		byte[] value = new byte[length];
		for (int i=0; i<length; i++) {
			byte ch = buffer[offset+i];
			ch &= 0x7f;
			value[i] = ch;
		}
		return new String(value);
	}
	
	/**
	 * Create an Apple string that is space delimited.
	 */
	public static void setString(byte[] buffer, int offset, String string, int length) {
		setString(buffer, offset, string, length, true);
	}

	/**
	 * Create an Apple string that is space delimited.
	 */
	public static void setString(byte[] buffer, int offset, String string, int length, boolean highBitOn) {
		for (int i=0; i<length; i++) {
			char ch = ' ';
			if (i < string.length()) {
				ch = string.charAt(i);
			}
			buffer[offset+i] = (byte) (ch | (highBitOn ? 0x80 : 0x00));
		}
	}

	/**
	 * Create an Apple string that is the same length as the given string.
	 */
	public static void setString(byte[] buffer, int offset, String string) {
		setString(buffer, offset, string, string.length());
	}

	/**
	 * Extract a Pascal string from the buffer.
	 */
	public static String getPascalString(byte[] buffer, int offset) {
		int length = getUnsignedByte(buffer[offset]);
		return getString(buffer, offset+1, length);
	}
	
	/**
	 * Set a Pascal string into the buffer.
	 */
	public static void setPascalString(byte[] buffer, int offset, String string, int maxLength) {
		int len = Math.min(string.length(), maxLength);
		buffer[offset] = (byte) (len & 0xff);
		setString(buffer, offset+1, string, len, false);
	}
	
	/**
	  * Extract a Pascal date from the buffer.<br>
	  * Bits 0-3: month (1-12)<br>
	  * Bits 4-8: day (1-31)<br>
	  * Bits 9-15: year (0-99)
	  */
	public static Date getPascalDate(byte[] buffer, int offset) {
		int pascalDate = getWordValue(buffer, offset);
		int month =  pascalDate & 0x000f - 1;
		int day =   (pascalDate & 0x01f0) >> 4;
		int year =  (pascalDate & 0xfe00) >> 9;
		if (year < 50) year+= 2000;
		if (year < 100) year+= 1900;
		GregorianCalendar gc = new GregorianCalendar(year, month, day);
		return gc.getTime();
	}
	
	/**
	  * Set a Pascal data to the buffer.<br>
	  * Bits 0-3: month (1-12)<br>
	  * Bits 4-8: day (1-31)<br>
	  * Bits 9-15: year (0-99)
	  */
	public static void setPascalDate(byte[] buffer, int offset, Date date) {
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(date);
		int month = gc.get(Calendar.MONTH) + 1;
		int day = gc.get(Calendar.DAY_OF_MONTH);
		int year = gc.get(Calendar.YEAR) % 100;
		int pascalDate = (month & 0x000f)
			| ((day << 4) & 0x01f0)
			| ((year << 9) & 0xfe00);
		setWordValue(buffer, offset, pascalDate);
	}

	/**
	 * Extract a ProDOS string from the buffer.
	 */
	public static String getProdosString(byte[] buffer, int offset) {
		int length = getUnsignedByte(buffer[offset]) & 0x0f;
		return getString(buffer, offset+1, length);
	}
	
	/**
	 * Sets a ProDOS string into the buffer.
	 */
	public static void setProdosString(byte[] buffer, int offset, String string, int maxLength) {
		int len = Math.min(string.length(), maxLength);
		buffer[offset] = (byte) ((buffer[offset] & 0xf0) | (len & 0x0f));
		setString(buffer, offset+1, string, len, false);
	}
	
	/**
	 * Format a byte value as hexidecimal.
	 */
	public static String getFormattedByte(int byt) {
		int byt1 = byt & 0x0f;
		int byt2 = (byt & 0xf0) >> 4;
		StringBuffer buf = new StringBuffer(2);
		buf.append(hexDigits[byt2]);
		buf.append(hexDigits[byt1]);
		return buf.toString();
	}
	
	/**
	 * Format a word value as hexidecimal.
	 */
	public static String getFormattedWord(int word) {
		return getFormattedByte((word & 0xff00) >> 8)
			+ getFormattedByte(word & 0x00ff);
	}
	
	/**
	 * Format a 3 byte value as hexidecimal.
	 */
	public static String getFormatted3ByteAddress(int addr) {
		return getFormattedByte((addr & 0xff0000) >> 16)
			+ getFormattedWord(addr & 0x00ffff);
	}
	
	/**
	 * Convert a typical Apple formatted word.  This is essentially
	 * a hex string that may start with a '$' and has 1 - 4 digits.
	 */
	public static int convertFormattedWord(String word) {
		if (word == null) return 0;
		int value = 0;
		word = word.toUpperCase();
		for (int i=0; i<word.length(); i++) {
			char ch = word.charAt(i);
			for (int nybble = 0; nybble < hexDigits.length; nybble++) {
				if (ch == hexDigits[nybble]) {
					value <<= 4;
					value += nybble;
					break;
				}
			}
		}
		return value;
	}
	
	/**
	 * Extract a ProDOS date from the buffer.
	 */
	public static Date getProdosDate(byte[] buffer, int offset) {
		int ymd = getWordValue(buffer, offset);
		if (ymd == 0) return null;
		int hm = getWordValue(buffer, offset+2);
		
		int day = ymd & 0x001f;				// bits 0-4
		int month = ((ymd & 0x01e0) >> 5) - 1;	// bits 5-8
		int year = (ymd & 0xfe00) >> 9;		// bits 9-15
		int minute = hm & 0x003f;				// bits 0-5
		int hour = (hm & 0x1f00) >> 8;			// bits 8-12

		if (year < 50) year+= 2000;
		if (year < 100) year+= 1900;

		GregorianCalendar gc = new GregorianCalendar(year, month, day, hour, minute);
		return gc.getTime();
	}
	
	/**
	 * Set a ProDOS date into the buffer.
	 */
	public static void setProdosDate(byte[] buffer, int offset, Date date) {
		int day = 0;
		int month = 0;
		int year = 0;
		int minute = 0;
		int hour = 0;
		if (date != null) {
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTime(date);
			day = gc.get(Calendar.DAY_OF_MONTH);
			month = gc.get(Calendar.MONTH) + 1;
			year = gc.get(Calendar.YEAR);
			minute = gc.get(Calendar.MINUTE);
			hour = gc.get(Calendar.HOUR_OF_DAY);
			if (year >= 2000) {
				year -= 2000;
			} else {
				year -= 1900;
			}
		}
		int ymd = ((year & 0x7f) << 9) | ((month & 0xf) << 5) | (day & 0x1f);
		int hm = ((hour & 0x1f) << 8) | (minute & 0x3f);
		setWordValue(buffer, offset, ymd);
		setWordValue(buffer, offset+2, hm);
	}
	
	/**
	 * Make a "nice" filename.  Some of the Apple ][ file names
	 * have characters that are unpalatable - such as "/" or
	 * "\" or ":" which are directory separators along with other
	 * characters that are not allowed by various operating systems.
	 * This method just sanitizes the filename.
	 */
	public static String getNiceFilename(String filename) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<filename.length(); i++) {
			char ch = filename.charAt(i);
			if (ch == '\\' || ch == '/' || ch == '?' || ch == '*'
				|| ch == ':' || ch == '"' || ch == '<' || ch == '>'
				|| ch == '|') {
				// bad characters - skip them
			} else {
				buf.append(ch);
			}
		}
		return buf.toString();
	}
	
	/**
	 * Mimic the Apple IIGS UnPackBytes method call.  The compression is
	 * very similar the RLE, but has the following coding conventions:<br>
	 *     00xx xxxx = 1 to 64 bytes follow (all different)<br>
	 *     01xx xxxx = 3, 5, 6, or 7 repeats of next byte<br>
	 *     10xx xxxx = 1 to 64 repeats of next 4 bytes<br>
	 *     11xx xxxx = 1 to 64 repeats of next byte taken as 4 bytes<br>
	 * The 6 data bits are stored as length-1; hence 000000 is a length of
	 * 1 and 111111 is a length of 64.
	 */
	public static byte[] unpackBytes(byte[] compressedData) {
		ByteArrayOutputStream decompressedStream = 
			new ByteArrayOutputStream(compressedData.length * 2);
		int offset = 0;
		byte data;
		byte[] dataArray = new byte[4];
		while (offset < compressedData.length) {
			byte header = compressedData[offset++];
			int length = (header & 0x3f) + 1; 	// 0x3f = 00111111
			switch (header & 0xc0) {			// 0xc0 = 11000000
				case 0x00:	// 00xx xxxx (copy)
					for (int i=0; i<length; i++) {
						decompressedStream.write(compressedData[offset++]);
					}
					break;
				case 0x40:	// 01xx xxxx (repeat byte)
					data = compressedData[offset++];
					for (int i=0; i<length; i++) {
						decompressedStream.write(data);
					}
					break;
				case 0x80:	// 10xx xxxx (repeat next 4 bytes)
					for (int i=0; i<4; i++) {
						dataArray[i] = compressedData[offset++];
					}
					for (int i=0; i<length; i++) {
						try {
							decompressedStream.write(dataArray);
						} catch (IOException ignored) {
							// Ignored
						}
					}
					break;
				case 0xc0:	// 11xx xxxx (repeat byte 4 times length)
					data = compressedData[offset++];
					for (int i=0; i<4; i++) {
						dataArray[i] = data;
					}
					for (int i=0; i<length; i++) {
						try {
							decompressedStream.write(dataArray);
						} catch (IOException ignored) {
							// Ignored
						}
					}
					break;
			}
		}
		return decompressedStream.toByteArray();
	}
	
	/**
	 * Pull a SANE formatted number from the buffer and return it
	 * as a Java double datatype.  Fortunately, SANE is the IEEE 754
	 * format which _is_ Java's double datatype.  The Double class
	 * has an intrinsic longBitsToDouble method to do this.  The
	 * SANE/IEEE 754 format is setup as such:
	 * <pre>
	 *   E SSSSSSSSSSS FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF...F<br>
	 *   0 1........11 12....................................63<br>
	 * </pre>
	 * Where E is the sign bit, S is the exponent bits and F is the
	 * fraction bits.  The format is discussed within the Double class
	 * documentation as around the web.  Be aware that the fraction
	 * bits are base 2.  Meaning that a fraction of .101 is, in reality,
	 * a binary fraction.  In decimal, this is 1/2 + 0/4 + 1/8 = 5/8
	 * or .625.
	 * See http://www.psc.edu/general/software/packages/ieee/ieee.html
	 * for an example.
	 * <p>
	 * Note: SANE numbers, as stored by AppleWorks are in typical
	 * low/high format.
	 */
	public static double getSaneNumber(byte[] buffer, int offset) {
		long doubleBits = 0;
		for (int i=8; i>0; i--) {
			doubleBits <<= 8;
			doubleBits+= getUnsignedByte(buffer[offset+i-1]);
		}
		return Double.longBitsToDouble(doubleBits);
	}
	
	/**
	 * Convert a double number to an Applesoft float.  This is a 5 byte
	 * number.  See "Applesoft: Internals" for more details and review
	 * the Merlin-generated Applesoft source code.
	 * <p>
	 * Since the number is
	 * 5 bytes long, a float will not work - hence the double.  Some
	 * precision is lost, but (hopefully) nothing significant!
	 * <p>
	 * More specificially, the mapping is as follows:<br>
	 * (Applesoft)<br>
	 * <tt>EEEEEEEE SFFFFFFF FFFFFFFF FFFFFFFF FFFFFFFF</tt><br>
	 * (IEEE 754 - Java)<br>
	 * <tt>SEEEEEEE EEEEFFFF FFFFFFFF FFFFFFFF FFFFFFFF FFFFFFFF FFFFFFFF FFFFFFFF</tt><br>
	 * The mapping will blank the following Double bits:<br>
	 * <tt>S000EEEE EEEEFFFF FFFFFFFF FFFFFFFF FFFFFFFF FFFF0000 00000000 00000000</tt><br>
	 */
	public static byte[] getApplesoftFloat(double number) {
		// get bit representation:
		long value = Double.doubleToRawLongBits(Math.abs(number));
		// make Applesoft number:
		long exponentMask =	0x0ff0000000000000L;
		long exponentAdj =		0x0820000000000000L;
		long signAdj =			0x0000000000000000L;
		if (number < 0) {
			signAdj =			0x0080000000000000L;
		}
		long fractionMask =	0x000ffffffff00000L;
		long result = ((value & exponentMask) + exponentAdj) << 4
			| signAdj
			| (value & fractionMask) << 3;
		// convert to bytes and return:
		long byte1Mask = 	0xff00000000000000L;
		long byte2Mask =	0x00ff000000000000L;
		long byte3Mask =	0x0000ff0000000000L;
		long byte4Mask =	0x000000ff00000000L;
		long byte5Mask =	0x00000000ff000000L;
		return new byte[] {
			(byte) ((result & byte1Mask) >> 56 & 0xff),
			(byte) ((result & byte2Mask) >> 48 & 0xff),
			(byte) ((result & byte3Mask) >> 40 & 0xff),
			(byte) ((result & byte4Mask) >> 32 & 0xff),
			(byte) ((result & byte5Mask) >> 24 & 0xff)
		};
	}
	
	/**
	 * Generate a simple hex dump from the given byte array.
	 * <p>
	 * This is in the general form of:<br>
	 * MMMMMM: HH HH HH HH HH HH HH HH  HH HH HH HH HH HH HH HH  AAAAAAAA AAAAAAAA<br>
	 * Where MMMMMM = memory address, HH = hex byte, and
	 * A = ASCII character.
	 */
	public static String getHexDump(byte[] bytes) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		PrintWriter printer = new PrintWriter(output);
		printer.println(textBundle.get("AppleUtil.HexDumpLine1")); //$NON-NLS-1$
		printer.println(textBundle.get("AppleUtil.HexDumpLine2")); //$NON-NLS-1$
		for (int offset=0; offset<bytes.length; offset+= BYTES_PER_LINE) {
			printer.print("$"); //$NON-NLS-1$
			printer.print(AppleUtil.getFormatted3ByteAddress(offset));
			printer.print("  "); //$NON-NLS-1$
			for (int b=0; b<BYTES_PER_LINE; b++) {
				if (b == BYTES_PER_LINE / 2) printer.print(' ');
				int index = offset+b;
				printer.print( (index < bytes.length) ?
					AppleUtil.getFormattedByte(bytes[index]) : ".."); //$NON-NLS-1$
				printer.print(' ');
			}
			printer.print(' ');
			for (int a=0; a<BYTES_PER_LINE; a++) {
				if (a == BYTES_PER_LINE / 2) printer.print(' ');
				int index = offset+a;
				if (index < bytes.length) {
					char ch = (char) (bytes[index] & 0x7f);
					if ((byte)ch >= (byte)APPLE_SPACE) {
						printer.print(ch);
					} else {
						printer.print('.');
					}
				} else {
					printer.print(' ');
				}
			}
			printer.println();
		}
		printer.println(textBundle.get("AppleUtil.HexDumpEndMessage")); //$NON-NLS-1$
		printer.flush();
		printer.close();
		return output.toString();
	}
	
	/**
	 * Change ImageOrder from source order to target order by copying sector by sector.
	 */
	public static void changeImageOrderByTrackAndSector(ImageOrder sourceOrder, ImageOrder targetOrder) {
		if (!sameSectorsPerDisk(sourceOrder, targetOrder)) {
			throw new IllegalArgumentException(textBundle.
					get("AppleUtil.CannotChangeImageOrder")); //$NON-NLS-1$
		}
		for (int track = 0; track < sourceOrder.getTracksPerDisk(); track++) {
			for (int sector = 0; sector < sourceOrder.getSectorsPerTrack(); sector++) {
				byte[] data = sourceOrder.readSector(track, sector);
				targetOrder.writeSector(track, sector, data);
			}
		}
	}
	
	/**
	 * Answers true if the two disks have the same sectors per disk.
	 */
	protected static boolean sameSectorsPerDisk(ImageOrder sourceOrder, ImageOrder targetOrder) {
		return sourceOrder.getSectorsPerDisk() == targetOrder.getSectorsPerDisk();
	}
	
	/**
	 * Compare two disks by track and sector.
	 */
	public static boolean disksEqualByTrackAndSector(FormattedDisk sourceDisk, FormattedDisk targetDisk) {
		ImageOrder sourceOrder = sourceDisk.getImageOrder();
		ImageOrder targetOrder = targetDisk.getImageOrder();
		if (!sameSectorsPerDisk(sourceOrder, targetOrder)) {
			throw new IllegalArgumentException(textBundle.
					get("AppleUtil.CannotCompareDisks")); //$NON-NLS-1$
		}
		for (int track = 0; track < sourceOrder.getTracksPerDisk(); track++) {
			for (int sector = 0; sector < sourceOrder.getSectorsPerTrack(); sector++) {
				byte[] sourceData = sourceOrder.readSector(track, sector);
				byte[] targetData = targetOrder.readSector(track, sector);
				if (!Arrays.equals(sourceData, targetData)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Change ImageOrder from source order to target order by copying block by block.
	 */
	public static void changeImageOrderByBlock(ImageOrder sourceOrder, ImageOrder targetOrder) {
		if (!sameBlocksPerDisk(sourceOrder, targetOrder)) {
			throw new IllegalArgumentException(textBundle.
					get("AppleUtil.CannotChangeImageOrder")); //$NON-NLS-1$
		}
		for (int block = 0; block < sourceOrder.getBlocksOnDevice(); block++) {
			byte[] blockData = sourceOrder.readBlock(block);
			targetOrder.writeBlock(block, blockData);
		}
	}

	/**
	 * Answers true if the two disks have the same number of blocks per disk.
	 */
	protected static boolean sameBlocksPerDisk(ImageOrder sourceOrder, ImageOrder targetOrder) {
		return sourceOrder.getBlocksOnDevice() == targetOrder.getBlocksOnDevice();
	}

	/**
	 * Compare two disks block by block.
	 */
	public static boolean disksEqualByBlock(FormattedDisk sourceDisk, FormattedDisk targetDisk) {
		ImageOrder sourceOrder = sourceDisk.getImageOrder();
		ImageOrder targetOrder = targetDisk.getImageOrder();
		if (!sameBlocksPerDisk(sourceOrder, targetOrder)) {
			throw new IllegalArgumentException(textBundle.
					get("AppleUtil.CannotCompareDisks")); //$NON-NLS-1$
		}
		for (int block = 0; block < sourceOrder.getBlocksOnDevice(); block++) {
				byte[] sourceData = sourceOrder.readBlock(block);
				byte[] targetData = targetOrder.readBlock(block);
				if (!Arrays.equals(sourceData, targetData)) {
					return false;
				}
		}
		return true;
	}
}
