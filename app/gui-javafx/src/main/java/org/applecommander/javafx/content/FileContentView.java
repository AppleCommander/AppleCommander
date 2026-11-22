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

import javafx.scene.Node;

import java.util.Optional;

/// The `FileContentView` is the sub-view interface that the `FileContentWindow` uses.
/// Note that there is no assumption about scrollable regions, so if a view needs it,
/// it must provide it. This is done in order to allow custom toolbars or menus.
public interface FileContentView {
    /// Return the primary graphical `Node` representing the contents.
    Node getViewNode();

    /// Get the toggle button configuration. The `FileContentWindow` will set up the toolbar
    /// with this button. When the button is clicked, the view will be displayed.
    ButtonConfig getButtonConfig();

    /// Provide any additional toolbar items. Note that a single button can be returned
    /// with as a `Button` but multiple buttons should be in an `HBox` or similar
    /// container.
    default Optional<Node> getAdditionalToolbarButtons() {
        return Optional.empty();
    }

    /// Perform a copy of the contents to the clipboard.
    void copy();

    /// Toggle button configuration.
    /// @param text The text to be displayed below the image in the button.
    /// @param imageName The name of the image to be displayed.
    record ButtonConfig(String text, String imageName) {}
}
