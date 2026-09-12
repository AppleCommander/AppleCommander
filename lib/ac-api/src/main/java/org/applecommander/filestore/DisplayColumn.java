/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2026 by Robert Greene and others
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
package org.applecommander.filestore;

import java.util.Set;
import java.util.function.Function;

/**
 * A DisplayColumn supports formatted output from various FileStores in a 
 * dynamic fashion. The intent is to provide a flexible display interface
 * that does not rely on hard-coded implementations.
 */
public record DisplayColumn(String headerText, Alignment alignment, Function<FileEntry,Object> valueFn, String fmt, Mode... modes) {
	/**
	 * Indicates if this DisplayColumn supports the mode. In this manner, the columns
	 * can be filtered out. Or it can be ignored to display everything.
	 */
	public boolean supports(Mode mode) {
		return Set.of(modes).contains(mode);
	}
	/**
	 * Helper method to convert the column into a formatted string for display.
	 */
	public String formatAsText(FileEntry fileEntry) {
		return String.format(fmt, valueFn.apply(fileEntry));
	}

	/**
	 * Indicates the display mode to help filter columns to users' preference.
	 */
	public enum Mode {
		NATIVE,
		DETAIL
	}
	/**
	 * Indicates how this column should be formatted.
	 */
	public enum Alignment {
		LEFT,
		CENTER,
		RIGHT
	}
}