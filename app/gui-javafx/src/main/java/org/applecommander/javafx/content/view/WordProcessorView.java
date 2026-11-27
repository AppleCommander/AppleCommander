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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.text.TextAlignment;
import org.applecommander.converter.ConverterRepository;
import org.applecommander.converter.WordProcessorConverter;
import org.applecommander.converter.WordProcessorConverter.DocumentSection;
import org.applecommander.converter.WordProcessorConverter.EventHandler;
import org.applecommander.converter.WordProcessorConverter.FontFamily;
import org.applecommander.filestore.FileEntry;
import org.applecommander.javafx.FxUtils;
import org.applecommander.javafx.content.FileContentWindow;

import java.util.Optional;

/// Provides a common view to word processing documents.
public class WordProcessorView extends FormattedTextView {
    private final String fileTypeText;
    private final WordProcessorConverter converter;
    private final HBox toolBar = new HBox();
    private final ToggleButton headerButton;
    private final ToggleButton footerButton;
    private final ObjectProperty<DocumentSection> sectionProperty = new SimpleObjectProperty<>();

    public WordProcessorView(FileContentWindow parent) {
        FileEntry fileEntry = parent.getFileEntry();
        fileTypeText = fileEntry.getProdosAttributes().fileTypeText();
        this.converter = ConverterRepository.getWordProcessor(fileEntry).orElseThrow();
        super(parent);
        // Setup section toggles; must be after superclass constructor
        ToggleGroup section = new ToggleGroup();
        ToggleButton bodyButton = FxUtils.createToggleButton("page-layout-body.png", "Body", section, e -> {
            sectionProperty.setValue(DocumentSection.BODY);
        });
        bodyButton.setSelected(true);
        headerButton = FxUtils.createToggleButton("page-layout-header.png", "Header", section, e -> {
            sectionProperty.setValue(DocumentSection.HEADER);
        });
        footerButton = FxUtils.createToggleButton("page-layout-footer.png", "Footer", section, e -> {
            sectionProperty.setValue(DocumentSection.FOOTER);
        });
        this.toolBar.setSpacing(0);
        this.toolBar.getChildren().addAll(bodyButton, headerButton, footerButton);

        sectionProperty.addListener((_, _, _) -> {
            refresh();
        });
        sectionProperty.setValue(DocumentSection.BODY);
    }

    /// Refresh the contents of the text area.
    @Override
    public void refresh() {
        textArea.replaceText("");
        StyleTracker styleTracker = new StyleTracker(parent.fontSizeProperty().getValue());
        textArea.setParagraphStyle(textArea.getCurrentParagraph(), styleTracker.getStyle());
        WordProcessorEventHandler wpEventHandler = new WordProcessorEventHandler(styleTracker, sectionProperty.getValue());
        try {
            converter.convert(wpEventHandler);
        } catch (Throwable t) {
            FxUtils.showErrorDialog("Error converting document", t);
        }
        this.headerButton.setDisable(wpEventHandler.isHeaderEmpty());
        this.footerButton.setDisable(wpEventHandler.isFooterEmpty());
    }

    /// Get the toggle button configuration. The `FileContentWindow` will set up the toolbar
    /// with this button. When the button is clicked, the view will be displayed.
    @Override
    public ButtonConfig getButtonConfig() {
        return new ButtonConfig(fileTypeText, "document.png");
    }

    /// Provide any additional toolbar items. Note that a single button can be returned
    /// with as a `Button` but multiple buttons should be in an `HBox` or similar
    /// container.
    public Optional<Node> getAdditionalToolbarButtons() {
        return Optional.of(toolBar);
    }

    /// The RichTextFX component takes styling by CSS, and the styles replace the existing
    /// style. This component tracks current state and builds the appropriate CSS style
    /// when requested. It also tracks if there have been changes to reduce the number
    /// of styles being applied.
    public static class StyleTracker {
        private boolean changed = false;
        private double fontSize;
        private String fontFace = "monospace";
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
        public void setFontFace(String fontFace) {
            this.fontFace = fontFace;
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
            sb.append(String.format("%f %s; ", fontSize, fontFace));
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

    private class WordProcessorEventHandler implements EventHandler {
        private final StyleTracker styleTracker;
        private final DocumentSection desiredSection;
        private DocumentSection currentSection;
        private boolean headerEmpty = true;
        private boolean footerEmpty = true;

        public WordProcessorEventHandler(StyleTracker styleTracker, DocumentSection desiredSection) {
            this.styleTracker = styleTracker;
            this.desiredSection = desiredSection;
            this.currentSection = DocumentSection.BODY;
        }

        public boolean isHeaderEmpty() {
            return headerEmpty;
        }
        public boolean isFooterEmpty() {
            return footerEmpty;
        }

        /// Add a text string into the resulting document.
        @Override
        public void text(String text) {
            if (!text.isBlank()) {
                if (currentSection == DocumentSection.HEADER) headerEmpty = false;
                if (currentSection == DocumentSection.FOOTER) footerEmpty = false;
            }
            if (desiredSection != currentSection) return;
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
            if (desiredSection != currentSection) {
                return;
            }
            switch (control) {
                case BEGIN_BOLD -> styleTracker.setBold(true);
                case END_BOLD -> styleTracker.setBold(false);
                case BEGIN_UNDERLINE -> styleTracker.setUnderline(true);
                case END_UNDERLINE -> styleTracker.setUnderline(false);
                case BEGIN_ITALIC -> styleTracker.setItalic(true);
                case END_ITALIC -> styleTracker.setItalic(false);
                case BEGIN_SUPERSCRIPT -> styleTracker.setSuperscript(true);
                case END_SUPERSCRIPT -> styleTracker.setSuperscript(false);
                case BEGIN_SUBSCRIPT -> styleTracker.setSubscript(true);
                case END_SUBSCRIPT -> styleTracker.setSubscript(false);
                case FONT_SIZE -> {
                    if (args.length > 0) {
                        styleTracker.setFontSize(args[0]);
                    }
                }
                case ALIGN_LEFT -> styleTracker.setAlignment(TextAlignment.LEFT);
                case ALIGN_CENTER -> styleTracker.setAlignment(TextAlignment.CENTER);
                case ALIGN_RIGHT -> styleTracker.setAlignment(TextAlignment.RIGHT);
                case ALIGN_JUSTIFY -> styleTracker.setAlignment(TextAlignment.JUSTIFY);
                default -> {}
            }
        }

        /// Identifies which section of the document is being processed.
        @Override
        public void section(WordProcessorConverter.DocumentSection documentSection) {
            this.currentSection = documentSection;
        }

        /// Indicate what type of font to use. It is likely that the font does not exist
        /// on the system, so leaving that to implementation to "figure it out."
        @Override
        public void font(WordProcessorConverter.FontFamily fontFamily) {
            styleTracker.setFontFace(switch (fontFamily) {
                case FontFamily.MONACO, FontFamily.DEFAULT_MONOSPACE -> "monospace";
                default -> "system";
            });
        }
    }
}
