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

import com.jthemedetecor.OsThemeDetector;
import com.webcodepro.applecommander.storage.*;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;
import javafx.application.Platform;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class FileStoreViewer {
    private static final int CONTENT_FILES = 0;
    private static final int CONTENT_DISK_USAGE = 1;

    @FXML private Button openFileButton;
    @FXML private Button createFileButton;
    @FXML private Button saveFileButton;
    @FXML private Button saveFileAsButton;
    @FXML private TableView<DiskFileRow> fileTable;
    @FXML private Label statusLabel;
    @FXML private ToggleButton filesContentButton;
    @FXML private ToggleButton diskUsageContentButton;
    @FXML private ToggleButton nativeToolButton;
    @FXML private ToggleButton detailToolButton;
    @FXML private ToggleButton deletedFilesToggleButton;
    @FXML private ImageView deletedFilesIcon;
    @FXML private Button switchDiskButton;
    @FXML private HBox breadcrumbBar;
    @FXML private VBox diskUsagePane;
    @FXML private Canvas diskUsageCanvas;
    @FXML private HBox legendBox;

    private Stage primaryStage;
    private final SelectionHolder<FormattedDisk> selection = new SelectionHolder<>();
    private final List<DirectoryEntry> directoryPath = new ArrayList<>();
    private int currentContentMode = CONTENT_FILES;
    private int currentDisplayMode = FormattedDisk.FILE_DISPLAY_STANDARD;
    private boolean showDeletedFiles = false;

    public static void openNewWindow(File diskFile) {
        Stage stage = new Stage();
        try {
            createWindow(stage, diskFile);
            stage.toFront();
            stage.requestFocus();
        } catch (Exception ex) {
            throw new RuntimeException("Could not open new disk window", ex);
        }
    }

    public static void createWindow(Stage stage, File diskFile) throws Exception {
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

        if (diskFile != null) {
            controller.openDiskFile(diskFile, false);
        }
    }

    @FXML
    private void initialize() {
        fileTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        fileTable.setItems(FXCollections.emptyObservableList());
        fileTable.setOnMouseClicked(event -> {
            if (event.getClickCount() != 2) {
                return;
            }
            DiskFileRow selectedRow = fileTable.getSelectionModel().getSelectedItem();
            if (selectedRow == null || selectedRow.fileEntry() == null) {
                return;
            }
            FileEntry entry = selectedRow.fileEntry();
            if (entry.isDirectory() && entry instanceof DirectoryEntry directoryEntry) {
                navigateToDirectory(directoryEntry);
            } else {
                //FileViewer.open(entry, primaryStage);
            }
        });
        setDeletedFilesButtonState();
        setContentControlsEnabled(false);
        setViewControlsEnabled(false);
        updateSwitchDiskButton();
        applyContentMode(currentContentMode);
        applyViewMode(currentDisplayMode);

        // Bind canvas size to the table area so the disk usage can reuse available space
        diskUsageCanvas.widthProperty().bind(fileTable.widthProperty());
        diskUsageCanvas.heightProperty().bind(fileTable.heightProperty().subtract(60));
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
        // TODO need to expose file filters in new API
        for (FilenameFilter filter : FilenameFilter.getFilenameFilters()) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(filter.getNames(), filter.getExtensionList()));
        }

        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile == null) {
            return;
        }
        AppleCommanderFX.setLastOpenedDirectory(selectedFile.getParentFile());
        openDiskFile(selectedFile, true);
    }

    public void openDiskFile(File selectedFile, boolean promptForWindow) {
        if (selectedFile == null) {
            return;
        }

        if (promptForWindow && !selection.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.initOwner(primaryStage);
            alert.setTitle("Open disk image");
            alert.setHeaderText("A disk image is already open.");
            alert.setContentText("Would you like to open this disk in the current window or in a new window?");
            ButtonType newWindow = new ButtonType("New Window", ButtonBar.ButtonData.YES);
            ButtonType thisWindow = new ButtonType("This Window", ButtonBar.ButtonData.NO);
            ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(thisWindow, newWindow, cancel);

            java.util.Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() == cancel) {
                return;
            }
            if (result.get() == newWindow) {
                openNewWindow(selectedFile);
                return;
            }
        }

        try {
            selection.clear();
            Optional<Source> opt = Sources.create(selectedFile);
            opt.ifPresent(source -> {
                var inspected = Disks.inspect(source);
                selection.setSelectedItems(inspected.disks);
            });
            if (selection.isEmpty()) {
                closeDisk();
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Unable to open disk");
                alert.setHeaderText("Disk image not recognized.");
                alert.setContentText("The disk format was not recognized. No error occurred.");
                Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
                stage.setAlwaysOnTop(true);
                alert.showAndWait();
                return;
            }
            displayDisk();
            if (primaryStage != null) {
                primaryStage.setTitle(AppleCommanderFX.buildTitle(selectedFile.getName()));
            }
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
        selection.nextItem();
        displayDisk();
        primaryStage.setTitle(AppleCommanderFX.buildTitle(selection.getSelectedItem().getFilename()));
    }

    @FXML
    private void closeDisk() {
        selection.clear();
        directoryPath.clear();
        currentContentMode = CONTENT_FILES;
        showDeletedFiles = false;
        fileTable.setItems(FXCollections.emptyObservableList());
        fileTable.getColumns().clear();
        setContentControlsEnabled(false);
        setViewControlsEnabled(false);
        if (filesContentButton != null) {
            filesContentButton.setSelected(true);
        }
        if (diskUsageContentButton != null) {
            diskUsageContentButton.setSelected(false);
        }
        setDeletedFilesButtonState();
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
        applyContentMode(CONTENT_FILES);
    }

    @FXML
    private void selectDiskUsageContent() {
        applyContentMode(CONTENT_DISK_USAGE);
    }

    @FXML
    private void selectStandardView() {
        applyViewMode(FormattedDisk.FILE_DISPLAY_STANDARD);
    }

    @FXML
    private void selectNativeView() {
        applyViewMode(FormattedDisk.FILE_DISPLAY_NATIVE);
    }

    @FXML
    private void selectDetailView() {
        applyViewMode(FormattedDisk.FILE_DISPLAY_DETAIL);
    }

    @FXML
    private void toggleDeletedFiles() {
        showDeletedFiles = deletedFilesToggleButton != null && deletedFilesToggleButton.isSelected();
        setDeletedFilesButtonState();
        if (!selection.isEmpty() && currentContentMode == CONTENT_FILES) {
            refreshDiskView();
        }
    }

    private void applyContentMode(int contentMode) {
        this.currentContentMode = contentMode;

        filesContentButton.setSelected(contentMode == CONTENT_FILES);
        diskUsageContentButton.setSelected(contentMode == CONTENT_DISK_USAGE);

        boolean filesSelected = contentMode == CONTENT_FILES;
        setViewControlsEnabled(!selection.isEmpty() && filesSelected);
        nativeToolButton.setVisible(filesSelected);
        nativeToolButton.setManaged(filesSelected);
        detailToolButton.setVisible(filesSelected);
        detailToolButton.setManaged(filesSelected);
        deletedFilesToggleButton.setVisible(filesSelected);
        deletedFilesToggleButton.setManaged(filesSelected);
        deletedFilesToggleButton.setDisable(!selection.isEmpty() || !filesSelected);
        boolean breadcrumbSupported = selection.getSelectedItem() instanceof ProdosFormatDisk;
        breadcrumbBar.setVisible(filesSelected && breadcrumbSupported);
        breadcrumbBar.setManaged(filesSelected && breadcrumbSupported);

        // toggle which content pane is visible
        fileTable.setVisible(filesSelected);
        fileTable.setManaged(filesSelected);
        diskUsagePane.setVisible(!filesSelected);
        diskUsagePane.setManaged(!filesSelected);

        if (!selection.isEmpty()) {
            refreshDiskView();
        }
    }

    private void applyViewMode(int displayMode) {
        this.currentDisplayMode = displayMode;

        nativeToolButton.setSelected(displayMode == FormattedDisk.FILE_DISPLAY_NATIVE);
        detailToolButton.setSelected(displayMode == FormattedDisk.FILE_DISPLAY_DETAIL);

        if (!selection.isEmpty()) {
            refreshDiskView();
        }
    }

    private void setContentControlsEnabled(boolean enabled) {
        filesContentButton.setDisable(!enabled);
        diskUsageContentButton.setDisable(!enabled);
        deletedFilesToggleButton.setDisable(!enabled || currentContentMode != CONTENT_FILES);
    }

    private void setViewControlsEnabled(boolean enabled) {
        nativeToolButton.setDisable(!enabled);
        detailToolButton.setDisable(!enabled);
    }

    private void displayDisk() {
        FormattedDisk disk = selection.getSelectedItem();
        directoryPath.clear();
        directoryPath.add(disk);
        setContentControlsEnabled(true);
        setDeletedFilesButtonState();
        updateSwitchDiskButton();

        // Enable disk-usage only if the disk reports support
        boolean supported = disk.supportsDiskMap();
        diskUsageContentButton.setDisable(!supported);
        if (!supported && currentContentMode == CONTENT_DISK_USAGE) {
            // fall back to files view
            currentContentMode = CONTENT_FILES;
        }

        setViewControlsEnabled(currentContentMode == CONTENT_FILES);
        refreshDiskView();
    }

    private void navigateToDirectory(DirectoryEntry directory) {
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
            breadcrumbBar.setVisible(false);
            breadcrumbBar.setManaged(false);
            return;
        }

        try {
            FormattedDisk currentDisk = selection.getSelectedItem();
            boolean breadcrumbSupported = currentContentMode == CONTENT_FILES && currentDisk instanceof ProdosFormatDisk;
            breadcrumbBar.setVisible(breadcrumbSupported);
            breadcrumbBar.setManaged(breadcrumbSupported);

            updateSwitchDiskButton();
            refreshBreadcrumbs();

            if (currentContentMode == CONTENT_DISK_USAGE) {
                // Render the disk usage map
                renderDiskUsage(currentDisk);
                statusLabel.setText(buildDiskStatusText());
                return;
            }
            // Files view
            diskUsagePane.setVisible(false);
            diskUsagePane.setManaged(false);
            fileTable.setVisible(true);
            fileTable.setManaged(true);
            populateDiskRows(directoryPath.getLast(), currentDisplayMode);
            statusLabel.setText(buildDiskStatusText());
        } catch (DiskException ex) {
            setContentControlsEnabled(false);
            setViewControlsEnabled(false);
            showErrorDialog("Could not read files from disk image", ex);
            fileTable.setItems(FXCollections.emptyObservableList());
            fileTable.getColumns().clear();
            statusLabel.setText("No disk image opened.");
        }
    }

    private void renderDiskUsage(FormattedDisk disk) throws DiskException {
        if (disk == null) return;

        if (!disk.supportsDiskMap()) {
            // nothing to render
            diskUsagePane.setVisible(false);
            diskUsagePane.setManaged(false);
            return;
        }

        // Show disk usage pane, hide file table
        diskUsagePane.setVisible(true);
        diskUsagePane.setManaged(true);
        fileTable.setVisible(false);
        fileTable.setManaged(false);

        String[] bitmapLabels = disk.getBitmapLabels();
        int[] dims = disk.getBitmapDimensions();

        int xCount = 1;
        int yCount = 1;
        String xLabel = "BLOCKS";
        String yLabel = "BLOCKS";
        int totalBlocks = -1;

        boolean isDim2 = (dims != null && dims.length >= 2);
        boolean isSingleDim = (dims != null && dims.length == 1) || (dims == null && disk.getBitmapLength() > 0);

        // Determine labels from bitmapLabels when available
        if (bitmapLabels != null && bitmapLabels.length > 0) {
            if (isDim2 && bitmapLabels.length >= 2) {
                xLabel = bitmapLabels[0];
                yLabel = bitmapLabels[1];
            } else {
                // single-dimension label applies to both axes
                xLabel = bitmapLabels[0];
                yLabel = bitmapLabels[0];
            }
        } else {
            if (isDim2) {
                xLabel = "TRACKS";
                yLabel = "SECTORS";
            } else {
                xLabel = "BLOCKS";
                yLabel = "BLOCKS";
            }
        }

        if (isDim2) {
            xCount = dims[0];
            yCount = dims[1];
        } else if (isSingleDim) {
            totalBlocks = dims != null ? dims[0] : disk.getBitmapLength();
            // Compute a near-square grid to render individual blocks
            int cols = (int) Math.ceil(Math.sqrt(totalBlocks));
            int rows = (int) Math.ceil((double) totalBlocks / cols);
            xCount = Math.max(1, cols);
            yCount = Math.max(1, rows);
        }

        // Draw onto canvas
        GraphicsContext gc = diskUsageCanvas.getGraphicsContext2D();
        double w = diskUsageCanvas.getWidth();
        double h = diskUsageCanvas.getHeight();
        if (w <= 0) w = 800;
        if (h <= 0) h = 480;

        double padding = 8.0;
        double gap = 2.0;

        // Determine label font sizes and measure required label areas
        Font titleFont = Font.font(12);
        Font labelFont = Font.font(10);

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
        Color textColor = OsThemeDetector.getDetector().isDark() ? Color.web("#E0E0E0") : Color.web("#000000");

        // Draw axis titles and numeric labels
        gc.setFill(textColor);
        // Use measured titleFont and labelFont from earlier
        gc.setFont(titleFont);

        // Draw top title (centered in title area)
        String topTitle = xLabel;
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
            if (dims == null || dims.length == 1) {
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
        String leftTitle = yLabel;
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
        FormattedDisk.DiskUsage usage = disk.getDiskUsage();

        for (int col = 0; col < xCount; col++) {
            for (int row = 0; row < yCount; row++) {
                if (!usage.hasNext()) {
                    // no more data
                    break;
                }
                usage.next();
                boolean isFree = usage.isFree();
                boolean isUsed = usage.isUsed();

                double x = padding + leftLabelWidth + col * (cellW + gap);
                double y = padding + topLabelHeight + row * (cellH + gap);

                if (isFree) {
                    gc.setFill(freeColor);
                } else if (isUsed) {
                    gc.setFill(usedColor);
                } else {
                    gc.setFill(Color.LIGHTGRAY);
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

    private void populateDiskRows(DirectoryEntry directory, int displayMode) throws DiskException {
        fileTable.getColumns().clear();
        List<FormattedDisk.FileColumnHeader> headers = selection.getSelectedItem().getFileColumnHeaders(displayMode);

        for (int i = 0; i < headers.size(); i++) {
            final int columnIndex = i;
            FormattedDisk.FileColumnHeader header = headers.get(i);
            TableColumn<DiskFileRow, String> column = new TableColumn<>(header.getTitle());
            column.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().values().get(columnIndex)));
            column.setMinWidth(60);
            column.setPrefWidth(Math.clamp(header.getMaximumWidth() * 7L, 80, 220));
            column.setMaxWidth(400);
            if (header.isRightAlign()) {
                column.setStyle("-fx-alignment: CENTER-RIGHT;");
            } else if (header.isCenterAlign()) {
                column.setStyle("-fx-alignment: CENTER;");
            }
            fileTable.getColumns().add(column);
        }

        List<DiskFileRow> rows = directory.getFiles().stream()
                .filter(fileEntry -> showDeletedFiles || !fileEntry.isDeleted())
                .map(fileEntry -> new DiskFileRow(fileEntry, fileEntry.getFileColumnData(displayMode)))
                .toList();

        ObservableList<DiskFileRow> rowList = FXCollections.observableArrayList(rows);
        SortedList<DiskFileRow> sortedRows = new SortedList<>(rowList);
        sortedRows.comparatorProperty().bind(fileTable.comparatorProperty());
        fileTable.setItems(sortedRows);
        fileTable.getSortOrder().clear();
    }

    private void refreshBreadcrumbs() {
        breadcrumbBar.getChildren().clear();
        FormattedDisk currentDisk = selection.getSelectedItem();
        if (!(currentDisk instanceof ProdosFormatDisk)) {
            return;
        }

        Label pathLabel = new Label("Path:");
        breadcrumbBar.getChildren().add(pathLabel);
        if (directoryPath == null || directoryPath.isEmpty()) {
            return;
        }

        String volumeName = currentDisk instanceof ProdosFormatDisk prodosDisk
                ? prodosDisk.getDiskName().replace("/", "")
                : "/";

        for (int i = 0; i < directoryPath.size(); i++) {
            DirectoryEntry dir = directoryPath.get(i);
            String crumbText = (dir == currentDisk) ? volumeName : dir.getDirname();
            Button crumb = new Button(crumbText);
            final int index = i;
            crumb.setOnAction(event -> {
                List<DirectoryEntry> newPath = new ArrayList<>(directoryPath.subList(0, index + 1));
                directoryPath.clear();
                directoryPath.addAll(newPath);
                refreshDiskView();
            });
            Label separator = new Label("/");
            breadcrumbBar.getChildren().add(separator);
            breadcrumbBar.getChildren().add(crumb);
        }
    }

    private void setDeletedFilesButtonState() {
        deletedFilesToggleButton.setSelected(showDeletedFiles);
        String imagePath = showDeletedFiles
                ? "/images/deleted-files-visible.png"
                : "/images/deleted-files-hidden.png";
        deletedFilesIcon.setImage(new Image(getClass().getResource(imagePath).toExternalForm()));
    }

    private void updateSwitchDiskButton() {
        selection.nextItem();
        boolean enabled = selection.getSize() > 1;
        switchDiskButton.setDisable(!enabled);
        switchDiskButton.setVisible(enabled);
        switchDiskButton.setManaged(enabled);
    }

    private String buildDiskStatusText() {
        if (selection.isEmpty()) {
            return "No disk image opened.";
        }
        FormattedDisk currentDisk = selection.getSelectedItem();
        String diskName = currentDisk.getDiskName();
        String format = currentDisk.getFormat();
        if (selection.getSize() > 1) {
            return "Current disk (" + (selection.getSelectedIndex() + 1) + " of " + selection.getSize() + "): " + diskName + " (" + format + ")";
        }
        return "Current disk: " + diskName + " (" + format + ")";
    }

    private void showErrorDialog(String message, Throwable t) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Disk Browser Error");
        alert.setHeaderText(message);
        alert.setContentText(t.getMessage() == null ? "An unexpected error occurred." : t.getMessage());
        alert.showAndWait();
    }

    public record DiskFileRow(FileEntry fileEntry, List<String> values) {
    }
}
