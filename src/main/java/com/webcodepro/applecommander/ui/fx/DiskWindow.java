package com.webcodepro.applecommander.ui.fx;

import java.io.File;

import javafx.stage.Stage;

public class DiskWindow {

	// This won't take a File ultimately, probably a Disk.  And obviously
	// it will, y'know, do something.  :)  All TODO!
	public DiskWindow(File file) {
		Stage diskStage = new Stage();
		System.out.println("Yeah, we're opening \""+file+"\"");

		diskStage.show();
	}
}
