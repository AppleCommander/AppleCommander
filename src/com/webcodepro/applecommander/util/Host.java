/*
 * Copyright (C) 2006 John B. Matthews
 * matthewsj at users.sourceforge.net
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

import java.io.File;

/**
 * Host contains static methods that return information
 * that varies from one host operating system to another.
 * 
 * @author John B. Matthews
 */

public class Host {

	private static final boolean macOSX;
	private static final boolean linux;
	private static final boolean windows;
	private static final String userHome = System.getProperty("user.home");
	private static final String osName = System.getProperty("os.name");
	private static final String prefDir;

	static {
		macOSX  = (osName.indexOf("Mac OS X") >= 0);
		linux   = (osName.indexOf("Linux") >= 0);
		windows = (osName.indexOf("Windows") >= 0);
		
		if (macOSX) {
			prefDir = userHome + "/Library/Preferences/";
		}
		else if (linux) {
			prefDir = userHome + "/.";
		}
		else if (windows) {
			prefDir = userHome;
		}
		else {
			prefDir = "";
		}
	}
	
	private Host() {}
	
	/**
	 * Get the host specific preferences directory.
	 * On Mac OS X, this is ~/Library/Preferences/.
	 * On other host systems, it is the empty string.
	 * @return The empty string or the Mac OS X preferences directory with trailing '/'.
	 */
	public static String getPrefDir() {
		return prefDir;
	}
	
	/**
	 * Get the host specific form of a file name suitable for a file dialog.
	 * On Mac OS X, this is just the file name portion of the path name.
	 * On other systems, it is the unchanged path name.
	 * @param pathName The full path name of a file. 
	 * @return The full path name or the file name for Mac OS X
	 */
	public static String getFileName(String pathName) {
		if (macOSX) {
			File file = new File(pathName);
			return file.getName();
		}
		return pathName;
	}
}
