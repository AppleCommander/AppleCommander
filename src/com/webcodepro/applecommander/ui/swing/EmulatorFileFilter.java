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
