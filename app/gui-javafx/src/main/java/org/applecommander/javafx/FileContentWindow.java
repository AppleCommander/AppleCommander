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

import atlantafx.base.controls.SelectableTextFlow;
import com.webcodepro.applecommander.util.AppleUtil;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.applecommander.converter.ConverterRepository;
import org.applecommander.filestore.ContentType;
import org.applecommander.filestore.ContentType.Subtype;
import org.applecommander.filestore.FileEntry;

import java.util.Map;

public class FileContentWindow {
    private static final Map<Subtype,ViewType> SUBTYPE_VIEW_MAP = Map.of(
        Subtype.TEXT, ViewType.TEXT
    );
    private final BorderPane window;
    private final ToolBar toolBar;
    private final Label statusBar;
    private final StackPane content;
    private final SelectableTextFlow textView;

    private Stage stage;
    private ToggleButton viewAsText;
    private ToggleButton viewAsHex;
    private final ObjectProperty<ContentType> contentTypeProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<Subtype> contentSubtypeProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<ViewType> viewTypeProperty = new SimpleObjectProperty<>();
    private double fontSize;

    private final FileEntry fileEntry;

    public FileContentWindow(FileEntry fileEntry) {
        this.toolBar = new ToolBar();

        this.textView = new SelectableTextFlow();
        this.textView.setPadding(new Insets(10));

        this.content = new StackPane(textView);

        this.statusBar = new Label();
        this.statusBar.setPadding(new Insets(6));
        this.statusBar.setAlignment(Pos.CENTER_LEFT);

        this.fileEntry = fileEntry;
        this.fontSize = Font.getDefault().getSize();

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);

        this.window = new BorderPane();
        this.window.setTop(toolBar);
        this.window.setCenter(scrollPane);
        this.window.setBottom(statusBar);

        initView();
        initToolbar();
        updateStatusBar();
    }

    public void show() {
        ContentType contentType = fileEntry.getContentType();
        this.contentTypeProperty.setValue(contentType);

        Scene scene = new Scene(window, 800, 500);
        stage = new Stage();
        stage.setScene(scene);
        stage.toFront();
        stage.requestFocus();
        this.initView();
        stage.show();
    }

    public void close() {
        stage.close();
    }

    private void initView() {
        this.contentTypeProperty.addListener((_, _, newValue) -> {
            contentSubtypeProperty.setValue(newValue.subtype);
            viewTypeProperty.setValue(SUBTYPE_VIEW_MAP.getOrDefault(newValue.subtype, ViewType.HEX));
        });
        this.textView.visibleProperty().bind(viewTypeProperty.isEqualTo(ViewType.TEXT).or(viewTypeProperty.isEqualTo(ViewType.HEX)));
        this.textView.managedProperty().bind(textView.visibleProperty());

        this.viewTypeProperty.addListener((_, _, newValue) -> {
            viewAsText.setSelected(newValue == ViewType.TEXT);
            viewAsHex.setSelected(newValue == ViewType.HEX);
            switch (newValue) {
                case TEXT -> {
                    ConverterRepository.getTextConverter(fileEntry.getContentType()).ifPresentOrElse(textConverter -> {
                        showPlainText(textConverter.read(fileEntry));
                    }, () -> {
                        // HEX VIEW FIXME
                        showPlainText(AppleUtil.getHexDump(fileEntry.getDataFork().asBytes()));
                    });
                }
                case HEX -> {
                    // HEX VIEW FIXME
                    showPlainText(AppleUtil.getHexDump(fileEntry.getDataFork().asBytes()));
                }
            }
            updateStatusBar();
        });
    }

    private void initToolbar() {
        ToggleGroup toggleGroup = new ToggleGroup();
        viewAsText = FxUtils.createToggleButton("view-text.png", "Text", toggleGroup, _ -> {
            this.viewTypeProperty.setValue(ViewType.TEXT);
        });
        viewAsText.disableProperty().bind(contentSubtypeProperty.isNotEqualTo(Subtype.TEXT));
        viewAsHex = FxUtils.createToggleButton("view-hex.png", "Hex", toggleGroup, _ -> {
            this.viewTypeProperty.setValue(ViewType.HEX);
        });

        Button decreaseSize = FxUtils.createButton("minus.png", "Decrease", _ -> {
            // FIXME feels a bit hacky
            fontSize -= 1;
            if (fontSize < 7) {
                fontSize = 7;
            }
            updateStatusBar();
            textView.getChildren().forEach(node -> {
                Font font = Font.font("monospaced", fontSize);
                if (node instanceof Text text) {
                    text.setFont(font);
                }
            });
        });
        Button increaseSize = FxUtils.createButton("plus.png", "Increase", _ -> {
            // FIXME feels a bit hacky
            fontSize += 1;
            updateStatusBar();
            textView.getChildren().forEach(node -> {
                Font font = Font.font("monospaced", fontSize);
                if (node instanceof Text text) {
                    text.setFont(font);
                }
            });
        });
        Button copy = FxUtils.createButton("copy.png", "Copy", _ -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(textView.getContentAsString());
            clipboard.setContent(content);
        });

        this.toolBar.getItems().addAll(viewAsText, viewAsHex, new Separator(), decreaseSize, increaseSize, copy);
    }

    private void showPlainText(String value) {
        Text text = new Text(value);
        text.setFont(Font.font("monospaced", fontSize));
        textView.getChildren().clear();
        textView.getChildren().add(text);
    }

    private void updateStatusBar() {
        if (viewTypeProperty.getValue() == null) {
            statusBar.setText("...");
            return;
        }
        switch (viewTypeProperty.getValue()) {
            case TEXT, HEX -> statusBar.setText(String.format("Font size: %1.0f pt", fontSize));
        }
    }

    private enum ViewType {
        TEXT,
        HEX
    }
}
