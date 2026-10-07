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

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.applecommander.applesingle.AppleSingle;
import org.applecommander.applesingle.FileDatesInfo;
import org.applecommander.applesingle.ProdosFileInfo;
import org.applecommander.capability.Capability;
import org.applecommander.filestore.*;
import org.applecommander.javafx.settings.ExportOption;
import org.applecommander.javafx.settings.Settings;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;
import org.applecommander.util.DataBuffer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.applecommander.javafx.FxUtils.*;

public class FileView extends BorderPane {
    // See https://en.wikipedia.org/wiki/AppleSingle_and_AppleDouble_formats
    public static final DataFormat APPLESINGLE_MIME = new DataFormat("application/applefile");

    private final FileStoreWindow fileStoreWindow;
    private final HBox breadcrumbBar;
    private final ToggleButton nativeToolButton;
    private final ToggleButton detailToolButton;
    private final ToggleButton deletedFilesToggleButton;

    // Note that the TableView has a lingering data issue that I couldn't resolve.
    // Therefore, it gets tossed and recreated as needed. The primary issue is that
    // it hangs on to the old items and tries to access values with the new item
    // converters. So if GameDOS was loaded and then a Zip was loaded into the table,
    // a sort caused an exception because something tried to render a GameDOS file
    // entry with the Zip renderer. But only when a sort was applied. Clearing out
    // every property didn't seem to help. Thus this extreme solution. Please fix!
    private TableView<FileEntry> fileTable;

    private final ObservableList<Directory> directoryPath = FXCollections.observableArrayList();
    private final ObjectProperty<Directory> selectedDirectory = new SimpleObjectProperty<>();
    private final ObjectProperty<DisplayColumn.Mode> listingMode = new SimpleObjectProperty<>(DisplayColumn.Mode.NATIVE);
    private final SimpleBooleanProperty supportsDirectories = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty supportsFileDeletion = new SimpleBooleanProperty(false);
    private final ObservableList<FileEntry> fileEntries = FXCollections.observableArrayList();

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

        createNewFileTable();

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
            nativeToolButton.setSelected(newValue == DisplayColumn.Mode.NATIVE);
            detailToolButton.setSelected(newValue == DisplayColumn.Mode.DETAIL);
            fileTable.getColumns().forEach(column -> {
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

        selectedDirectory.addListener((_, _, _) -> {
           populateDiskRows();
        });
        fileStoreWindow.changeCountProperty().addListener((_, _, newValue) -> {
            if (newValue != null && newValue.intValue() > 0) {
                populateDiskRows();
            }
        });
    }

    public void createNewFileTable() {
        if (fileTable != null) {
            fileTable.visibleProperty().unbind();
        }

        fileTable = new TableView<>();
        VBox placeholder = new VBox(new Label("This image has no files."));
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setSpacing(10);
        fileTable.setPlaceholder(placeholder);
        setCenter(fileTable);

        fileTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        fileTable.setItems(FXCollections.emptyObservableList());
        fileTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
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
        // See: https://stackoverflow.com/questions/32534113/javafx-drag-and-drop-a-file-into-a-program
        fileTable.setOnDragDetected(event -> {
            if (!fileTable.getSelectionModel().getSelectedItems().isEmpty()) {
                try {
                    // If we are dragging OUT of AppleCommander, we need to create working files:
                    ExportOption exportOption = Settings.getExportOption();
                    Path tempDir = Files.createTempDirectory("AppleCommander-drag-");
                    tempDir.toFile().deleteOnExit();
                    List<File> files = new ArrayList<>();
                    for (FileEntry fileEntry : fileTable.getSelectionModel().getSelectedItems()) {
                        // We may write two files, so copyToPath gives us ALL the names we care about
                        for (Path path : exportOption.copyToPath(tempDir, fileEntry)) {
                            File file = path.toFile();
                            file.deleteOnExit();
                            files.add(file);
                        }
                    }
                    ClipboardContent content = new ClipboardContent();
                    content.putFiles(files);
                    // Internal drag format is AppleSingle, and one file at a time:
                    AppleSingle appleSingle = ExportOption.createAppleSingle(fileTable.getSelectionModel().getSelectedItem());
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    appleSingle.save(baos);
                    content.put(APPLESINGLE_MIME, ByteBuffer.wrap(baos.toByteArray()));
                    // Now we're ready, so allow it to proceed:
                    Dragboard db = fileTable.startDragAndDrop(TransferMode.COPY_OR_MOVE);
                    db.setContent(content);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            event.consume();
        });
        fileTable.setOnDragOver(event -> {
            FileStore fileStore = fileStoreWindow.fileStoreSelection().getSelectedItem();
            Directory directory = selectedDirectory.get();
            Dragboard db = event.getDragboard();
            boolean internalDrag = db.hasContent(APPLESINGLE_MIME);
            boolean externalDrag = db.hasFiles();
            boolean canCreateFiles = fileStore != null && fileStore.can(Capability.CREATE_FILES) && directory instanceof WritableDirectory;
            if ((internalDrag || externalDrag) && canCreateFiles) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
        fileTable.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            try {
                if (db.hasContent(APPLESINGLE_MIME)) {
                    // Give preference to internal dragging.
                    ByteBuffer data = (ByteBuffer) db.getContent(APPLESINGLE_MIME);
                    AppleSingle appleSingle = AppleSingle.read(data.array());
                    ProdosAttributes prodosAttributes = createProdosAttributes(appleSingle);
                    createFile(prodosAttributes, appleSingle.getDataFork(), appleSingle.getResourceFork());
                    success = true;
                } else if (db.hasFiles()) {
                    // From the file system.
                    System.out.println("DROPPED");
                    System.out.println(db.getFiles());
                    success = true;
                }
            } catch (IOException e) {
                FxUtils.showErrorDialog("Unable to drop files", e);
            }
            event.setDropCompleted(success);
            event.consume();
        });

        managedProperty().bind(fileTable.visibleProperty());
    }

    public ProdosAttributes createProdosAttributes(AppleSingle appleSingle) {
        ProdosAttributes.Builder builder = ProdosAttributes.builder().name("NEWFILE").BIN(0x0000);
        if (appleSingle.getRealName() != null) {
            builder.name(appleSingle.getRealName());
        }
        if (appleSingle.getFileDatesInfo() != null) {
            FileDatesInfo fileDatesInfo = appleSingle.getFileDatesInfo();
            builder.creation(Date.from(fileDatesInfo.getCreationInstant()));
            builder.modification(Date.from(fileDatesInfo.getModificationInstant()));
        }
        if (appleSingle.getProdosFileInfo() != null) {
            ProdosFileInfo prodosFileInfo = appleSingle.getProdosFileInfo();
            builder.auxType(prodosFileInfo.getAuxType());
            builder.fileType(prodosFileInfo.getFileType());
            builder.locked((prodosFileInfo.getAccess() & 0xe3) == 0xe3);
        }
        return builder.build();
    }

    public void createFile(ProdosAttributes prodosAttributes, byte[] dataFork, byte[] resourceFork) {
        Directory directory = selectedDirectory.get();
        if (directory instanceof WritableDirectory writableDirectory) {
            WritableFileEntry fileEntry = writableDirectory.createFrom(prodosAttributes);
            fileEntry.setDataFork(DataBuffer.wrap(dataFork));
            if (resourceFork != null) {
                fileEntry.setResourceFork(DataBuffer.wrap(resourceFork));
            }
            fileStoreWindow.addChange();
        }
        else {
            throw new RuntimeException("This is not a writable directory");
        }
    }

    public void bindScene(Scene scene) {
        // Function keys for view modes
        applyShortcutToButton(scene, nativeToolButton, "Native View",
                new KeyCodeCombination(KeyCode.F5), this::selectNativeView);
        applyShortcutToButton(scene, detailToolButton, "Detail View",
                new KeyCodeCombination(KeyCode.F6), this::selectDetailView);
    }

    public void clear() {
        directoryPath.clear();
        selectedDirectory.set(null);
        listingMode.setValue(DisplayColumn.Mode.NATIVE);
        deletedFilesToggleButton.setSelected(false);
        createNewFileTable();
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
        if (selectedDirectory.isNull().get()) {
            // No directories, leave a cleared list. Likely in transition.
            return;
        }

        createNewFileTable();
        Directory directory = selectedDirectory.get();
        List<DisplayColumn> displayColumns = directory.getFileStore().getDisplayColumns();

        boolean editable = false;
        List<TableColumn<FileEntry,?>> tableColumns = new ArrayList<>();
        for (final DisplayColumn displayColumn : displayColumns) {
            TableColumn<FileEntry,?> column = switch (displayColumn.dataType()) {
                case STRING -> {
                    TableColumn<FileEntry,String> stringColumn = new TableColumn<>(displayColumn.headerText());
                    if (displayColumn.editInline()) {
                        stringColumn.setCellFactory(TextFieldTableCell.forTableColumn());
                        stringColumn.setEditable(true);
                        stringColumn.setCellValueFactory(cell -> {
                            SimpleStringProperty property = new SimpleStringProperty(displayColumn.formatAsText(cell.getValue()));
                            property.addListener((_, _, newValue) -> {
                                displayColumn.setValueFn().accept(cell.getValue(), newValue);
                                fileStoreWindow.addChange();
                            });
                            return property;
                        });
                        editable = true;
                    } else {
                        stringColumn.setCellValueFactory(cell ->
                                new SimpleStringProperty(displayColumn.formatAsText(cell.getValue())));
                    }
                    yield stringColumn;
                }
                case INTEGER,LONG,DOUBLE -> {
                    TableColumn<FileEntry,Number> numberColumn = new TableColumn<>(displayColumn.headerText());
                    numberColumn.setCellFactory(c -> new TableCell<>() {
                        @Override
                        protected void updateItem(Number item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                            } else {
                                setText(String.format(displayColumn.fmt(), item));
                            }
                        }
                    });
                    switch (displayColumn.dataType()) {
                        case INTEGER:
                            numberColumn.setCellValueFactory(cell ->
                                    new SimpleIntegerProperty((Integer)displayColumn.getValueFn().apply(cell.getValue())));
                            break;
                        case LONG:
                            numberColumn.setCellValueFactory(cell ->
                                    new SimpleLongProperty((Long)displayColumn.getValueFn().apply(cell.getValue())));
                            break;
                        case DOUBLE:
                            numberColumn.setCellValueFactory(cell ->
                                    new SimpleDoubleProperty((Double)displayColumn.getValueFn().apply(cell.getValue())));
                            break;
                    }
                    yield numberColumn;
                }
            };
            column.setUserData(displayColumn);
            column.visibleProperty().setValue(displayColumn.supports(listingMode.get()));
            if (displayColumn.alignment() == DisplayColumn.Alignment.RIGHT) {
                column.setStyle("-fx-alignment: CENTER-RIGHT;");
            } else if (displayColumn.alignment() == DisplayColumn.Alignment.CENTER) {
                column.setStyle("-fx-alignment: CENTER;");
            } else {
                column.setStyle("-fx-alignment: CENTER-LEFT;");
            }
            tableColumns.add(column);
        }
        fileTable.setEditable(editable);
        fileTable.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.F2) {
                TableView.TableViewFocusModel<FileEntry> focusModel = fileTable.getFocusModel();
                @SuppressWarnings("unchecked")
                final TablePosition<FileEntry,?> pos = focusModel.getFocusedCell();
                fileTable.edit(pos.getRow(), pos.getTableColumn());
                event.consume();
            }
            else if (event.getCode() == KeyCode.DELETE && fileTable.getSelectionModel().getSelectedItem() != null) {
                FileEntry entry = fileTable.getSelectionModel().getSelectedItem();
                FileStore fileStore = entry.getFileStore();
                if (fileStore.can(Capability.DELETE_FILES)) {
                    WritableDirectory writableDirectory = (WritableDirectory) fileStore.getRootDirectory();
                    writableDirectory.deleteFile(entry);
                    fileEntries.remove(entry);
                    fileStoreWindow.addChange();
                }
            }
        });

        List<? extends FileEntry> rows = directory.getFiles().stream()
                .filter(fileEntry -> deletedFilesToggleButton.isSelected() || !fileEntry.isDeleted())
                .toList();
        fileEntries.clear();
        fileEntries.addAll(rows);
        SortedList<FileEntry> sortedRows = new SortedList<>(fileEntries);
        fileTable.getSortOrder().clear();
        fileTable.setItems(sortedRows);
        fileTable.getColumns().addAll(tableColumns);
        sortedRows.comparatorProperty().bind(fileTable.comparatorProperty());
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
