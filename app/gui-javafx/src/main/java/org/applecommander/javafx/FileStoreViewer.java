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
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import org.applecommander.capability.Capability;
import org.applecommander.filestore.*;
import org.applecommander.filestore.DisplayColumn.Alignment;
import org.applecommander.filestore.DisplayColumn.Mode;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;
import org.applecommander.usage.BlockUsage;
import org.applecommander.usage.DiskUsage;
import org.applecommander.usage.SectorUsage;
import org.applecommander.util.FileExtensions;
import org.applecommander.util.FileExtensions.FileExtension;

import java.io.File;
import java.util.*;

public class FileStoreViewer {
    @FXML private Button openFileButton;
    @FXML private Button createFileButton;
    @FXML private Button saveFileButton;
    @FXML private Button saveFileAsButton;
    @FXML private VBox landingPage;
    @FXML private TableView<FileEntry> fileTable;
    @FXML private Label statusLabel;
    @FXML private ToggleButton filesContentButton;
    @FXML private ToggleButton diskUsageContentButton;
    @FXML private ToggleButton nativeToolButton;
    @FXML private ToggleButton detailToolButton;
    @FXML private ToggleButton deletedFilesToggleButton;
    @FXML private ImageView deletedFilesIcon;
    @FXML private Button switchDiskButton;
    @FXML private HBox breadcrumbBar;
    @FXML private BorderPane diskUsagePane;
    @FXML private CanvasPane diskUsageCanvas;
    @FXML private HBox legendBox;

    private Stage primaryStage;
    private final List<Directory> directoryPath = new ArrayList<>();

    private final ObjectProperty<ViewMode> viewMode = new SimpleObjectProperty<>(ViewMode.LANDING);
    private final ObjectProperty<Mode> listingMode = new SimpleObjectProperty<>(Mode.NATIVE);
    private final FileStoreSelectionModel selection = new FileStoreSelectionModel();
    private final SimpleBooleanProperty supportsDirectories = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty supportsFileDeletion = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty supportsDiskUsage = new SimpleBooleanProperty(false);

    public static void openNewWindow(Source source) {
        Objects.requireNonNull(source);
        Stage stage = new Stage();
        try {
            FileStoreViewer controller = createWindow(stage);
            controller.openImage(source, false);
            stage.toFront();
            stage.requestFocus();
        } catch (Exception ex) {
            throw new RuntimeException("Could not open new disk window", ex);
        }
    }

    public static FileStoreViewer createWindow(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(AppleCommanderFX.class.getResource("/fxml/FileStoreViewer.fxml"));
        Parent root = loader.load();

        FileStoreViewer controller = loader.getController();
        AppleCommanderFX.enforceFxmlTagsArePopulated(controller);
        controller.setPrimaryStage(stage);

        Scene scene = new Scene(root, 1200, 700);
        stage.setTitle(AppleCommanderFX.buildTitle());
        stage.setScene(scene);

        // Bind keyboard shortcuts in controller
        try {
            controller.bindScene(scene);
        } catch (Exception ignored) {
        }

        stage.show();
        return controller;
    }

    @FXML
    private void initialize() {
        fileTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        fileTable.setItems(FXCollections.emptyObservableList());
        fileTable.setOnMouseClicked(event -> {
            if (event.getClickCount() != 2) {
                return;
            }
            FileEntry selectedRow = fileTable.getSelectionModel().getSelectedItem();
            if (selectedRow == null) {
                return;
            }
            selectedRow.get(Directory.class).ifPresentOrElse(this::navigateToDirectory, () -> {
                // TODO
                //FileViewer.open(entry, primaryStage);
            });
            switch (selectedRow.getContentType()) {
                case DISK_IMAGE, ARCHIVE_IMAGE -> {
                    Optional<Source> opt = Sources.create(selectedRow);
                    Source source = opt.orElseThrow();  // we don't expect this to fail!
                    FileStoreViewer.openNewWindow(source);
                }
                case UNKNOWN -> { /* Do Nothing */ }
            }
        });
        updateSwitchDiskButton();

        // Bind canvas size to the table area so the disk usage can reuse available space
//        diskUsageCanvas.widthProperty().bind(fileTable.widthProperty());
//        diskUsageCanvas.heightProperty().bind(fileTable.heightProperty().subtract(60));
        diskUsageCanvas.setRepaint(this::renderDiskUsage);

        filesContentButton.disableProperty().bind(selection.selectedItemProperty().isNull());
        diskUsageContentButton.disableProperty().bind(selection.selectedItemProperty().isNull().or(supportsDiskUsage.not()));

        nativeToolButton.visibleProperty().bind(viewMode.isEqualTo(ViewMode.FILES));
        nativeToolButton.managedProperty().bind(nativeToolButton.visibleProperty());
        detailToolButton.visibleProperty().bind(viewMode.isEqualTo(ViewMode.FILES));
        detailToolButton.managedProperty().bind(detailToolButton.visibleProperty());
        deletedFilesToggleButton.visibleProperty().bind(fileTable.visibleProperty());
        deletedFilesToggleButton.managedProperty().bind(deletedFilesToggleButton.visibleProperty());
        deletedFilesToggleButton.disableProperty().bind(supportsFileDeletion.not());

        breadcrumbBar.visibleProperty().bind(viewMode.isEqualTo(ViewMode.FILES).and(supportsDirectories));
        breadcrumbBar.managedProperty().bind(breadcrumbBar.visibleProperty());

        landingPage.visibleProperty().bind(viewMode.isEqualTo(ViewMode.LANDING));
        landingPage.managedProperty().bind(landingPage.visibleProperty());
        fileTable.visibleProperty().bind(viewMode.isEqualTo(ViewMode.FILES));
        fileTable.managedProperty().bind(fileTable.visibleProperty());
        diskUsagePane.visibleProperty().bind(viewMode.isEqualTo(ViewMode.USAGE));
        diskUsagePane.managedProperty().bind(diskUsagePane.visibleProperty());

        selection.selectedItemProperty().addListener((_, _, newValue) -> {
            if (newValue != null) {
                directoryPath.clear();
                directoryPath.add(newValue.getRootDirectory());
                viewMode.setValue(ViewMode.FILES);
                listingMode.setValue(Mode.NATIVE);
                supportsDirectories.setValue(newValue.can(Capability.SUPPORTS_DIRECTORIES));
                supportsFileDeletion.setValue(newValue.can(Capability.DELETE_FILES));
                supportsDiskUsage.setValue(newValue.get(DiskUsage.class).isPresent());
            }
        });

        deletedFilesToggleButton.selectedProperty().addListener((_, _, _) -> {
            String imagePath = deletedFilesToggleButton.isSelected()
                    ? "/images/deleted-files-visible.png"
                    : "/images/deleted-files-hidden.png";
            deletedFilesIcon.setImage(new Image(Objects.requireNonNull(getClass().getResource(imagePath)).toExternalForm()));
        });

        listingMode.addListener((_, _, newValue) -> {
            fileTable.getColumns().forEach(column -> {
                nativeToolButton.setSelected(newValue == Mode.NATIVE);
                detailToolButton.setSelected(newValue == Mode.DETAIL);
                if (column.getUserData() instanceof DisplayColumn displayColumn) {
                    column.visibleProperty().setValue(displayColumn.supports(newValue));
                }
            });
        });
        viewMode.addListener((_, _, _) -> populateDiskRows());
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public void bindScene(Scene scene) {
        // Open: Shortcut + o (lowercase)
        applyShortcutToButton(scene, openFileButton, "Open Disk",
                new KeyCharacterCombination("o", KeyCombination.SHORTCUT_DOWN), this::openFile);

        // Function keys for view modes
        applyShortcutToButton(scene, nativeToolButton, "Native View",
                new KeyCodeCombination(KeyCode.F2), this::selectNativeView);
        applyShortcutToButton(scene, detailToolButton, "Detail View",
                new KeyCodeCombination(KeyCode.F3), this::selectDetailView);

        // Shortcut+1 etc for information panes
        applyShortcutToButton(scene, filesContentButton, "View File Listing",
                new KeyCharacterCombination("1", KeyCombination.SHORTCUT_DOWN), this::selectFilesContent);
        applyShortcutToButton(scene, diskUsageContentButton, "Disk Usage",
                new KeyCharacterCombination("2", KeyCombination.SHORTCUT_DOWN), this::selectDiskUsageContent);

        // Shortcut+Esc to switch disks
        applyShortcutToButton(scene, switchDiskButton, "Switch Disks",
                new KeyCharacterCombination("x", KeyCombination.SHORTCUT_DOWN), this::switchDisk);
    }
    private void applyShortcutToButton(Scene scene, ButtonBase button, String tooltipText, KeyCombination keyCombination, Runnable runnable) {
        scene.getAccelerators().put(keyCombination, runnable);
        button.setOnAction(e -> runnable.run());
        button.setTooltip(new Tooltip(String.format("%s (%s)", tooltipText, keyCombination.getDisplayText().toUpperCase(Locale.ROOT))));
    }

    @FXML
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
        if (promptForWindow && !selection.isEmpty()) {
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
            selection.changeFileStores(inspected.fileStores);
            if (selection.isEmpty()) {
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
            listingMode.setValue(Mode.NATIVE);
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

    @FXML
    private void switchDisk() {
        selection.selectNext();
        displayDisk();
        Optional<Source> source = selection.getSelectedItem().get(Source.class);
        primaryStage.setTitle(AppleCommanderFX.buildTitle(source.map(Source::getName).orElse("Unknown")));
    }

    @FXML
    private void closeDisk() {
        selection.clearFileStores();
        directoryPath.clear();
        viewMode.setValue(ViewMode.LANDING);
        listingMode.setValue(Mode.NATIVE);
        deletedFilesToggleButton.setSelected(false);
        fileTable.setItems(FXCollections.emptyObservableList());
        fileTable.getColumns().clear();
        updateSwitchDiskButton();
        statusLabel.setText("No disk image opened.");
        if (primaryStage != null) {
            primaryStage.setTitle(AppleCommanderFX.buildTitle());
        }
    }

    @FXML
    private void exitApplication() {
        Platform.exit();
    }

    @FXML
    private void selectFilesContent() {
        viewMode.setValue(ViewMode.FILES);
    }

    @FXML
    private void selectDiskUsageContent() {
        viewMode.setValue(ViewMode.USAGE);
    }

    @FXML
    private void selectNativeView() {
        listingMode.set(Mode.NATIVE);
    }

    @FXML
    private void selectDetailView() {
        listingMode.set(Mode.DETAIL);
    }

    @FXML
    private void toggleDeletedFiles() {
        if (!selection.isEmpty() && viewMode.isEqualTo(ViewMode.FILES).get()) {
            refreshDiskView();
        }
    }

    private void displayDisk() {
        FileStore fileStore = selection.getSelectedItem();
        directoryPath.clear();
        directoryPath.add(fileStore.getRootDirectory());
        updateSwitchDiskButton();

        // Enable disk-usage only if the disk reports support
        boolean supported = fileStore.get(DiskUsage.class).isPresent();
        if (!supported && viewMode.isEqualTo(ViewMode.USAGE).get()) {
            // fall back to files view
            viewMode.setValue(ViewMode.FILES);
        }

        refreshDiskView();
    }

    private void navigateToDirectory(Directory directory) {
        if (directory == null) {
            return;
        }
        if (directoryPath.contains(directory)) {
            while (directoryPath.size() > 1 && !directoryPath.getLast().equals(directory)) {
                directoryPath.removeLast();
            }
        } else {
            directoryPath.add(directory);
        }
        refreshDiskView();
    }

    private void refreshDiskView() {
        if (selection.isEmpty()) {
            return;
        }

        try {
            updateSwitchDiskButton();
            refreshBreadcrumbs();

            if (viewMode.isEqualTo(ViewMode.USAGE).get()) {
                // Render the disk usage map
                renderDiskUsage(diskUsageCanvas.getCanvas());
                statusLabel.setText(buildDiskStatusText());
                return;
            }
            // Files view
            populateDiskRows();
            statusLabel.setText(buildDiskStatusText());
        } catch (Throwable t) {
            showErrorDialog("Could not read files from disk image", t);
            fileTable.setItems(FXCollections.emptyObservableList());
            fileTable.getColumns().clear();
            statusLabel.setText("No disk image opened.");
        }
    }

    private void renderDiskUsage(Canvas canvas) {
        if (selection.isEmpty()) {
            return;
        }
        FileStore fileStore = selection.getSelectedItem();
        Optional<DiskUsage> opt = fileStore.get(DiskUsage.class);
        if (opt.isEmpty()) {
            // nothing to render
            return;
        }
        DiskUsage usage = opt.get();

        String topTitle = "Tracks";
        String leftTitle = "Sectors";
        if (usage instanceof BlockUsage) {
            topTitle = "Blocks";
            leftTitle = "Blocks";
        }

        int xCount = 1;
        int yCount = 1;

        if (usage instanceof SectorUsage sectorUsage) {
            xCount = sectorUsage.getTotalTracks();
            yCount = sectorUsage.getTotalSectors();
        } else  {
            // Compute a near-square grid to render individual blocks
            int cols = (int) Math.ceil(Math.sqrt(usage.getTotal()));
            int rows = (int) Math.ceil((double) usage.getTotal() / cols);
            xCount = Math.max(1, cols);
            yCount = Math.max(1, rows);
        }

        // Draw onto canvas
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0) w = 800;
        if (h <= 0) h = 480;

        double padding = 8.0;
        double gap = 2.0;

        // Determine label font sizes and measure required label areas
        Font titleFont = Font.font(14);
        Font labelFont = Font.font(12);

        // Measure top labels (column numbers) and left labels (row numbers)
        double maxTopLabelWidth = 0.0;
        double maxTopLabelHeight = 0.0;
        for (int col = 0; col < xCount; col++) {
            String label = Integer.toString(col);
            Text t = new Text(label);
            t.setFont(labelFont);
            Bounds b = t.getLayoutBounds();
            maxTopLabelWidth = Math.max(maxTopLabelWidth, b.getWidth());
            maxTopLabelHeight = Math.max(maxTopLabelHeight, b.getHeight());
        }

        double maxLeftLabelWidth = 0.0;
        double maxLeftLabelHeight = 0.0;
        for (int row = 0; row < yCount; row++) {
            String label = Integer.toString(row);
            Text t = new Text(label);
            t.setFont(labelFont);
            Bounds b = t.getLayoutBounds();
            maxLeftLabelWidth = Math.max(maxLeftLabelWidth, b.getWidth());
            maxLeftLabelHeight = Math.max(maxLeftLabelHeight, b.getHeight());
        }

        // Split top area into title area and number area to prevent overlap
        double topTitleHeight = titleFont.getSize() + 4.0;
        double topNumberHeight = maxTopLabelHeight + 6.0;
        double topLabelHeight = Math.max(24.0, topTitleHeight + topNumberHeight + 4.0);

        // Split left area into title area (for rotated title) and number area
        double leftTitleWidth = titleFont.getSize() + 6.0; // rotated title approx
        double leftNumberWidth = maxLeftLabelWidth + 8.0;
        double leftLabelWidth = Math.max(48.0, leftTitleWidth + leftNumberWidth + 6.0);

        double availableW = Math.max(10, w - padding * 2 - leftLabelWidth);
        double availableH = Math.max(10, h - padding * 2 - topLabelHeight);

        double cellW = xCount > 0 ? (availableW - (xCount - 1) * gap) / xCount : availableW;
        double cellH = yCount > 0 ? (availableH - (yCount - 1) * gap) / yCount : availableH;
        gc.clearRect(0, 0, w, h);

        // Colors
        Color freeColor = Color.web("#90EE90"); // lightgreen
        Color usedColor = Color.web("#F08080"); // lightcoral
        Color borderColor = Color.web("#000000");

        // Use measured titleFont and labelFont from earlier
        gc.setFont(titleFont);

        // Draw top title (centered in title area)
        double titleX = padding + leftLabelWidth + availableW / 2.0;
        double titleY = padding + topTitleHeight / 2.0;
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(topTitle, titleX, titleY);

        // Draw top numeric labels (in their own band below the title)
        gc.setFont(labelFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        double topNumbersY = padding + topTitleHeight + topNumberHeight / 2.0;
        for (int col = 0; col < xCount; col++) {
            // show 0, then every 5th index (0,5,10,...) and always show last index
            if ((col % 5 != 0) && col != xCount - 1) continue;
            String label;
            if (usage instanceof BlockUsage) {
                // single-dimension blocks: top label shows starting block index for column
                int labelVal = col * yCount;
                label = Integer.toString(labelVal);
            } else {
                label = Integer.toString(col);
            }
            double xCenter = padding + leftLabelWidth + col * (cellW + gap) + cellW / 2.0;
            gc.fillText(label, xCenter, topNumbersY);
        }

        // Left vertical title: place in the left title band, centered vertically against grid
        gc.save();
        double leftTitleCenterX = padding + leftTitleWidth / 2.0;
        double leftTitleCenterY = padding + topLabelHeight + availableH / 2.0;
        gc.translate(leftTitleCenterX, leftTitleCenterY);
        gc.rotate(-90);
        gc.fillText(leftTitle, 0, 0);
        gc.restore();

        // Left numeric labels: place in the left number band (to the right of the rotated title)
        double leftNumbersX = padding + leftTitleWidth + leftNumberWidth / 2.0;
        for (int row = 0; row < yCount; row++) {
            // show 0, then every 5th index (0,5,10,...) and always show last index
            if ((row % 5 != 0) && row != yCount - 1) continue;
            String label = Integer.toString(row);
            double yCenter = padding + topLabelHeight + row * (cellH + gap) + cellH / 2.0;
            gc.fillText(label, leftNumbersX, yCenter);
        }

        // Iterate DiskUsage - column-major (columns first)
        for (int col = 0; col < xCount; col++) {
            for (int row = 0; row < yCount; row++) {
                boolean isFree = false;
                boolean isUsed = false;
                if (usage instanceof BlockUsage blockUsage) {
                    int block = col * yCount + row;
                    if (block < blockUsage.getTotal()) {
                        isUsed = blockUsage.isUsed(block);
                        isFree = !isUsed;
                    }
                } else if (usage instanceof SectorUsage sectorUsage) {
                    isUsed = sectorUsage.isUsed(col, row);
                    isFree = !isUsed;
                }

                double x = padding + leftLabelWidth + col * (cellW + gap);
                double y = padding + topLabelHeight + row * (cellH + gap);

                if (isFree) {
                    gc.setFill(freeColor);
                } else if (isUsed) {
                    gc.setFill(usedColor);
                } else {
                    continue;
                }
                gc.fillRect(x, y, Math.max(1, cellW), Math.max(1, cellH));
                gc.setStroke(borderColor);
                gc.strokeRect(x, y, Math.max(1, cellW), Math.max(1, cellH));
            }
        }

        // Legend
        legendBox.getChildren().clear();

        HBox freeLegend = new HBox(6);
        Region freeSwatch = new Region();
        freeSwatch.setStyle("-fx-background-color: #90EE90; -fx-border-color: #000000; -fx-min-width: 16px; -fx-min-height: 16px;");
        Label freeLabel = new Label("Free");
        freeLegend.getChildren().addAll(freeSwatch, freeLabel);

        HBox usedLegend = new HBox(6);
        Region usedSwatch = new Region();
        usedSwatch.setStyle("-fx-background-color: #F08080; -fx-border-color: #000000; -fx-min-width: 16px; -fx-min-height: 16px;");
        Label usedLabel = new Label("Used");
        usedLegend.getChildren().addAll(usedSwatch, usedLabel);

        legendBox.getChildren().addAll(freeLegend, usedLegend);
    }

    private void populateDiskRows() {
        Directory directory = directoryPath.getLast();
        List<DisplayColumn> displayColumns = directory.getFileStore().getDisplayColumns();

        fileTable.getColumns().clear();
        for (final DisplayColumn displayColumn : displayColumns) {
            TableColumn<FileEntry,String> column = new TableColumn<>(displayColumn.headerText());
            column.setUserData(displayColumn);
            column.setCellValueFactory(cell ->
                    new SimpleStringProperty(displayColumn.formatAsText(cell.getValue())));
            column.visibleProperty().setValue(displayColumn.supports(listingMode.get()));
            if (displayColumn.alignment() == Alignment.RIGHT) {
                column.setStyle("-fx-alignment: CENTER-RIGHT;");
            } else if (displayColumn.alignment() == Alignment.CENTER) {
                column.setStyle("-fx-alignment: CENTER;");
            }
            fileTable.getColumns().add(column);
        }

        List<? extends FileEntry> rows = directory.getFiles().stream()
                .filter(fileEntry -> deletedFilesToggleButton.isSelected() || !fileEntry.isDeleted())
                .toList();

        ObservableList<FileEntry> rowList = FXCollections.observableArrayList(rows);
        SortedList<FileEntry> sortedRows = new SortedList<>(rowList);
        sortedRows.comparatorProperty().bind(fileTable.comparatorProperty());
        fileTable.setItems(sortedRows);
        fileTable.getSortOrder().clear();
        // If selection is bound, the ToolButton crashes and burns, so need to manage it manually.
        nativeToolButton.setSelected(listingMode.get() == Mode.NATIVE);
        detailToolButton.setSelected(listingMode.get() == Mode.DETAIL);
    }

    private void refreshBreadcrumbs() {
        breadcrumbBar.getChildren().clear();
        FileStore fileStore = selection.getSelectedItem();
        if (!fileStore.can(Capability.SUPPORTS_DIRECTORIES)) {
            return;
        }

        Label pathLabel = new Label("Path:");
        breadcrumbBar.getChildren().add(pathLabel);
        if (directoryPath.isEmpty()) {
            return;
        }

        for (int i = 0; i < directoryPath.size(); i++) {
            Directory dir = directoryPath.get(i);
            Button crumb = new Button(dir.getName().replace("/", ""));
            final int index = i;
            crumb.setOnAction(event -> {
                List<Directory> newPath = new ArrayList<>(directoryPath.subList(0, index + 1));
                directoryPath.clear();
                directoryPath.addAll(newPath);
                refreshDiskView();
            });
            Label separator = new Label("/");
            breadcrumbBar.getChildren().add(separator);
            breadcrumbBar.getChildren().add(crumb);
        }
    }

    private void updateSwitchDiskButton() {
        boolean enabled = selection.getItemCount() > 1;
        switchDiskButton.setDisable(!enabled);
        switchDiskButton.setVisible(enabled);
        switchDiskButton.setManaged(enabled);
    }

    private String buildDiskStatusText() {
        if (selection.isEmpty()) {
            return "No disk image opened.";
        }
        FileStore currentDisk = selection.getSelectedItem();
        if (selection.getItemCount() > 1) {
            return String.format("Current disk (%d of %d): %s", selection.getSelectedIndex()+1,
                    selection.getItemCount(), currentDisk.getLabel());
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

    private enum ViewMode {
        LANDING,
        FILES,
        USAGE
    }

    private static class FileStoreSelectionModel extends SingleSelectionModel<FileStore> {
        private final List<FileStore> fileStores = new ArrayList<>();

        public void changeFileStores(List<FileStore> fileStores) {
            this.fileStores.clear();
            this.fileStores.addAll(fileStores);
            if (!this.fileStores.isEmpty()) {
                selectFirst();
            }
        }

        public void clearFileStores() {
            this.clearSelection();
            this.fileStores.clear();
        }

        @Override
        protected FileStore getModelItem(int index) {
            if (index >= 0 && index < fileStores.size()) {
                return fileStores.get(index);
            }
            return null;
        }

        @Override
        protected int getItemCount() {
            return fileStores.size();
        }
    }
}
