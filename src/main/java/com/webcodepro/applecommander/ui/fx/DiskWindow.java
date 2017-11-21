package com.webcodepro.applecommander.ui.fx;

import java.io.File;

import javafx.stage.Stage;

import com.webcodepro.applecommander.storage.FormattedDisk;

public class DiskWindow {

	// This won't take a File ultimately, probably a Disk.  And obviously
	// it will, y'know, do something.  :)  All TODO!
	public DiskWindow(FormattedDisk[] disks) {
		Stage diskStage = new Stage();
		diskStage.setTitle(disks[0].getFilename());

		diskStage.show();
	}
}
