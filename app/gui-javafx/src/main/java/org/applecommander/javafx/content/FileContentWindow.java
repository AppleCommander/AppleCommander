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
import org.applecommander.filestore.FileEntry;
import org.applecommander.javafx.FxUtils;
import org.applecommander.javafx.content.view.BASICView;
import org.applecommander.javafx.content.view.HexView;
import org.applecommander.javafx.content.view.TextView;
import org.applecommander.javafx.content.view.VariablesView;

public class FileContentWindow {
    private final BorderPane window;
    private final ToolBar toolBar;
    private final Label statusBar;
    private final ScrollPane scrollPane;
    private final ToggleGroup toggleGroup;
    private final HBox viewSelectionButtons;
    private final HBox viewCustomizationButtons;

    private Stage stage;
    private final DoubleProperty fontSizeProperty = new SimpleDoubleProperty();

    private final FileEntry fileEntry;

    public FileContentWindow(FileEntry fileEntry) {
        this.toolBar = new ToolBar();

        this.statusBar = new Label();
        this.statusBar.setPadding(new Insets(6));
        this.statusBar.setAlignment(Pos.CENTER_LEFT);

        this.fileEntry = fileEntry;
        this.fontSizeProperty.set(Font.getDefault().getSize() + 1.0);

        this.scrollPane = new ScrollPane();
        this.scrollPane.setFitToHeight(true);
        this.scrollPane.setFitToWidth(true);
        this.scrollPane.setPadding(new Insets(10));

        this.window = new BorderPane();
        this.window.setTop(toolBar);
        this.window.setCenter(scrollPane);
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
        }
        addView(new HexView(this));
        updateStatusBar();
    }

    public FileEntry getFileEntry() {
        return fileEntry;
    }
    public DoubleProperty fontSizeProperty() {
        return fontSizeProperty;
    }
    public ScrollPane getScrollPane() {
        return scrollPane;
    }

    public void show() {
        Scene scene = new Scene(window, 800, 500);
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
            this.scrollPane.setContent(view.getViewNode());
        });
        this.viewSelectionButtons.getChildren().add(toggleButton);
        view.getAdditionalToolbarButtons().ifPresent(node -> {
            this.viewCustomizationButtons.getChildren().add(node);
        });
        if (this.scrollPane.getContent() == null) {
            this.scrollPane.setContent(view.getViewNode());
        }
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
            if (this.scrollPane.getContent() instanceof FileContentView view) {
                view.copy();
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
