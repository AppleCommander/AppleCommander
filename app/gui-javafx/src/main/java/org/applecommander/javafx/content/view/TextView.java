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

import org.applecommander.converter.ConverterRepository;
import org.applecommander.filestore.FileEntry;
import org.applecommander.javafx.content.FileContentWindow;

/// Provides a simple viewer for text file types. It uses the given `TextConverter`.
public class TextView extends BaseTextView {
    public TextView(FileContentWindow parent) {
        super(parent);
        FileEntry fileEntry = parent.getFileEntry();
        ConverterRepository.getTextConverter(fileEntry.getContentType()).ifPresent(textConverter -> {
            setText(textConverter.read(fileEntry));
        });
    }

    /// Get the toggle button configuration. The `FileContentWindow` will set up the toolbar
    /// with this button. When the button is clicked, the view will be displayed.
    @Override
    public ButtonConfig getButtonConfig() {
        return new ButtonConfig("Text", "view-text.png");
    }
}
