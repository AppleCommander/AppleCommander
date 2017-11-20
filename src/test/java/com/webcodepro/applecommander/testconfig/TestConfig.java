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
package com.webcodepro.applecommander.testconfig;

import java.io.InputStream;
import java.util.Properties;

/**
 * A simple class to contains test configuration information.
 * Update the file TestConfig.properties to match the system.
 * 
 * @author Rob
 */
public class TestConfig {
	private static final String FILENAME = "TestConfig.properties"; //$NON-NLS-1$
	private static final String DISK_DIRECTORY = "DiskDir"; //$NON-NLS-1$
	private static final String TEMP_DIRECTORY = "TempDir"; //$NON-NLS-1$
	private static TestConfig instance;
	private Properties properties = new Properties();
	/**
	 * Hide constructor from other classes.
	 */
	private TestConfig() {
		// empty
	}
	/**
	 * Get the singleton TestConfig.
	 */
	public static TestConfig getInstance() {
		if (instance == null) {
			instance = new TestConfig();
			instance.load();
		}
		return instance;
	}
	/**
	 * Initialize the test config from disk.
	 */
	private void load() {
		try {
			InputStream inputStream = getClass().getResourceAsStream(FILENAME);
			properties.load(inputStream);
			inputStream.close();
		} catch (Exception ignored) {
			// Ignored
		}
	}
	/**
	 * Answer with the temp directory.
	 */
	public String getTempDir() {
		return properties.getProperty(TEMP_DIRECTORY);
	}
	/**
	 * Answer with the directory where disk images are stored.
	 */
	public String getDiskDir() {
		return properties.getProperty(DISK_DIRECTORY);
	}
}
