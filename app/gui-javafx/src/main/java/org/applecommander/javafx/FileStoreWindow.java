/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2026 by Robert Greene and others
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
package org.applecommander.javafx;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import org.applecommander.filestore.FileStore;
import org.applecommander.filestore.FileStores;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;
import org.applecommander.usage.DiskUsage;
import org.applecommander.util.FileExtensions;
import org.applecommander.util.FileExtensions.FileExtension;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.applecommander.javafx.FxUtils.*;

public class FileStoreWindow {
    private final BorderPane window;

    // Primary toolbar
    private final Button openFileButton;
    private final Button createFileButton;
    private final Button saveFileButton;
    private final Button saveFileAsButton;
    private final ToggleButton filesViewButton;
    private final ToggleButton diskUsageViewButton;
    private final ToggleButton informationViewButton;

    // Content components
    private final StackPane contentPane;
    private final VBox landingPage;
    private final Button switchDiskButton;
    private final Label statusLabel;
    private final FileView fileView;
    private final DiskUsageView diskUsageView;
    private final InformationView informationView;

    private final Stage primaryStage;
    private final FileStoreSelectionModel fileStoreSelection = new FileStoreSelectionModel();
    private final ObjectProperty<ViewMode> viewMode = new SimpleObjectProperty<>(ViewMode.LANDING);
    private final SimpleBooleanProperty supportsDiskUsage = new SimpleBooleanProperty(false);

    public FileStoreSelectionModel fileStoreSelection() {
        return fileStoreSelection;
    }
    public ObjectProperty<ViewMode> viewModeProperty() {
        return viewMode;
    }
    public SimpleBooleanProperty  supportsDiskUsageProperty() {
        return supportsDiskUsage;
    }

    public static void openNewWindow(Source source) {
        Objects.requireNonNull(source);
        Stage stage = new Stage();
        try {
            FileStoreWindow controller = createWindow(stage);
            controller.openImage(source, false);
            stage.toFront();
            stage.requestFocus();
        } catch (Exception ex) {
            throw new RuntimeException("Could not open new disk window", ex);
        }
    }

    public static FileStoreWindow createWindow(Stage stage) throws Exception {
        FileStoreWindow controller = new FileStoreWindow(stage);
        Scene scene = new Scene(controller.window, 1200, 700);
        stage.setTitle(AppleCommanderFX.buildTitle());
        stage.setScene(scene);

        // Bind keyboard shortcuts in controller
        try {
            controller.bindScene(scene);
            controller.fileView.bindScene(scene);
        } catch (Exception ignored) {
        }

        stage.show();
        return controller;
    }

    public FileStoreWindow(Stage stage) {
        this.primaryStage = stage;

        openFileButton = createButton("open-file.png", "Open", _ -> openFile());
        createFileButton = createButton("new-file.png", "Create", _ -> createFile());
        saveFileButton = createButton("save-file.png", "Save", _ -> saveFile());
        saveFileAsButton = createButton("save-as-file.png", "Save As...", _ -> saveFileAs());

        ToggleGroup viewModeGroup = new ToggleGroup();
        filesViewButton = createToggleButton("image-file-view.png", "Files", viewModeGroup, _ -> selectFilesContent());
        diskUsageViewButton = createToggleButton("image-usage-view.png", "Usage", viewModeGroup, _ -> selectDiskUsageContent());
        informationViewButton = createToggleButton("image-information-view.png", "Information", viewModeGroup, _ -> selectInformationView());
        HBox viewModeBox = new HBox(filesViewButton, diskUsageViewButton, informationViewButton);

        ToolBar toolBar = new ToolBar(
            openFileButton, createFileButton, saveFileButton, saveFileAsButton,
            new Separator(Orientation.VERTICAL),
            viewModeBox,
            new Separator(Orientation.VERTICAL)
        );

        // TEMPORARILY DISABLE UNTIL THESE ARE IMPLEMENTED
        Set.of(createFileButton, saveFileButton, saveFileAsButton).forEach(b -> b.setDisable(true));

        ImageView logo = new ImageView(imageUrl("AppleCommanderLogo.png"));
        Label label = new Label("No disk image open. Use open to browse for a disk image.");
        landingPage = new VBox(logo, label);
        landingPage.setAlignment(Pos.CENTER);
        fileView = new FileView(this, toolBar);
        diskUsageView = new DiskUsageView(this);
        informationView = new InformationView(this);
        contentPane = new StackPane(landingPage, fileView, diskUsageView, informationView);

        ImageView imageView = new ImageView(imageUrl("switch-disks.png"));
        imageView.setFitHeight(16);
        imageView.setFitWidth(16);
        imageView.setPreserveRatio(true);
        switchDiskButton = new Button();
        switchDiskButton.setGraphic(imageView);
        statusLabel = new Label("No disk image opened.");
        HBox footer = new HBox(switchDiskButton, statusLabel);

        window = new BorderPane();
        window.setTop(toolBar);
        window.setCenter(contentPane);
        window.setBottom(footer);

        filesViewButton.disableProperty().bind(fileStoreSelection.selectedItemProperty().isNull());
        diskUsageViewButton.disableProperty().bind(fileStoreSelection.selectedItemProperty().isNull().or(supportsDiskUsage.not()));
        informationViewButton.disableProperty().bind(fileStoreSelection.selectedItemProperty().isNull());

        // Cannot bind buttons, so we have a listener!
        viewMode.addListener((_, _, newValue) -> {
            filesViewButton.setSelected(newValue == ViewMode.FILES);
            diskUsageViewButton.setSelected(newValue == ViewMode.USAGE);
            informationViewButton.setSelected(newValue == ViewMode.INFORMATION);
        });

        landingPage.visibleProperty().bind(viewMode.isEqualTo(ViewMode.LANDING));
        landingPage.managedProperty().bind(landingPage.visibleProperty());

        fileStoreSelection.selectedItemProperty().addListener((_, _, newValue) -> {
            if (newValue != null) {
                viewMode.setValue(ViewMode.FILES);
                supportsDiskUsage.setValue(newValue.get(DiskUsage.class).isPresent());
                // There doesn't appear to be a item list changed, so this should work?
                boolean enabled = fileStoreSelection.getItemCount() > 1;
                switchDiskButton.setDisable(!enabled);
                switchDiskButton.setVisible(enabled);
                switchDiskButton.setManaged(enabled);
            }
        });
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void bindScene(Scene scene) {
        // Open: Shortcut + o (lowercase)
        applyShortcutToButton(scene, openFileButton, "Open Disk",
                new KeyCharacterCombination("o", KeyCombination.SHORTCUT_DOWN), this::openFile);

        // Shortcut+1 etc for information panes
        applyShortcutToButton(scene, filesViewButton, "View File Listing",
                new KeyCharacterCombination("1", KeyCombination.SHORTCUT_DOWN), this::selectFilesContent);
        applyShortcutToButton(scene, diskUsageViewButton, "Disk Usage",
                new KeyCharacterCombination("2", KeyCombination.SHORTCUT_DOWN), this::selectDiskUsageContent);
        applyShortcutToButton(scene, informationViewButton, "Information",
                new KeyCharacterCombination("3", KeyCombination.SHORTCUT_DOWN), this::selectInformationView);

        // Shortcut+Esc to switch disks
        applyShortcutToButton(scene, switchDiskButton, "Switch Disks",
                new KeyCharacterCombination("x", KeyCombination.SHORTCUT_DOWN), this::switchDisk);
    }

    private void openFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Apple II disk image");
        AppleCommanderFX.getLastOpenedDirectory().ifPresent(fileChooser::setInitialDirectory);
        for (FileExtension extension : FileExtensions.FILE_FILTERS) {
            fileChooser.getExtensionFilters().add(new ExtensionFilter(extension.description(), extension.extensions()));
        }

        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile == null) {
            return;
        }
        AppleCommanderFX.setLastOpenedDirectory(selectedFile.getParentFile());
        Optional<Source> source = Sources.create(selectedFile);
        openImage(source.orElseThrow(), true);
    }

    public void openImage(Source source, boolean promptForWindow) {
        Objects.requireNonNull(source);
        if (promptForWindow && !fileStoreSelection.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.initOwner(primaryStage);
            alert.setTitle("Open image");
            alert.setHeaderText("An image is already open.");
            alert.setContentText("Would you like to open this image in the current window or in a new window?");
            ButtonType newWindow = new ButtonType("New Window", ButtonBar.ButtonData.YES);
            ButtonType thisWindow = new ButtonType("This Window", ButtonBar.ButtonData.NO);
            ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(thisWindow, newWindow, cancel);

            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.setAlwaysOnTop(true);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() == cancel) {
                return;
            }
            if (result.get() == newWindow) {
                openNewWindow(source);
                return;
            }
        }

        try {
            var inspected = FileStores.inspect(source);
            fileStoreSelection.changeFileStores(inspected.fileStores);
            if (fileStoreSelection.isEmpty()) {
                closeDisk();
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Unable to open image");
                alert.setHeaderText("Image format not recognized.");
                alert.setContentText("The image format was not recognized. No error occurred.");
                Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
                stage.setAlwaysOnTop(true);
                alert.showAndWait();
                return;
            }
            displayDisk();
            viewMode.setValue(ViewMode.FILES);
            primaryStage.setTitle(AppleCommanderFX.buildTitle(source.getName()));
        } catch (Throwable t) {
            showErrorDialog("Could not open disk image", t);
        }
    }

    public void createFile() {
        // TODO
    }

    public void saveFile() {
        // TODO
    }

    public void saveFileAs() {
        // TODO
    }

    private void switchDisk() {
        fileStoreSelection.selectNext();
        displayDisk();
        Optional<Source> source = fileStoreSelection.getSelectedItem().get(Source.class);
        primaryStage.setTitle(AppleCommanderFX.buildTitle(source.map(Source::getName).orElse("Unknown")));
    }

    private void closeDisk() {
        fileStoreSelection.clearFileStores();
        viewMode.setValue(ViewMode.LANDING);
        fileView.clear();
        statusLabel.setText("No disk image opened.");
        if (primaryStage != null) {
            primaryStage.setTitle(AppleCommanderFX.buildTitle());
        }
    }

    private void exitApplication() {
        Platform.exit();
    }

    private void selectFilesContent() {
        viewMode.setValue(ViewMode.FILES);
    }

    private void selectInformationView() {
        viewMode.setValue(ViewMode.INFORMATION);
    }

    private void selectDiskUsageContent() {
        viewMode.setValue(ViewMode.USAGE);
    }

    private void displayDisk() {
        FileStore fileStore = fileStoreSelection.getSelectedItem();

        // Enable disk-usage only if the disk reports support
        boolean supported = fileStore.get(DiskUsage.class).isPresent();
        if (!supported && viewMode.isEqualTo(ViewMode.USAGE).get()) {
            // fall back to files view
            viewMode.setValue(ViewMode.FILES);
        }

        refreshDiskView();
    }

    private void refreshDiskView() {
        if (fileStoreSelection.isEmpty()) {
            return;
        }

        try {
            if (viewMode.isEqualTo(ViewMode.USAGE).get()) {
                // Render the disk usage map
                //renderDiskUsage(diskUsageCanvas.getCanvas());
                statusLabel.setText(buildDiskStatusText());
                return;
            }
            // Files view
            //populateDiskRows();
            statusLabel.setText(buildDiskStatusText());
        } catch (Throwable t) {
            showErrorDialog("Could not read files from disk image", t);
            fileView.clear();
            statusLabel.setText("No disk image opened.");
        }
    }

    private String buildDiskStatusText() {
        if (fileStoreSelection.isEmpty()) {
            return "No disk image opened.";
        }
        FileStore currentDisk = fileStoreSelection.getSelectedItem();
        if (fileStoreSelection.getItemCount() > 1) {
            return String.format("Current disk (%d of %d): %s", fileStoreSelection.getSelectedIndex()+1,
                    fileStoreSelection.getItemCount(), currentDisk.getLabel());
        }
        return "Current disk: " + currentDisk.getLabel();
    }

    private void showErrorDialog(String message, Throwable t) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Disk Browser Error");
        alert.setHeaderText(message);
        alert.setContentText(t.getMessage() == null ? "An unexpected error occurred." : t.getMessage());
        alert.showAndWait();
    }

}
