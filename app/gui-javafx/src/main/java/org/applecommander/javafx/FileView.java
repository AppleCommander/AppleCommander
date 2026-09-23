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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.applecommander.capability.Capability;
import org.applecommander.filestore.Directory;
import org.applecommander.filestore.DisplayColumn;
import org.applecommander.filestore.FileEntry;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.applecommander.javafx.FxUtils.*;

public class FileView extends BorderPane {
    private final FileStoreWindow fileStoreWindow;
    private final HBox breadcrumbBar;
    private final TableView<FileEntry> fileTable;
    private ToggleButton nativeToolButton;
    private ToggleButton detailToolButton;
    private ToggleButton deletedFilesToggleButton;

    private final ObservableList<Directory> directoryPath = FXCollections.observableArrayList();
    private final ObjectProperty<Directory> selectedDirectory = new SimpleObjectProperty<>();
    private final ObjectProperty<DisplayColumn.Mode> listingMode = new SimpleObjectProperty<>(DisplayColumn.Mode.NATIVE);
    private final SimpleBooleanProperty supportsDirectories = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty supportsFileDeletion = new SimpleBooleanProperty(false);

    public FileView(FileStoreWindow fileStoreWindow, ToolBar toolBar) {
        this.fileStoreWindow = fileStoreWindow;

        ToggleGroup listingModeGroup = new ToggleGroup();
        nativeToolButton = createToggleButton("file-native-view.png", "Native", listingModeGroup, _ -> selectNativeView());
        detailToolButton = createToggleButton("file-detail-view.png", "Detail", listingModeGroup, _ -> selectDetailView());
        HBox listingModeBox = new HBox(nativeToolButton, detailToolButton);
        toolBar.getItems().add(listingModeBox);

        deletedFilesToggleButton = createOnOffButton("deleted-files-hidden.png", "deleted-files-visible.png", "Deleted", _ -> toggleDeletedFiles());
        toolBar.getItems().add(deletedFilesToggleButton);

        breadcrumbBar = new HBox();
        breadcrumbBar.setSpacing(6);
        breadcrumbBar.setPadding(new Insets(6));
        breadcrumbBar.setAlignment(Pos.CENTER_LEFT);
        setTop(breadcrumbBar);

        fileTable = new TableView<>();
        VBox placeholder = new VBox(new Label("This image has no files."));
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setSpacing(10);
        fileTable.setPlaceholder(placeholder);
        setCenter(fileTable);

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
            if (selectedRow.get(Directory.class).isPresent()) {
                directoryPath.add(selectedRow.get(Directory.class).get());
                return;
            }
            switch (selectedRow.getContentType()) {
                case DISK_IMAGE, ARCHIVE_IMAGE -> {
                    Optional<Source> opt = Sources.create(selectedRow);
                    Source source = opt.orElseThrow();  // we don't expect this to fail!
                    FileStoreWindow.openNewWindow(source);
                }
                case UNKNOWN -> { /* Do Nothing */ }
            }
        });

        nativeToolButton.visibleProperty().bind(fileStoreWindow.viewModeProperty().isEqualTo(ViewMode.FILES));
        nativeToolButton.managedProperty().bind(nativeToolButton.visibleProperty());
        detailToolButton.visibleProperty().bind(fileStoreWindow.viewModeProperty().isEqualTo(ViewMode.FILES));
        detailToolButton.managedProperty().bind(detailToolButton.visibleProperty());
        deletedFilesToggleButton.visibleProperty().bind(fileStoreWindow.viewModeProperty().isEqualTo(ViewMode.FILES));
        deletedFilesToggleButton.managedProperty().bind(deletedFilesToggleButton.visibleProperty());
        deletedFilesToggleButton.disableProperty().bind(supportsFileDeletion.not());

        breadcrumbBar.visibleProperty().bind(fileStoreWindow.viewModeProperty().isEqualTo(ViewMode.FILES).and(supportsDirectories));
        breadcrumbBar.managedProperty().bind(breadcrumbBar.visibleProperty());

        visibleProperty().bind(fileStoreWindow.viewModeProperty().isEqualTo(ViewMode.FILES));
        managedProperty().bind(fileTable.visibleProperty());

        fileStoreWindow.fileStoreSelection().selectedItemProperty().addListener((_, _, newValue) -> {
            if (newValue != null) {
                directoryPath.clear();
                directoryPath.add(newValue.getRootDirectory());
                listingMode.setValue(DisplayColumn.Mode.NATIVE);
                supportsDirectories.setValue(newValue.can(Capability.SUPPORTS_DIRECTORIES));
                supportsFileDeletion.setValue(newValue.can(Capability.DELETE_FILES));
            }
        });

        fileStoreWindow.viewModeProperty().addListener((_, _, _) -> populateDiskRows());

        listingMode.addListener((_, _, newValue) -> {
            fileTable.getColumns().forEach(column -> {
                nativeToolButton.setSelected(newValue == DisplayColumn.Mode.NATIVE);
                detailToolButton.setSelected(newValue == DisplayColumn.Mode.DETAIL);
                if (column.getUserData() instanceof DisplayColumn displayColumn) {
                    column.visibleProperty().setValue(displayColumn.supports(newValue));
                }
            });
        });

        directoryPath.addListener((ListChangeListener<? super Directory>) change -> {
            breadcrumbBar.getChildren().clear();
            if (fileStoreWindow.fileStoreSelection().getSelectedItem() == null) {
                return;
            }
            String pathSeparator = fileStoreWindow.fileStoreSelection().getSelectedItem().getPathSeparator();

            Label pathLabel = new Label("Path:");
            breadcrumbBar.getChildren().add(pathLabel);

            for (int i = 0; i < directoryPath.size(); i++) {
                Directory dir = directoryPath.get(i);
                // FIXME this is because ProdosFormatDisk uses "/DISK.NAME/"
                Button crumb = new Button(dir.getName().replace("/", ""));
                final int index = i;
                crumb.setOnAction(event -> {
                    List<Directory> newPath = new ArrayList<>(directoryPath.subList(0, index + 1));
                    directoryPath.clear();
                    directoryPath.addAll(newPath);
                    populateDiskRows();
                });
                Label separator = new Label(pathSeparator);
                breadcrumbBar.getChildren().add(separator);
                breadcrumbBar.getChildren().add(crumb);
            }
            selectedDirectory.set(directoryPath.isEmpty() ? null : directoryPath.getLast());
        });

        selectedDirectory.addListener((_, _, newValue) -> {
           populateDiskRows();
        });
    }

    public void bindScene(Scene scene) {
        // Function keys for view modes
        applyShortcutToButton(scene, nativeToolButton, "Native View",
                new KeyCodeCombination(KeyCode.F2), this::selectNativeView);
        applyShortcutToButton(scene, detailToolButton, "Detail View",
                new KeyCodeCombination(KeyCode.F3), this::selectDetailView);
    }

    public void clear() {
        directoryPath.clear();
        selectedDirectory.set(null);
        listingMode.setValue(DisplayColumn.Mode.NATIVE);
        deletedFilesToggleButton.setSelected(false);
        fileTable.setItems(FXCollections.emptyObservableList());
        fileTable.getColumns().clear();
    }


    private void selectNativeView() {
        listingMode.set(DisplayColumn.Mode.NATIVE);
    }

    private void selectDetailView() {
        listingMode.set(DisplayColumn.Mode.DETAIL);
    }

    private void populateDiskRows() {
        fileTable.getColumns().clear();
        if (selectedDirectory.isNull().get()) {
            // No directories, leave a cleared list. Likely in transition.
            return;
        }

        Directory directory = selectedDirectory.get();
        List<DisplayColumn> displayColumns = directory.getFileStore().getDisplayColumns();

        for (final DisplayColumn displayColumn : displayColumns) {
            TableColumn<FileEntry,String> column = new TableColumn<>(displayColumn.headerText());
            column.setUserData(displayColumn);
            column.setCellValueFactory(cell ->
                    new SimpleStringProperty(displayColumn.formatAsText(cell.getValue())));
            column.visibleProperty().setValue(displayColumn.supports(listingMode.get()));
            if (displayColumn.alignment() == DisplayColumn.Alignment.RIGHT) {
                column.setStyle("-fx-alignment: CENTER-RIGHT;");
            } else if (displayColumn.alignment() == DisplayColumn.Alignment.CENTER) {
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
        nativeToolButton.setSelected(listingMode.get() == DisplayColumn.Mode.NATIVE);
        detailToolButton.setSelected(listingMode.get() == DisplayColumn.Mode.DETAIL);
    }

    private void toggleDeletedFiles() {
        if (!fileStoreWindow.fileStoreSelection().isEmpty() &&
                fileStoreWindow.viewModeProperty().isEqualTo(ViewMode.FILES).get()) {
            populateDiskRows();
        }
    }
}
