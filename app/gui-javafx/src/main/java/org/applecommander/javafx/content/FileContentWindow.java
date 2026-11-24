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
package org.applecommander.javafx.content;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.applecommander.filestore.ContentType;
import org.applecommander.filestore.FileEntry;
import org.applecommander.javafx.FxUtils;
import org.applecommander.javafx.content.view.*;
import org.applecommander.javafx.settings.Settings;

import java.net.URL;
import java.util.Objects;

public class FileContentWindow {
    private final BorderPane window;
    private final ToolBar toolBar;
    private final Label statusBar;
    private final ToggleGroup toggleGroup;
    private final HBox viewSelectionButtons;
    private final HBox viewCustomizationButtons;

    private Stage stage;
    private final DoubleProperty fontSizeProperty = new SimpleDoubleProperty();

    private final FileEntry fileEntry;
    private FileContentView currentView;

    public FileContentWindow(FileEntry fileEntry) {
        this.toolBar = new ToolBar();

        this.statusBar = new Label();
        this.statusBar.setPadding(new Insets(6));
        this.statusBar.setAlignment(Pos.CENTER_LEFT);

        this.fileEntry = fileEntry;
        this.fontSizeProperty.set(Font.getDefault().getSize() + 1.0);

        this.window = new BorderPane();
        this.window.setTop(toolBar);
        this.window.setBottom(statusBar);

        this.viewSelectionButtons = new HBox();
        this.viewSelectionButtons.setSpacing(0);
        this.viewCustomizationButtons = new HBox();
        this.viewCustomizationButtons.setSpacing(6);

        initToolbar();
        this.toggleGroup = new ToggleGroup();
        switch (fileEntry.getContentType().subtype) {
            case BASIC -> addView(new BASICView(this));
            case BASIC_VARIABLES -> addView(new VariablesView(this));
            case TEXT -> addView(new TextView(this));
            case DISASSEMBLY -> addView(new DisassemblyView(this));
            case WORD_PROCESSOR -> addView(new WordProcessorView(this));
        }
        addView(new HexView(this));
        // We don't have a "known" executable (a general BIN is what this is), add disassembly as a secondary option
        if (fileEntry.getContentType() == ContentType.BINARY) {
            addView(new DisassemblyView(this));
        }
        // Make certain the first one is visible
        this.toggleGroup.selectToggle(this.toggleGroup.getToggles().getFirst());
        updateStatusBar();
    }

    public FileEntry getFileEntry() {
        return fileEntry;
    }
    public DoubleProperty fontSizeProperty() {
        return fontSizeProperty;
    }

    public void show() {
        Scene scene = new Scene(window, 800, 500);
        // Not a great solution, but we need to apply themes to the RichTextFX component since it ignores the existing
        // theme. The 'ThemeSelection' object contains the modifier theme to hook them together. The CSS cannot be
        // applied in the application, since that is a _single_ CSS file, and the Scene itself doesn't show anywhere
        // in the created objects (it's null). Thus, this shim/hack/bad code is here. :shrug:
        Settings.getThemeSelection().ifPresent(theme -> {
            if (theme.getModifierUrl() != null) {
                URL resource = getClass().getResource(theme.getModifierUrl());
                Objects.requireNonNull(resource);
                scene.getStylesheets().addAll(resource.toExternalForm());
            }
        });

        stage = new Stage();
        stage.setScene(scene);
        stage.setTitle(String.format("%s - %s", fileEntry.getFileStore().getLabel(), fileEntry.getName()));
        stage.toFront();
        stage.requestFocus();
        stage.show();
    }

    public void close() {
        stage.close();
    }

    public void addView(FileContentView view) {
        FileContentView.ButtonConfig config = view.getButtonConfig();
        ToggleButton toggleButton = FxUtils.createToggleButton(config.imageName(), config.text(), toggleGroup, _ -> {
            showView(view);
        });
        this.viewSelectionButtons.getChildren().add(toggleButton);
        if (this.window.getCenter() == null) {
            showView(view);
        }
    }

    /// Toggle a view into the display. Note that this also handles swapping buttons.
    public void showView(FileContentView view) {
        this.window.setCenter(view.getViewNode());
        this.currentView = view;
        view.getAdditionalToolbarButtons().ifPresentOrElse(node -> {
            this.viewCustomizationButtons.getChildren().setAll(node);
        }, () -> {
            this.viewCustomizationButtons.getChildren().clear();
        });
    }

    private void increaseFontSize(ActionEvent event) {
        double fontSize = fontSizeProperty.getValue();
        fontSize -= 1;
        if (fontSize < 7) {
            fontSize = 7;
        }
        fontSizeProperty.setValue(fontSize);
        updateStatusBar();
    }

    private void decreaseFontSize(ActionEvent event) {
        double fontSize = fontSizeProperty.getValue();
        fontSize += 1;
        fontSizeProperty.setValue(fontSize);
        updateStatusBar();
    }

    private void initToolbar() {
        Button decreaseSize = FxUtils.createButton("minus.png", "Decrease", this::increaseFontSize);
        Button increaseSize = FxUtils.createButton("plus.png", "Increase", this::decreaseFontSize);

        Button copy = FxUtils.createButton("copy.png", "Copy", _ -> {
            if (this.currentView != null) {
                this.currentView.copy();
            }
        });

        this.toolBar.getItems().addAll(this.viewSelectionButtons,
                new Separator(),
                decreaseSize, increaseSize, copy,
                new Separator(),
                this.viewCustomizationButtons);
    }

    private void updateStatusBar() {
        // More TBD
        statusBar.setText(String.format("Font size: %1.0f pt", fontSizeProperty.getValue()));
    }
}
