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

import java.util.Comparator;

/**
 * Sort FileEntry objects by the columnIndex.  Mostly useful to the interface.
 * The columns are tested to see if they are numerical - if so, an Integer compare
 * is done (instead of a String).
 * <p>
 * Date created: Oct 27, 2002 8:24:39 PM
 * @author Rob Greene
 */
public class FileEntryComparator implements Comparator {
	private int columnIndex;
	private int displayMode;
	
	/**
	 * Construct a FileEntryComparator for the given columnIndex.
	 */
	public FileEntryComparator(int columnIndex, int displayMode) {
		this.columnIndex = columnIndex;
		this.displayMode = displayMode;
	}

	/**
	 * Compare two FileEntry objects.
	 * @see java.util.Comparator#compare(Object, Object)
	 */
	public int compare(Object o1, Object o2) {
		if (!(o1 instanceof FileEntry) || !(o2 instanceof FileEntry)) {
			return 0;
		}
		
		if (o1 == null || o2 == null) {
			return ((o1 == null) ? -1 : 0) + ((o2 == null) ? 1 : 0);
		}
		
		FileEntry entry1 = (FileEntry) o1;
		FileEntry entry2 = (FileEntry) o2;
		
		String column1 = (String) entry1.getFileColumnData(displayMode).get(columnIndex);
		String column2 = (String) entry2.getFileColumnData(displayMode).get(columnIndex);
		
		if (isAllDigits(column1) && isAllDigits(column2)) {
			int int1 = toInt(column1);
			int int2 = toInt(column2);
			return int1 - int2;
		}
		return column1.compareTo(column2);
	}

	/**
	 * Test for digits in the screen.
	 */
	protected boolean isAllDigits(String string) {
		if (string == null || string.length() == 0) return false;
		for (int i=0; i<string.length(); i++) {
			char ch = string.charAt(i);
			if (!Character.isDigit(ch) && ch != ',') {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Convert String to int.
	 */
	protected int toInt(String string) {
		StringBuffer buf = new StringBuffer(string.length());
		for (int i=0; i<string.length(); i++) {
			char ch = string.charAt(i);
			if (Character.isDigit(ch)) {
				buf.append(ch);
			}
		}
		return Integer.parseInt(buf.toString());
	}
}
