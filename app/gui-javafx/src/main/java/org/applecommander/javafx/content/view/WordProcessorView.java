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

import javafx.scene.text.TextAlignment;
import org.applecommander.converter.ConverterRepository;
import org.applecommander.converter.WordProcessorConverter;
import org.applecommander.filestore.FileEntry;
import org.applecommander.javafx.FxUtils;
import org.applecommander.javafx.content.FileContentWindow;

/// Provides a common view to word processing documents.
public class WordProcessorView extends FormattedTextView {
    private final String fileTypeText;
    private final WordProcessorConverter converter;

    public WordProcessorView(FileContentWindow parent) {
        FileEntry fileEntry = parent.getFileEntry();
        fileTypeText = fileEntry.getProdosAttributes().fileTypeText();
        this.converter = ConverterRepository.getWordProcessor(fileEntry).orElseThrow();
        super(parent);
        refresh();
    }

    /// Refresh the contents of the text area.
    @Override
    public void refresh() {
        textArea.replaceText("");
        StyleTracker styleTracker = new StyleTracker(parent.fontSizeProperty().getValue());
        textArea.setParagraphStyle(textArea.getCurrentParagraph(), styleTracker.getStyle());
        try {
            converter.convert(new WordProcessorEventHandler(styleTracker));
        } catch (Throwable t) {
            FxUtils.showErrorDialog("Error converting document", t);
        }
    }

    /// Get the toggle button configuration. The `FileContentWindow` will set up the toolbar
    /// with this button. When the button is clicked, the view will be displayed.
    @Override
    public ButtonConfig getButtonConfig() {
        return new ButtonConfig(fileTypeText, "document.png");
    }

    /// The RichTextFX component takes styling by CSS, and the styles replace the existing
    /// style. This component tracks current state and builds the appropriate CSS style
    /// when requested. It also tracks if there have been changes to reduce the number
    /// of styles being applied.
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
                sb.append("; ");
            }
            this.changed = false;
            return sb.toString();
        }
    }

    private class WordProcessorEventHandler implements WordProcessorConverter.EventHandler {
        private final StyleTracker styleTracker;
        // Used to skip sections of text, such as page headers and footers.
        private boolean ignore;

        public WordProcessorEventHandler(StyleTracker styleTracker) {
            this.styleTracker = styleTracker;
            ignore = false;
        }

        /// Add a text string into the resulting document.
        @Override
        public void text(String text) {
            if (ignore) return;
            if (styleTracker.hasChanged()) {
                textArea.setParagraphStyle(textArea.getCurrentParagraph(), styleTracker.getStyle());
            }
            textArea.appendText(text);
        }

        /// Handler a control element. Note that some elements _can_ have parameters, but
        /// they can vary across formats, so supply sensible defaults and code for the no-args
        /// situations.
        @Override
        public void control(WordProcessorConverter.Control control, int... args) {
            if (ignore) {
                ignore = switch (control) {
                    case END_PAGE_HEADER, END_PAGE_FOOTER -> false;
                    default -> true;
                };
                return;
            }
            switch (control) {
                case BEGIN_BOLD -> styleTracker.setBold(true);
                case END_BOLD -> styleTracker.setBold(false);
                case BEGIN_UNDERLINE -> styleTracker.setUnderline(true);
                case END_UNDERLINE -> styleTracker.setUnderline(false);
                case BEGIN_SUPERSCRIPT -> styleTracker.setSuperscript(true);
                case END_SUPERSCRIPT -> styleTracker.setSuperscript(false);
                case BEGIN_SUBSCRIPT -> styleTracker.setSubscript(true);
                case END_SUBSCRIPT -> styleTracker.setSubscript(false);
                case ALIGN_LEFT -> styleTracker.setAlignment(TextAlignment.LEFT);
                case ALIGN_CENTER -> styleTracker.setAlignment(TextAlignment.CENTER);
                case ALIGN_RIGHT -> styleTracker.setAlignment(TextAlignment.RIGHT);
                case ALIGN_JUSTIFY -> styleTracker.setAlignment(TextAlignment.JUSTIFY);
                case BEGIN_PAGE_HEADER, BEGIN_PAGE_FOOTER -> ignore = true;
                default -> {}
            }
        }
    }
}
