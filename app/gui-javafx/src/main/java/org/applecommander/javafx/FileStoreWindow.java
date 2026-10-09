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

import com.webcodepro.applecommander.ui.AppleCommander;
import javafx.beans.property.*;
import javafx.geometry.Insets;
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
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import org.applecommander.applesingle.AppleSingle;
import org.applecommander.bastools.api.BasTools;
import org.applecommander.capability.Capability;
import org.applecommander.filestore.FileStore;
import org.applecommander.filestore.FileStores;
import org.applecommander.javafx.settings.Settings;
import org.applecommander.javafx.settings.SettingsDialog;
import org.applecommander.javafx.wizard.CreateFileStoreWizard;
import org.applecommander.shrinkit.NuFileArchive;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;
import org.applecommander.usage.DiskUsage;
import org.applecommander.util.FileExtensions;
import org.applecommander.util.FileExtensions.FileExtension;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Optional;

import static org.applecommander.javafx.FxUtils.*;

public class FileStoreWindow {
    private final BorderPane window;

    // Primary toolbar
    private final Button openFileButton;
    private final Button createFileButton;
    private final Button saveFileButton;
    private final Button saveFileAsButton;
    private final Button settingsButton;
    private final Button aboutButton;
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
    private final ObjectProperty<ViewMode> viewMode = new SimpleObjectProperty<>();
    private final BooleanProperty supportsDiskUsage = new SimpleBooleanProperty();
    private final IntegerProperty changeCount = new SimpleIntegerProperty();
    private final BooleanProperty canSave = new SimpleBooleanProperty();

    public FileStoreSelectionModel fileStoreSelection() {
        return fileStoreSelection;
    }
    public ObjectProperty<ViewMode> viewModeProperty() {
        return viewMode;
    }
    public IntegerProperty changeCountProperty() {
        return changeCount;
    }
    public void addChange() {
        changeCount.setValue(changeCount.getValue() + 1);
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
            controller.diskUsageView.bindScene(scene);
        } catch (Exception ignored) {
        }

        stage.show();
        return controller;
    }

    public FileStoreWindow(Stage stage) {
        this.primaryStage = stage;
        stage.setOnCloseRequest(event -> {
            if (changeCount.getValue() > 0) {
                if (!showYesNoDialog("Unsaved Data",
                        "This image has changes and has not been saved. Exit anyway?")) {
                    event.consume();
                }
            }
        });

        openFileButton = createButton("open-file.png", "Open", _ -> openFile());
        createFileButton = createButton("new-file.png", "Create", _ -> createFile());
        saveFileButton = createButton("save-file.png", "Save", _ -> saveFile());
        saveFileAsButton = createButton("save-as-file.png", "Save As...", _ -> saveFileAs());
        settingsButton = createButton("settings.png", "Settings", _ -> settings());
        aboutButton = createButton("about.png", "About", _ -> about());

        ToggleGroup viewModeGroup = new ToggleGroup();
        filesViewButton = createToggleButton("image-file-view.png", "Files", viewModeGroup, _ -> selectFilesContent());
        diskUsageViewButton = createToggleButton("image-usage-view.png", "Usage", viewModeGroup, _ -> selectDiskUsageContent());
        informationViewButton = createToggleButton("image-information-view.png", "Information", viewModeGroup, _ -> selectInformationView());
        HBox viewModeBox = new HBox(filesViewButton, diskUsageViewButton, informationViewButton);

        ToolBar toolBar = new ToolBar(
            openFileButton, createFileButton, saveFileButton, saveFileAsButton, settingsButton, aboutButton,
            new Separator(Orientation.VERTICAL),
            viewModeBox,
            new Separator(Orientation.VERTICAL)
        );

        saveFileButton.disableProperty().bind(changeCount.isEqualTo(0).or(canSave.not()));

        ImageView logo = new ImageView(imageUrl("AppleCommanderLogoLarge.png"));
        Label label = new Label("No disk image open. Use open to browse for a disk image.");
        label.setTextFill(Color.BLACK);
        Rectangle border = new Rectangle();
        border.setWidth(logo.getImage().getWidth() + 50);
        border.setHeight(logo.getImage().getHeight() + 50);
        border.setArcHeight(20);
        border.setArcWidth(20);
        border.setFill(Color.BEIGE);
        border.setStroke(Color.BLACK);
        VBox logoBox = new VBox(logo, label);
        logoBox.setAlignment(Pos.CENTER);
        StackPane stackPane = new StackPane(border, logoBox);
        landingPage = new VBox(stackPane);
        landingPage.setAlignment(Pos.CENTER);

        fileView = new FileView(this, toolBar);
        diskUsageView = new DiskUsageView(this, toolBar);
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
        footer.setSpacing(6);
        footer.setPadding(new Insets(6));
        footer.setAlignment(Pos.CENTER_LEFT);

        window = new BorderPane();
        window.setTop(toolBar);
        window.setCenter(contentPane);
        window.setBottom(footer);

        filesViewButton.disableProperty().bind(fileStoreSelection.selectedItemProperty().isNull());
        diskUsageViewButton.disableProperty().bind(fileStoreSelection.selectedItemProperty().isNull().or(supportsDiskUsage.not()));
        informationViewButton.disableProperty().bind(fileStoreSelection.selectedItemProperty().isNull());

        // Cannot bind button selection, so we have a listener!
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

                // Making the save button state to be properties. The only one we should modify elsewhere is hasChanged.
                Source source = newValue.get(Source.class).orElseThrow();
                changeCount.setValue(source.hasChanged() ? 1 : 0);
                canSave.setValue(source.can(Capability.SAVE_SOURCE));

                // There doesn't appear to be a item list changed, so this should work?
                boolean enabled = fileStoreSelection.getItemCount() > 1;
                switchDiskButton.setDisable(!enabled);
                switchDiskButton.setVisible(enabled);
                switchDiskButton.setManaged(enabled);

                displayDisk();
            }
        });

        viewMode.set(ViewMode.LANDING);
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
        Settings.getLastOpenedDirectory().ifPresent(fileChooser::setInitialDirectory);
        for (FileExtension extension : FileExtensions.FILE_FILTERS) {
            fileChooser.getExtensionFilters().add(new ExtensionFilter(extension.description(), extension.extensions()));
        }

        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile == null) {
            return;
        }
        Settings.setLastOpenedDirectory(selectedFile.getParentFile());
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
            viewMode.setValue(ViewMode.FILES);
            primaryStage.setTitle(AppleCommanderFX.buildTitle(source.getName()));
        } catch (Throwable t) {
            showErrorDialog("Could not open disk image", t);
        }
    }

    public void createFile() {
        CreateFileStoreWizard wizard = new CreateFileStoreWizard();
        if (wizard.showAndWait(primaryStage)) {
            Source source = wizard.getSource();
            openImage(source, true);
            changeCount.set(1);
        }
    }

    public void saveFile() {
        try {
            FileStore fileStore = fileStoreSelection.getSelectedItem();
            Source source = fileStore.get(Source.class).orElseThrow();
            source.save();
            changeCount.setValue(0);
        } catch (Throwable t) {
            showErrorDialog("Could not save file", t);
        }
    }

    public void saveFileAs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Apple II disk image");
        Settings.getLastOpenedDirectory().ifPresent(fileChooser::setInitialDirectory);
        for (FileExtension extension : FileExtensions.FILE_FILTERS) {
            fileChooser.getExtensionFilters().add(new ExtensionFilter(extension.description(), extension.extensions()));
        }

        File selectedFile = fileChooser.showSaveDialog(primaryStage);
        if (selectedFile == null) {
            return;
        }
        Settings.setLastOpenedDirectory(selectedFile.getParentFile());

        try {
            FileStore fileStore = fileStoreSelection.getSelectedItem();
            Source source = fileStore.get(Source.class).orElseThrow();
            Files.write(selectedFile.toPath(), source.readAllBytes().asBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Optional<Source> newSource = Sources.create(selectedFile);
            openImage(newSource.orElseThrow(), false);
        } catch (Throwable t) {
            showErrorDialog("Could not save file", t);
        }
    }

    public void settings() {
        SettingsDialog dialog = new SettingsDialog();
        dialog.showAndWait(primaryStage);
    }

    public void about() {
        Dialog<ButtonBar.ButtonData> dialog = new Dialog<>();
        dialog.setTitle("About AppleCommanderFX");

        URL imageURL = getClass().getResource("/images/AppleCommanderLogoSmall.png");
        Objects.requireNonNull(imageURL);
        ImageView imageView = new ImageView(imageURL.toExternalForm());
        VBox imagePane = new VBox(imageView);
        imagePane.setPadding(new Insets(0, 0, 10, 0));
        imagePane.setStyle("-fx-background-color: white;");
        imagePane.setAlignment(Pos.CENTER);

        dialog.getDialogPane().setHeader(imagePane);
        dialog.getDialogPane().setContent(FXControls.vertical()
                .alignment(Pos.CENTER)
                .spacing(5)
                .largeBold("AppleCommanderFX")
                .label("Version %s", AppleCommander.VERSION)
                .node(FXControls.horizontal()
                        .alignment(Pos.CENTER)
                        .spacing(5)
                        .label("Visit:")
                        .link("website", "https://applecommander.github.io/")
                        .link("github", "https://github.com/AppleCommander/AppleCommander")
                        .get())
                .table(2,
                        "AppleSingle:", AppleSingle.VERSION,
                        "ShrinkIt:", NuFileArchive.VERSION,
                        "BASIC Tools:", BasTools.VERSION)
                .get());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK);
        dialog.showAndWait();
    }

    private void switchDisk() {
        if (fileStoreSelection.getSelectedIndex()+1 < fileStoreSelection.getItemCount()) {
            fileStoreSelection.selectNext();
        }
        else {
            fileStoreSelection.selectFirst();
        }
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
