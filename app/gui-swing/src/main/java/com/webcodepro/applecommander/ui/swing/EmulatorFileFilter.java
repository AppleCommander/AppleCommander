/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2008-2022 by Robert Greene
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
package com.webcodepro.applecommander.ui.swing;

import java.io.File;

import javax.swing.filechooser.FileFilter;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.StorageBundle;
import com.webcodepro.applecommander.util.TextBundle;

public class EmulatorFileFilter extends FileFilter {
private TextBundle textBundle = StorageBundle.getInstance();

	public boolean accept(File f) {
		if (f.isDirectory()) {
			return true;
		}
		// if it's *.po, it's ok...
		String[] st = Disk.getAllExtensions();
		for (int i = 0;i < st.length; i++) {
			if (f.getName().endsWith(st[i]))
				return true;
		}
		return false;
	}

	public String getDescription() {
		return textBundle.get("Disk.AllImages");
	}

}
