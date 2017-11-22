/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2007 by Robert Greene and others
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

package com.webcodepro.applecommander.ui.fx;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.util.TextBundle;

public class DiskWindow {
	private TextBundle textBundle = UiBundle.getInstance();
	private FormattedDisk[] disks;

	// This won't take a File ultimately, probably a Disk.  And obviously
	// it will, y'know, do something.  :)  All TODO!
	public DiskWindow(FormattedDisk[] disks) {
		this.disks = disks;
		Stage diskStage = new Stage();
		VBox diskWindow = new VBox();
		Scene diskScene = new Scene(diskWindow);

		diskStage.setTitle(disks[0].getFilename());
		diskStage.getIcons().add(new Image("/com/webcodepro/applecommander/ui/images/diskicon.gif")); //$NON-NLS-1$

		TabPane tabPane = new TabPane();
		tabPane.setSide(Side.BOTTOM);
		tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

		tabPane.getTabs().add(createFilesTab());

		diskWindow.getChildren().add(tabPane);
		diskStage.setScene(diskScene);
		diskStage.show();
	}

	public Tab createFilesTab() {
		Tab filesTab = new Tab(textBundle.get("FilesTab")); //$NON-NLS-1$
		VBox vbox = new VBox();
		ToolBar toolBar = new ToolBar();

		toolBar.getItems().add(new Button("Placeholder"));

		SplitPane splitPane = new SplitPane();
		TreeItem dummyRoot = new TreeItem("dummy");
		TreeView diskTreeView = new TreeView(dummyRoot);
		diskTreeView.setShowRoot(false); // Hide the dummy root node
		for (int i=0; i < disks.length; ++i) {
			TreeItem diskTree = new TreeItem(disks[i]);

			if (disks[i].canHaveDirectories()) {
				addDirectoriesToTree(diskTree, (DirectoryEntry)disks[i]);
			}
			dummyRoot.getChildren().add(diskTree);
		}

		// If there's only one disk, expand its top-level directory
		if (dummyRoot.getChildren().size() == 1) {
			TreeItem singleDir = (TreeItem) dummyRoot.getChildren().get(0);
			singleDir.setExpanded(true);
		}

		TableView fileTable = new TableView();
		splitPane.getItems().addAll(diskTreeView, fileTable);
		vbox.getChildren().addAll(toolBar, splitPane);
		filesTab.setContent(vbox);
		return filesTab;
	}

	protected void addDirectoriesToTree(TreeItem parent, DirectoryEntry dirEntry) {
		Iterator files = dirEntry.getFiles().iterator();
		while (files.hasNext()) {
			final FileEntry fileEntry = (FileEntry) files.next();
			if (fileEntry.isDirectory()) {
				TreeItem item = new TreeItem(fileEntry);
				parent.getChildren().add(item);
				addDirectoriesToTree(item, (DirectoryEntry)fileEntry);
			}
		}
	}
}
