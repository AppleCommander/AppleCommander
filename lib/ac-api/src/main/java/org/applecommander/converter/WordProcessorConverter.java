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
package org.applecommander.converter;

/// A generic word processor converter. It separates text from control sequences.
public interface WordProcessorConverter {
    /// Convert the document using the given event handler callback.
    void convert(EventHandler handler);

    /// The event handler is the callback mechanism for rendering the document.
    interface EventHandler {
        /// Add a text string into the resulting document.
        void text(String text);

        /// Handler a control element. Note that some elements _can_ have parameters, but
        /// they can vary across formats, so supply sensible defaults and code for the no-args
        /// situations.
        void control(Control control, int... args);
    }

    /// Control elements that the word processing document may generate. Not all features are
    /// present, and they will likely grow as more documents are handled.
    enum Control {
        BEGIN_BOLD,
        END_BOLD,
        BEGIN_SUPERSCRIPT,
        END_SUPERSCRIPT,
        BEGIN_SUBSCRIPT,
        END_SUBSCRIPT,
        BEGIN_UNDERLINE,
        END_UNDERLINE,
        SET_PAGE_NUMBER,
        SHOW_PAGE_NUMBER,
        PRINT_DATE,
        PRINT_TIME,
        BEGIN_PAGE_HEADER,
        END_PAGE_HEADER,
        BEGIN_PAGE_FOOTER,
        END_PAGE_FOOTER,
        INDENT_CHARS,
        ALIGN_JUSTIFY,
        ALIGN_LEFT,
        ALIGN_RIGHT,
        ALIGN_CENTER,
        SPACING_SINGLE,
        SPACING_DOUBLE,
        SPACING_TRIPLE,
        PAGE_BREAK
    }
}
