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
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.applecommander.converter.ApplesoftVariables;
import org.applecommander.converter.BASICConverter;
import org.applecommander.converter.ConverterRepository;
import org.applecommander.filestore.ContentType;
import org.applecommander.filestore.ContentType.Subtype;
import org.applecommander.filestore.FileEntry;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileContentWindow {
    private static final Map<Subtype,ViewType> SUBTYPE_VIEW_MAP = Map.of(
        Subtype.TEXT, ViewType.TEXT,
        Subtype.BASIC_VARIABLES, ViewType.TEXT,
        Subtype.BASIC, ViewType.BASIC
    );
    private final BorderPane window;
    private final ToolBar toolBar;
    private final Label statusBar;
    private final StackPane content;
    private final ScrollPane textScrollPane;
    private final SelectableTextFlow textView;

    private Stage stage;
    private ToggleButton viewAsText;
    private ToggleButton viewAsBasic;
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

        textScrollPane = new ScrollPane(content);
        textScrollPane.setFitToHeight(true);
        textScrollPane.setFitToWidth(true);

        this.window = new BorderPane();
        this.window.setTop(toolBar);
        this.window.setCenter(textScrollPane);
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
        stage.setTitle(String.format("%s - %s", fileEntry.getFileStore().getLabel(), fileEntry.getName()));
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
        this.textView.visibleProperty().bind(viewTypeProperty.isEqualTo(ViewType.TEXT)
                .or(viewTypeProperty.isEqualTo(ViewType.HEX))
                .or(viewTypeProperty.isEqualTo(ViewType.BASIC)));
        this.textView.managedProperty().bind(textView.visibleProperty());

        this.viewTypeProperty.addListener((_, _, newValue) -> {
            viewAsText.setSelected(newValue == ViewType.TEXT);
            viewAsBasic.setSelected(newValue == ViewType.BASIC);
            viewAsHex.setSelected(newValue == ViewType.HEX);
            switch (newValue) {
                case TEXT, BASIC -> handleTextView();
                case HEX -> {
                    // HEX VIEW FIXME
                    showPlainText(AppleUtil.getHexDump(fileEntry.getDataFork().asBytes()));
                }
            }
            updateStatusBar();
        });
    }

    private void handleTextView() {
        switch (contentSubtypeProperty.getValue()) {
            case TEXT -> {
                ConverterRepository.getTextConverter(fileEntry.getContentType()).ifPresent(textConverter -> {
                    showPlainText(textConverter.read(fileEntry));
                });
            }
            case BASIC_VARIABLES -> {
                ConverterRepository.getVariables(fileEntry).ifPresent(this::showVariables);
            }
            case BASIC -> {
                ConverterRepository.getBASIC(fileEntry).ifPresent(this::showBASIC);
            }
            default -> {
                // HEX VIEW FIXME
                showPlainText(AppleUtil.getHexDump(fileEntry.getDataFork().asBytes()));
            }
        }
    }

    private void initToolbar() {
        ToggleGroup toggleGroup = new ToggleGroup();
        viewAsText = FxUtils.createToggleButton("view-text.png", "Text", toggleGroup, _ -> {
            this.viewTypeProperty.setValue(ViewType.TEXT);
        });
        viewAsText.disableProperty().bind(Bindings.createBooleanBinding(() ->
                contentSubtypeProperty.isNotNull().get() &&
                    SUBTYPE_VIEW_MAP.get(contentSubtypeProperty.getValue()) != ViewType.TEXT,
                contentSubtypeProperty));
        viewAsBasic = FxUtils.createToggleButton("basic-icon.png", "BASIC", toggleGroup, _ -> {
            this.viewTypeProperty.setValue(ViewType.BASIC);
        });
        viewAsBasic.disableProperty().bind(contentSubtypeProperty.isNotEqualTo(Subtype.BASIC));
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
            Font font = Font.font("monospaced", fontSize);
            textView.getChildren().forEach(node -> {
                if (node instanceof Text text) {
                    text.setFont(font);
                }
            });
        });
        Button increaseSize = FxUtils.createButton("plus.png", "Increase", _ -> {
            // FIXME feels a bit hacky
            fontSize += 1;
            updateStatusBar();
            Font font = Font.font("monospaced", fontSize);
            textView.getChildren().forEach(node -> {
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

        this.toolBar.getItems().addAll(viewAsText, viewAsBasic, viewAsHex,
                new Separator(),
                decreaseSize, increaseSize, copy);
    }

    private void showPlainText(String value) {
        Text text = new Text(value);
        text.setFont(Font.font("monospaced", fontSize));
        textView.getChildren().clear();
        textView.getChildren().add(text);
    }

    private void showVariables(ApplesoftVariables variables) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.printf("Simple variables:\n");
        var names = variables.getSimpleNames();
        names.forEach(varName -> {
            if (varName.endsWith("$")) {
                pw.printf("%s = \"%s\"\n", varName, variables.getSimpleValue(varName));
            }
            else {
                pw.printf("%s = %s\n", varName, variables.getSimpleValue(varName));
            }
        });
        if (names.isEmpty()) {
            pw.printf("-- None --\n");
        }

        pw.printf("\nArray variables:\n");
        names = variables.getArrayNames();
        names.forEach(varName -> {
            pw.printf("DIM %s(%s)\n", varName, variables.getArrayDimensions(varName).stream()
                    .map(Object::toString).collect(Collectors.joining(",")));
        });
        if (names.isEmpty()) {
            pw.printf("-- None --\n");
        }

        textView.getChildren().clear();
        textView.getChildren().add(new Text(sw.toString()));
    }

    private void showBASIC(BASICConverter converter) {
        BASICEventHandler handler = new BASICEventHandler(fontSize, textScrollPane);
        converter.generate(handler);

        textView.getChildren().clear();
        textView.getChildren().addAll(handler.getComponents());
    }

    private void updateStatusBar() {
        if (viewTypeProperty.getValue() == null) {
            statusBar.setText("...");
            return;
        }
        switch (viewTypeProperty.getValue()) {
            case TEXT, HEX, BASIC -> statusBar.setText(String.format("Font size: %1.0f pt", fontSize));
        }
    }

    private enum ViewType {
        TEXT,
        HEX,
        BASIC
    }

    public static class BASICEventHandler implements BASICConverter.EventHandler {
        private final Font font;
        private final List<Node> components = new ArrayList<>();
        private final ScrollPane scrollPane;
        private final Map<String,Node> lineNumbers = new HashMap<>();

        public BASICEventHandler(double fontSize, ScrollPane scrollPane) {
            this.font = Font.font("monospaced", fontSize);
            this.scrollPane = scrollPane;
        }

        public List<Node> getComponents() {
            return components;
        }
        private void addText(String value) {
            if (components.isEmpty() || !(components.getLast() instanceof Text text)) {
                addNewText(value);
            }
            else {
                text.setText(text.getText() + value);
            }
        }
        private Node addNewText(String value) {
            Text text = new Text(value);
            text.setFont(font);
            components.add(text);
            return text;
        }
        private void addLink(String value) {
            Hyperlink link = new Hyperlink();
            link.setFont(font);
            link.setText(value);
            link.setOnAction(_ -> {
                Node node = lineNumbers.get(value.trim());
                if (node != null) {
                    FxUtils.centerNodeInScrollPane(scrollPane, node);
                }
            });
            components.add(link);
        }

        @Override
        public void startOfLine(String lineNumber) {
            Node node = addNewText(lineNumber);
            lineNumbers.put(lineNumber.trim(), node);
        }
        @Override
        public void rawBytes(byte[] data) {
            addText("*** HEX - TODO ***");
        }
        @Override
        public void token(String text) {
            addText(text);
        }
        @Override
        public void comment(String comment) {
            addText(comment);
        }
        @Override
        public void endOfLine() {
            addText("\n");
        }
        @Override
        public void string(String string) {
            addText(string);
        }
        @Override
        public void targetLine(String lineNumber) {
            addLink(lineNumber);
        }
        @Override
        public void identifier(String identifier) {
            addText(identifier);
        }
        @Override
        public void number(String number) {
            addText(number);
        }
        @Override
        public void syntax(String syntax) {
            addText(syntax);
        }
    }
}
