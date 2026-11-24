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

import com.google.common.base.Strings;
import javafx.scene.text.TextAlignment;
import org.applecommander.javafx.content.FileContentWindow;
import org.applecommander.util.DataBuffer;

// A hacked up AWP viewer. Should be made generic (per the name!).
public class WordProcessorView extends FormattedTextView {
    public WordProcessorView(FileContentWindow parent) {
        super(parent);
        refresh();
    }

    /// Refresh the contents of the text area.
    @Override
    public void refresh() {
        DataBuffer data = parent.getFileEntry().getDataFork();
        textArea.replaceText("");
        StyleTracker styleTracker = new StyleTracker(parent.fontSizeProperty().getValue());
        textArea.setTextInsertionStyle(styleTracker.getStyle());
        final int sfMinVers = data.getUnsignedByte(183);
        data.position(300);
        boolean first = true;
        while (data.hasRemaining()) {
            int byte0 = data.readUnsignedByte();
            int byte1 = data.readUnsignedByte();
            if (first && sfMinVers > 0) {
                first = false;
                continue;
            }
            first = false;
            if (byte0 == 0xff && byte1 == 0xff) {
                break;
            }
            switch (byte1) {
                case 0x00 -> {
                    DataBuffer record = data.readBuffer(byte0);
                    // These are adjusted since we already read bytes 0 and 1.
                    if (record.getUnsignedByte(0) != 0xff) {
                        int len = record.getUnsignedByte(1);
                        boolean addCR = len >= 0x80;
                        len &= 0x7f;
                        StringBuilder sb = new StringBuilder();
                        for (int i=0; i<len; i++) {
                            int ch = record.getUnsignedByte(i+2);
                            if (ch >= 0x20) {
                                sb.append((char)ch);
                            }
                            else {
                                switch (ch) {
                                    case 0x01 -> styleTracker.setBold(true);
                                    case 0x02 -> styleTracker.setBold(false);
                                    case 0x07 -> styleTracker.setUnderline(true);
                                    case 0x08 -> styleTracker.setUnderline(false);
                                }
                            }
                        }
                        if (addCR) sb.append("\n");
                        textArea.appendText(sb.toString());
                    }
                }
                case 0xd0 -> textArea.appendText("\n");
                case 0xde -> textArea.appendText(Strings.repeat(" ", byte0));
                case 0xdf -> styleTracker.setAlignment(TextAlignment.JUSTIFY);
                case 0xe0 -> styleTracker.setAlignment(TextAlignment.LEFT);
                case 0xe1 -> styleTracker.setAlignment(TextAlignment.CENTER);
                default -> {}
            }
            if (styleTracker.hasChanged()) {
                textArea.setTextInsertionStyle(styleTracker.getStyle());
            }
        }
    }

    public static class StyleTracker {
        private boolean changed = false;
        private double fontSize;
        private boolean bold = false;
        private boolean underline = false;
        private boolean italic = false;
        private boolean superscript = false;
        private boolean subscript = false;
        private TextAlignment alignment = TextAlignment.LEFT;

        public StyleTracker(double fontSize) {
            this.fontSize = fontSize;
        }

        public boolean hasChanged() {
            return changed;
        }
        public void setAlignment(TextAlignment alignment) {
            this.alignment = alignment;
            this.changed = true;
        }
        public void setBold(boolean bold) {
            this.bold = bold;
            this.changed = true;
        }
        public void setFontSize(double fontSize) {
            this.fontSize = fontSize;
            this.changed = true;
        }
        public void setItalic(boolean italic) {
            this.italic = italic;
            this.changed = true;
        }
        public void setUnderline(boolean underline) {
            this.underline = underline;
            this.changed = true;
        }
        public void setSuperscript(boolean superscript) {
            this.superscript = superscript;
            this.changed = true;
        }

        public void setSubscript(boolean subscript) {
            this.subscript = subscript;
            this.changed = true;
        }

        public String getStyle() {
            StringBuilder sb = new StringBuilder();
            sb.append("-fx-font: ");
            if (italic) {
                sb.append("italic ");
            }
            if (bold) {
                sb.append("bold ");
            }
            sb.append(String.format("%f monospace; ", fontSize));
            if (superscript) {
                sb.append("-fx-translate-y: ");
                sb.append(fontSize * -0.3);
            }
            if (subscript) {
                sb.append("-fx-translate-y: ");
                sb.append(fontSize * 0.3);
            }
            if (underline) {
                sb.append("-fx-underline: true; ");
            }
            if (alignment != TextAlignment.LEFT) {
                sb.append("-fx-text-alignment: ");
                sb.append(alignment.name().toLowerCase());
            }
            this.changed = false;
            return sb.toString();
        }
    }

    /// Get the toggle button configuration. The `FileContentWindow` will set up the toolbar
    /// with this button. When the button is clicked, the view will be displayed.
    @Override
    public ButtonConfig getButtonConfig() {
        return new ButtonConfig("AWP", "document.png");
    }

}
