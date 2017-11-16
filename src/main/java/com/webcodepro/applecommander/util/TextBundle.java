/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2004 by Robert Greene
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

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Manage the user-interface specific ResourceBundle.  Initally, this is to actually clean up the
 * text.  Ultimately, it should allow AppleCommander to be translated into other languages.
 * <p>
 * Note that TextBundle serves as a generic resource for the util package.  Other
 * groupings are used and will have their own subclasses for TextBundle.
 * 
 * @author Rob Greene
 */
public class TextBundle {
	private ResourceBundle resourceBundle;
	private static TextBundle instance;
	
	/**
	 * Do not allow other classes to instantiate this class.
	 */
	protected TextBundle() {
		// do nothing
	}
	
	/**
	 * Get the singleton instance of the UiBundle. 
	 */
	public static TextBundle getInstance() {
		if (instance == null) {
			instance = new TextBundle();
			instance.initialize();
		}
		return instance;
	}

	/**
	 * Initialize the UiBundle.
	 */
	protected void initialize() {
		if (resourceBundle == null) {
			resourceBundle = ResourceBundle.getBundle(getClass().getName());
		}
	}

	/**
	 * Retrieve a value for the given resource name.
	 */
	public String get(String name) {
		return resourceBundle.getString(name);
	}

	/**
	 * Format the given resource name with a single String value.
	 */
	public String format(String name, String value) {
		return format(name, new Object[] { value });
	}

	/**
	 * Format the given resource name with multiple values.
	 */
	public String format(String name, Object[] values) {
		String resourceValue = get(name);
		MessageFormat messageFormat = new MessageFormat(resourceValue);
		return messageFormat.format(values);
	}

	/**
	 * Format the given resource name with one integer.
	 */
	public String format(String name, int value1) {
		return format(name, new Object[] { 
				new Integer(value1) });
	}

	/**
	 * Format the given resource name with two integers.
	 */
	public String format(String name, int value1, int value2) {
		return format(name, new Object[] { 
				new Integer(value1), new Integer(value2) });
	}

	/**
	 * Format the given resource name with three integers.
	 */
	public String format(String name, int value1, int value2, int value3) {
		return format(name, new Object[] { 
				new Integer(value1), new Integer(value2),
				new Integer(value3) });
	}

	/**
	 * Format the given resource name with two integers.
	 */
	public String format(String name, String value1, int value2) {
		return format(name, new Object[] { 
				value1, new Integer(value2) });
	}
}
