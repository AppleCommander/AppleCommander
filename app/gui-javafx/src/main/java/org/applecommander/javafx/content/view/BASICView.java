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
import javafx.scene.control.ToggleButton;
import org.applecommander.converter.BASICConverter;
import org.applecommander.converter.ConverterRepository;
import org.applecommander.javafx.FxUtils;
import org.applecommander.javafx.content.FileContentWindow;

import java.util.Optional;

public class BASICView extends NavigableTextView {
    private final ToggleButton basicFormatButton;
    private final BASICConverter converter;

    public BASICView(FileContentWindow parent) {
        super(parent);
        this.basicFormatButton = FxUtils.createOnOffButton("basic-list-normal.png", "basic-list-pretty.png",
                "Listing", e -> refresh());
        this.converter = ConverterRepository.getBASIC(parent.getFileEntry()).orElseThrow();
        refresh();
    }

    /// Get the toggle button configuration. The `FileContentWindow` will set up the toolbar
    /// with this button. When the button is clicked, the view will be displayed.
    @Override
    public ButtonConfig getButtonConfig() {
        return new ButtonConfig("BASIC", "view-basic.png");
    }

    /// Provide any additional toolbar items. Note that a single button can be returned
    /// with as a `Button` but multiple buttons should be in an `HBox` or similar
    /// container.
    @Override
    public Optional<Node> getAdditionalToolbarButtons() {
        return Optional.of(basicFormatButton);
    }

    @Override
    public void build(TextBuilder builder) {
        if (basicFormatButton == null) {
            // We can get called during construction, skip those calls.
            return;
        }
        boolean prettyPrint = basicFormatButton.isSelected();
        BASICEventHandler handler = new BASICEventHandler(builder, prettyPrint);
        converter.generate(handler);
    }

    public static class BASICEventHandler implements BASICConverter.EventHandler {
        private final boolean prettyPrint;
        private final TextBuilder builder;
        private boolean startOfLine;

        public BASICEventHandler(TextBuilder builder, boolean prettyPrint) {
            this.builder = builder;
            this.prettyPrint = prettyPrint;
        }

        /// Convert the number to an integer. Trying to do this in a safe manner.
        public int toInteger(String text) {
            StringBuilder sb = new StringBuilder();
            text.chars().forEach(ch -> {
                if (Character.isDigit(ch)) sb.append((char)ch);
            });
            if (sb.isEmpty()) {
                return 0;
            }
            return Integer.parseInt(sb.toString());
        }

        @Override
        public void startOfLine(String text) {
            int lineNumber = toInteger(text);
            if (prettyPrint) {
                text = text.trim() + "\t  ";
            }
            builder.addNavigableTarget(lineNumber, text);
            startOfLine = true;
        }
        @Override
        public void rawBytes(byte[] data) {
            builder.addText("*** HEX - TODO ***");
        }
        @Override
        public void token(String text) {
            builder.addText(text);
        }
        @Override
        public void comment(String comment) {
            builder.addText(comment);
        }
        @Override
        public void endOfLine() {
            builder.addText("\n");
        }
        @Override
        public void string(String string) {
            builder.addText(string);
        }
        @Override
        public void targetLine(String text) {
            int lineNumber = toInteger(text);
            builder.addNavigableLink(lineNumber, text);
        }
        @Override
        public void identifier(String identifier) {
            if (prettyPrint && startOfLine) {
                builder.addText(" ");
            }
            builder.addText(identifier);
        }
        @Override
        public void number(String number) {
            builder.addText(number);
        }
        @Override
        public void syntax(String syntax) {
            if (prettyPrint && ":".equals(syntax)) {
                builder.addText("\n\t: ");
                startOfLine = true;
            }
            else {
                builder.addText(syntax);
            }
        }
    }
}
