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
package org.applecommander.javafx;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SelectionHolder<I> {
        private final List<I> items = new ArrayList<>();
        private int currentSelection = 0;

        public void clear() {
            items.clear();
            currentSelection = 0;
        }

        public void setSelectedItems(List<I> items) {
            Objects.requireNonNull(items);
            this.items.clear();
            this.items.addAll(items);
            this.currentSelection = 0;
        }

        public I getSelectedItem() {
            if (currentSelection >= items.size()) {
                return null;
            }
            return items.get(currentSelection);
        }

        public int getSelectedIndex() {
            return currentSelection;
        }

        public void nextItem() {
            currentSelection++;
            if (currentSelection >= items.size()) {
                currentSelection = 0;
            }
        }

        public int getSize() {
            return items.size();
        }

        public boolean isEmpty() {
            return items.isEmpty();
        }
    }
