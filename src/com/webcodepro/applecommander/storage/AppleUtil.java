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
package com.webcodepro.applecommander.storage;

import java.util.Date;
import java.util.GregorianCalendar;

/**
 * This class contains helper methods for dealing with Apple2 data.
 * <p>
 * Date created: Oct 5, 2002 4:16:16 PM
 * @author: Rob Greene
 */
public class AppleUtil {

	/**
	 * Compute the value of a word.
	 * Pulls value from buffer given the offset.
	 * A word is two bytes, in standard Apple LO/HI format.
	 */
	public static int getWordValue(byte[] buffer, int offset) {
		return getWordValue(buffer[offset], buffer[offset+1]);
	}
	/**
	 * Compute the value of a word.
	 */
	public static int getWordValue(byte low, byte high) {
		return getUnsignedByte(low) + getUnsignedByte(high)*256;
	}		

	/**
	 * Compute the value of a 3 byte value. This may be ProDOS specific.
	 * Pulls value from buffer given the offset.
	 * Stored in standard Apple LO/HI format.
	 */
	public static int get3ByteValue(byte[] buffer, int offset) {
		return getUnsignedByte(buffer[offset]) 
			+ getUnsignedByte(buffer[offset+1])*256
			+ getUnsignedByte(buffer[offset+2])*65536;
	}	

	/**
	 * Extract out an unsigned byte as an int.
	 * All Java bytes are signed; need to convert to an int
	 * and remove the sign.
	 */
	public static int getUnsignedByte(byte value) {
		return (int) value & 0xff;
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
		byte[] masks = { (byte)0x01, (byte)0x02,(byte)0x04, (byte)0x08, 
			(byte)0x10, (byte)0x20, (byte)0x40, (byte)0x80 };
		return (byt & masks[bit]) != 0;
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
	 * Extract a Pascal string from the buffer.
	 */
	public static String getPascalString(byte[] buffer, int offset) {
		int length = getUnsignedByte(buffer[offset]);
		return getString(buffer, offset+1, length);
	}
	
	/**
	 * Extract a Pascal date from the buffer.
	 */
	public static Date getPascalDate(byte[] buffer, int offset) {
		int pascalDate = getWordValue(buffer, offset);
		int month =  pascalDate & 0x000f;
		int day =   (pascalDate & 0x00f0) >> 4;
		int year =  (pascalDate & 0xff00) >> 8;
		if (year < 50) year+= 2000;
		if (year < 100) year+= 1900;
		GregorianCalendar gc = new GregorianCalendar(year, month, day);
		return gc.getTime();
	}

	/**
	 * Extract a ProDOS string from the buffer.
	 */
	public static String getProdosString(byte[] buffer, int offset) {
		int length = getUnsignedByte(buffer[offset]) & 0x0f;
		return getString(buffer, offset+1, length);
	}
	
	/**
	 * Format a byte value as hexidecimal.
	 */
	public static String getFormattedByte(int byt) {
		String[] values = { "0", "1", "2", "3", "4", "5", "6", "7", 
			"8", "9", "A", "B", "C", "D", "E", "F" };
		int byt1 = byt & 0x0f;
		int byt2 = (byt & 0xf0) >> 4;
		return values[byt2] + values[byt1];
	}
	
	/**
	 * Format a word value as hexidecimal.
	 */
	public static String getFormattedWord(int word) {
		return getFormattedByte((word & 0xff00) >> 8)
			+ getFormattedByte(word & 0x00ff);
	}
	
	/**
	 * Extract a ProDOS date from the buffer.
	 */
	public static Date getProdosDate(byte[] buffer, int offset) {
		int ymd = getWordValue(buffer, offset);
		if (ymd == 0) return null;
		int hm = getWordValue(buffer, offset+2);
		
		int day = ymd & 0x001f;			// bits 0-4
		int month = (ymd & 0x01e0) >> 5;	// bits 5-8
		int year = (ymd & 0xfe00) >> 9;	// bits 9-15
		int minute = hm & 0x003f;			// bits 0-5
		int hour = (hm & 0x1f00) >> 8;		// bits 8-12

		if (year < 50) year+= 2000;
		if (year < 100) year+= 1900;

		GregorianCalendar gc = new GregorianCalendar(year, month, day, hour, minute);
		return gc.getTime();
	}
}
