/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2008 by Robert Greene
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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.Disk.FilenameFilter;
import com.webcodepro.applecommander.ui.AppleCommander;
import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.UserPreferences;
import com.webcodepro.applecommander.util.TextBundle;



public class FXAppleCommander extends Application {

	/**
	 *
	 */
	private UserPreferences userPreferences = UserPreferences.getInstance();
	private TextBundle textBundle = UiBundle.getInstance();
	private StackPane contentPane;
	private MenuItem openItem, closeItem, newItem, quitItem;
	private Button openButton, createButton, compareButton, aboutButton;
	private Stage stage;

	/**
	 * Launch FXAppleCommander (legacy entrypoint).
	 */
	public static void main(String[] args) {
		launch(args);
	}

	/**
	 * Launch FXAppleCommander (JavaFX entrypoint).
	 */
	@Override
	public void start(Stage initialStage) {
		stage = initialStage;
		VBox root = new VBox();
		Scene scene = new Scene(root);

		MenuBar menuBar = createMenuBar();
		ToolBar toolBar = createToolBar();
		contentPane = createContentPane();

		stage.setTitle(textBundle.get("SwtAppleCommander.AppleCommander"));
		stage.getIcons().add(new Image("/com/webcodepro/applecommander/ui/images/diskicon.gif")); //$NON-NLS-1$
		root.getChildren().addAll(menuBar, toolBar, contentPane);

		stage.setScene(scene);
		stage.show();
	}

	/**
	 * FXAppleCommander quit handler.
	 */
	@Override
	public void stop() {
		UserPreferences.getInstance().save();
	}

	public void handleAction(ActionEvent e) {
		Object source = e.getSource();
		if (source == aboutButton) {
			showAboutAppleCommander();
		} else if (source == openItem || source == openButton) {
			openFile();
		} else if (source == quitItem) {
			Platform.exit();
		} else {
			System.out.println("Unhandled action: " + source);
		}
	}

	/**
	 * Create the toolbar and its buttons.
	 */
	ToolBar createToolBar() {
		ToolBar toolBar = new ToolBar();

		openButton = new Button(textBundle.get("OpenButton"), new ImageView(new Image("/com/webcodepro/applecommander/ui/images/opendisk.gif"))); //$NON-NLS-1$
		openButton.setTooltip(new Tooltip(textBundle.get("SwtAppleCommander.OpenDiskImageTooltip"))); //$NON-NLS-1$
		openButton.setContentDisplay(ContentDisplay.TOP);
		openButton.setOnAction(e -> handleAction(e));

		createButton = new Button(textBundle.get("CreateButton"), new ImageView(new Image("/com/webcodepro/applecommander/ui/images/newdisk.gif"))); //$NON-NLS-1$
		createButton.setTooltip(new Tooltip(textBundle.get("SwtAppleCommander.CreateDiskImageTooltip"))); //$NON-NLS-1$
		createButton.setContentDisplay(ContentDisplay.TOP);
		createButton.setOnAction(e -> handleAction(e));

		compareButton = new Button(textBundle.get("CompareButton"), new ImageView(new Image("/com/webcodepro/applecommander/ui/images/comparedisks.gif"))); //$NON-NLS-1$
		compareButton.setTooltip(new Tooltip(textBundle.get("SwtAppleCommander.CompareDiskImageTooltip"))); //$NON-NLS-1$
		compareButton.setContentDisplay(ContentDisplay.TOP);
		compareButton.setOnAction(e -> handleAction(e));

		aboutButton = new Button(textBundle.get("AboutButton"), new ImageView(new Image("/com/webcodepro/applecommander/ui/images/about.gif"))); //$NON-NLS-1$
		aboutButton.setTooltip(new Tooltip(textBundle.get("SwtAppleCommander.AboutTooltip"))); //$NON-NLS-1$
		aboutButton.setContentDisplay(ContentDisplay.TOP);
		aboutButton.setOnAction(e -> handleAction(e));
		toolBar.getItems().addAll(openButton, createButton, compareButton, aboutButton);

		return toolBar;
	}

	/**
	 * Set up the menu bar
	 */
	MenuBar createMenuBar() {
		MenuBar menuBar = new MenuBar();
		// File
		Menu menuFile = new Menu(textBundle.get("SwingAppleCommander.MenuFile")); //$NON-NLS-1$
		// File->Open
		openItem = new MenuItem(textBundle.get("SwingAppleCommander.MenuFileOpen")); //$NON-NLS-1$
		openItem.setOnAction(e -> handleAction(e));
		menuFile.getItems().add(openItem);
		// File->Close
		closeItem = new MenuItem(textBundle.get("SwingAppleCommander.MenuFileClose")); //$NON-NLS-1$
		closeItem.setOnAction(e -> handleAction(e));
		menuFile.getItems().add(closeItem);
		// File->New
		newItem = new MenuItem(textBundle.get("SwingAppleCommander.MenuFileNew")); //$NON-NLS-1$
		newItem.setOnAction(e -> handleAction(e));
		menuFile.getItems().add(newItem);
		// File->Exit
		quitItem = new MenuItem(textBundle.get("SwingAppleCommander.MenuFileQuit")); //$NON-NLS-1$
		quitItem.setOnAction(e -> handleAction(e));
		menuFile.getItems().add(quitItem);
		menuBar.getMenus().add(menuFile);
		return menuBar;
	}

	/**
	 * Add the title tab.
	 */
	StackPane createContentPane() {
		StackPane stackPane = new StackPane();
		ImageView titleImg = new ImageView(new Image("/com/webcodepro/applecommander/ui/images/AppleCommanderLogo.gif")); //$NON-NLS-1$
		stackPane.getChildren().add(titleImg);
		stackPane.setAlignment(Pos.CENTER);
		return stackPane;
	}

	/**
	 * Open a file.
	 */
	protected void openFile() {
		FileChooser fileChooser = new FileChooser();
		String pathName = userPreferences.getDiskImageDirectory();
		if (null == pathName) {
			pathName = ""; //$NON-NLS-1$
		}
		fileChooser.setInitialDirectory(new File(pathName));
		FilenameFilter[] fileFilters = Disk.getFilenameFilters();
		String[] names = new String[fileFilters.length];
		String[] extensions = new String[fileFilters.length];
		for (int i=0; i<fileFilters.length; i++) {
			fileChooser.getExtensionFilters().add(
					new ExtensionFilter(
						fileFilters[i].getNames(),
						fileFilters[i].getExtensionList()));
		}
		File selectedFile = fileChooser.showOpenDialog(stage);
		if (null != selectedFile) {
			userPreferences.setDiskImageDirectory(selectedFile.getParent());
			UserPreferences.getInstance().save();
			DiskWindow window = new DiskWindow(selectedFile);
		}
	}

	/**
	 * Close a file.
	 */
	protected void closeFile() {
	}

	/**
	 * Show About message box.
	 */
	public void showAboutAppleCommander() {
		Alert about = new Alert(Alert.AlertType.INFORMATION);
		about.setTitle(textBundle.get("FXAppleCommander.AboutTitle")); //$NON-NLS-1$
		about.setHeaderText(textBundle.format("FXAppleCommander.AboutHeader", //$NON-NLS-1$
			new Object[] { AppleCommander.VERSION, textBundle.get("Copyright") })); //$NON-NLS-1$
		about.setContentText(textBundle.format("FXAppleCommander.AboutMessage", //$NON-NLS-1$
			new Object[] { AppleCommander.VERSION, textBundle.get("Copyright") })); //$NON-NLS-1$
		about.showAndWait();
	}
}
