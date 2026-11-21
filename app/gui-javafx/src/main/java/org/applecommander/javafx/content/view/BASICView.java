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
package org.applecommander.javafx.content.view;

import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.applecommander.converter.BASICConverter;
import org.applecommander.converter.ConverterRepository;
import org.applecommander.javafx.FxUtils;
import org.applecommander.javafx.content.FileContentView;
import org.applecommander.javafx.content.FileContentWindow;

import java.util.*;

public class BASICView extends TextFlow implements FileContentView {
    private final FileContentWindow parent;
    private final ToggleButton basicFormatButton;
    private final BASICConverter converter;
    private Font normalFont;
    private Font boldFont;

    public BASICView(FileContentWindow parent) {
        this.parent = parent;
        this.parent.fontSizeProperty().addListener((_, _, newValue) -> {
            double fontSize = newValue.doubleValue();
            Font newNormalFont = Font.font("monospaced", fontSize);
            Font newBoldFont = Font.font("monospaced", FontWeight.BOLD, fontSize);
            for (Node node : getChildren()) {
                if (node instanceof Text text) {
                    // We can't tell the font based on attributes, so checking objects and handling them carefully.
                    if (text.getFont() == this.boldFont) {
                        text.setFont(newBoldFont);
                    }
                    else {
                        text.setFont(newNormalFont);
                    }
                }
            }
            setFonts(normalFont, boldFont);
        });
        this.basicFormatButton = FxUtils.createOnOffButton("basic-list-normal.png", "basic-list-pretty.png",
                "Listing", e -> refresh());
        this.converter = ConverterRepository.getBASIC(parent.getFileEntry()).orElseThrow();

        double fontSize = parent.fontSizeProperty().getValue();
        this.normalFont = Font.font("monospaced", fontSize);
        this.boldFont = Font.font("monospaced", FontWeight.BOLD, fontSize);
        refresh();
    }

    private void setFonts(Font normalFont, Font boldFont) {
        this.normalFont = normalFont;
        this.boldFont = boldFont;
    }

    @Override
    public Node getViewNode() {
        return this;
    }

    @Override
    public ButtonConfig getButtonConfig() {
        return new ButtonConfig("BASIC", "view-basic.png");
    }

    @Override
    public Optional<Node> getAdditionalToolbarButtons() {
        return Optional.of(basicFormatButton);
    }

    public void refresh() {
        boolean prettyPrint = basicFormatButton.isSelected();
        ScrollPane scrollPane = parent.getScrollPane();
        BASICEventHandler handler = new BASICEventHandler(normalFont, boldFont, scrollPane, prettyPrint);
        converter.generate(handler);
        getChildren().clear();
        getChildren().addAll(handler.getComponents());
    }

    @Override
    public void copy() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();

        StringBuilder sb = new StringBuilder();
        for (var node : getChildren()) {
            if (node instanceof Text text) {
                sb.append(text.getText());
            }
        }
        content.putString(sb.toString());

        clipboard.setContent(content);
    }

    public static class BASICEventHandler implements BASICConverter.EventHandler {
        private final List<Node> components = new ArrayList<>();
        private final Font normalFont;
        private final Font boldFont;
        private final ScrollPane scrollPane;
        private final Map<String,Node> lineNumbers = new HashMap<>();
        private final boolean prettyPrint;
        private boolean startOfLine;

        public BASICEventHandler(Font normalFont, Font boldFont, ScrollPane scrollPane, boolean prettyPrint) {
            this.normalFont = normalFont;
            this.boldFont = boldFont;
            this.scrollPane = scrollPane;
            this.prettyPrint = prettyPrint;
        }

        public List<Node> getComponents() {
            return components;
        }
        private void addText(String value, Font font) {
            if (startOfLine) {
                value = value.stripLeading();
                startOfLine = false;
            }
            if (components.isEmpty() || !(components.getLast() instanceof Text text)) {
                addNewText(value, font);
            }
            else if (text.getFont() == font) {
                text.setText(text.getText() + value);
            }
            else {
                addNewText(value, font);
            }
        }
        private Node addNewText(String value, Font font) {
            if (startOfLine) {
                value = value.stripLeading();
                startOfLine = false;
            }
            Text text = new Text(value);
            text.setFont(font);
            components.add(text);
            return text;
        }
        private void addLink(String value) {
            Hyperlink link = new Hyperlink();
            link.setFont(normalFont);
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
            if (prettyPrint) {
                lineNumber = lineNumber.trim() + "\t  ";
            }
            Node node = addNewText(lineNumber, normalFont);
            lineNumbers.put(lineNumber.trim(), node);
            startOfLine = true;
        }
        @Override
        public void rawBytes(byte[] data) {
            addText("*** HEX - TODO ***", normalFont);
        }
        @Override
        public void token(String text) {
            addText(text, boldFont);
        }
        @Override
        public void comment(String comment) {
            addText(comment, normalFont);
        }
        @Override
        public void endOfLine() {
            addText("\n", normalFont);
        }
        @Override
        public void string(String string) {
            addText(string, normalFont);
        }
        @Override
        public void targetLine(String lineNumber) {
            addLink(lineNumber);
        }
        @Override
        public void identifier(String identifier) {
            addText(identifier, normalFont);
        }
        @Override
        public void number(String number) {
            addText(number, normalFont);
        }
        @Override
        public void syntax(String syntax) {
            if (prettyPrint && ":".equals(syntax)) {
                addText("\n\t: ", normalFont);
                startOfLine = true;
            }
            else {
                addText(syntax, normalFont);
            }
        }
    }
}
