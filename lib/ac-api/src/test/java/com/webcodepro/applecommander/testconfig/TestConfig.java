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
package com.webcodepro.applecommander.testconfig;

/**
 * A simple class to contains test configuration information.
 * Update the file TestConfig.properties to match the system.
 * <p>
 * Note: Used to rely on a properties file; altered it to work
 * more dynamically to allow tests to be run...
 * 
 * @author Rob
 */
public class TestConfig {
	private static TestConfig instance;
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
		}
		return instance;
	}
	/**
	 * Answer with the temp directory.
	 */
	public String getTempDir() {
		return System.getProperty("java.io.tmpdir");
	}
	/**
	 * Answer with the directory where disk images are stored.
	 */
	public String getDiskDir() {
		return "./src/test/resources/disks";
	}
}
