/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-3 by Robert Greene
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
package com.webcodepro.applecommander.ui;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Provide a generalized and common mechanism to handle user preferences throughout
 * all AppleCommander user interfaces.
 * <p>
 * Date created: Nov 18, 2002 10:08:34 PM
 * @author Rob Greene
 */
public class UserPreferences {
	private static final String FILENAME = "AppleCommander.preferences";
	private static final String IMAGE_DIRECTORY = "imageDirectory";
	private static final String EXPORT_DIRECTORY = "exportDirectory";
	private static final String COMPILE_DIRECTORY = "compileDirectory";
	private static final String SAVE_DIRECTORY = "saveDirectory";
	private static final String IMPORT_DIRECTORY = "importDirectory";
	private static UserPreferences instance;
	private Properties properties = new Properties();
	/**
	 * Hide constructor from other classes.
	 */
	private UserPreferences() {
	}
	/**
	 * Get the singleton UserPreferences.
	 */
	public static UserPreferences getInstance() {
		if (instance == null) {
			instance = new UserPreferences();
			instance.load();
		}
		return instance;
	}
	/**
	 * Initialize the user preferences from disk.
	 */
	private void load() {
		try {
			FileInputStream inputStream = new FileInputStream(FILENAME);
			properties.load(inputStream);
			inputStream.close();
		} catch (Exception ignored) {
		}
	}
	/**
	 * Save the user preferences to disk.
	 */
	public void save() {
		try {
			FileOutputStream outputStream = new FileOutputStream(FILENAME);
			properties.store(outputStream, "AppleCommander user preferences");
			outputStream.close();
		} catch (Exception ignored) {
		}
	}
	/**
	 * Get the disk image directory (used for "open" command).
	 */
	public String getDiskImageDirectory() {
		return properties.getProperty(IMAGE_DIRECTORY);
	}
	/**
	 * Get the export directory.
	 */
	public String getExportDirectory() {
		return properties.getProperty(EXPORT_DIRECTORY);
	}
	/**
	 * Get the compile directory.
	 */
	public String getCompileDirectory() {
		return properties.getProperty(COMPILE_DIRECTORY);
	}
	/**
	 * Get the save directory.
	 */
	public String getSaveDirectory() {
		return properties.getProperty(SAVE_DIRECTORY);
	}
	/**
	 * Get the import directory.
	 */
	public String getImportDirectory() {
		return properties.getProperty(IMPORT_DIRECTORY);
	}
	/**
	 * Set the disk image directory.
	 */
	public void setDiskImageDirectory(String diskImageDirectory) {
		properties.setProperty(IMAGE_DIRECTORY, diskImageDirectory);
	}
	/**
	 * Set the export directory.
	 */
	public void setExportDirectory(String exportDirectory) {
		properties.setProperty(EXPORT_DIRECTORY, exportDirectory);
	}
	/**
	 * Set the compile directory.
	 */
	public void setCompileDirectory(String compileDirectory) {
		properties.setProperty(COMPILE_DIRECTORY, compileDirectory);
	}
	/**
	 * Set the save directory.
	 */
	public void setSaveDirectory(String saveDirectory) {
		properties.setProperty(SAVE_DIRECTORY, saveDirectory);
	}
	/**
	 * Set the import directory.
	 */
	public void setImportDirectory(String importDirectory) {
		properties.setProperty(IMPORT_DIRECTORY, importDirectory);
	}
}
