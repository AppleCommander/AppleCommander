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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.applecommander.javafx.content.FileContentView;
import org.applecommander.javafx.content.FileContentWindow;
import org.fxmisc.richtext.InlineCssTextArea;

public abstract class FormattedTextView implements FileContentView {
    protected final FileContentWindow parent;
    protected final InlineCssTextArea textArea;

    public FormattedTextView(FileContentWindow parent) {
        this.parent = parent;
        this.textArea = new InlineCssTextArea();
        this.textArea.autosize();
        this.textArea.setEditable(false);
        this.textArea.setWrapText(true);

        parent.fontSizeProperty().addListener((_, _, _) -> {
            refresh();
        });
    }

    /// Refresh the contents of the text area.
    public abstract void refresh();

    /// Return the primary graphical `Node` representing the contents.
    @Override
    public Node getViewNode() {
        return textArea;
    }

    /// Perform a copy of the contents to the clipboard.
    @Override
    public void copy() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(textArea.getText());
        clipboard.setContent(content);
    }
}
