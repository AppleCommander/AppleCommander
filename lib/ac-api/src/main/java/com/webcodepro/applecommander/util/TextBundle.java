/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2022 by Robert Greene
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
 * Manage the user-interface specific ResourceBundle.  Initially, this is to actually clean up the
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
	    if (resourceBundle.containsKey(name)) {
	        return resourceBundle.getString(name);
	    } 
	    else {
	        return String.format("KeyNotFound: %s", name);
	    }
	}

	/**
	 * Format the given resource name with multiple values.
	 */
	public String format(String name, Object... values) {
		String resourceValue = get(name);
		MessageFormat messageFormat = new MessageFormat(resourceValue);
		return messageFormat.format(values);
	}
}
