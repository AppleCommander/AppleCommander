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
package org.applecommander.javafx.filter;

import javafx.scene.control.TextFormatter;

import java.util.function.UnaryOperator;

// See: https://www.reddit.com/r/JavaFX/comments/qd5uds/how_do_you_limit_the_character_input_in_a/
public class FixedLengthFilter implements UnaryOperator<TextFormatter.Change> {
    private final int maxLength;

    public FixedLengthFilter(int maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public TextFormatter.Change apply(TextFormatter.Change change) {
        if (change.getControlNewText().length() > maxLength) {
            return null;
        }
        return change;
    }
}
