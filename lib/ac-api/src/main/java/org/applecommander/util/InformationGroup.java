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
package org.applecommander.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This is a simple group of informational items for various user interfaces.
 * Use the builder to assist in construction.
 */
public record InformationGroup(String title, List<InformationItem> items) {
    public static Builder builder(String fmt, Object... args) {
        return new Builder(fmt, args);
    }

    public static class Builder {
        private final String title;
        private final List<InformationItem> items = new ArrayList<>();

        public Builder(String fmt, Object... args) {
            this.title = String.format(fmt, args);
        }
        public ItemBuilder item(String fmt, Object... args) {
            return new ItemBuilder(this, fmt, args);
        }
        public List<InformationGroup> get(InformationProvider... providers) {
            List<InformationGroup> result = new ArrayList<>();
            result.add(new InformationGroup(title, items));
            for (InformationProvider provider : providers) {
                result.addAll(provider.information());
            }
            return result;
        }
    }
    public static class ItemBuilder {
        private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        private final Builder builder;
        private final String label;

        private ItemBuilder(Builder builder, String fmt, Object... args) {
            this.builder = builder;
            this.label = String.format(fmt, args);
        }
        public Builder value(String value) {
            builder.items.add(new InformationItem(label, value));
            return builder;
        }
        public Builder value(int value) {
            return value("%d", value);
        }
        public Builder value(String fmt, Object... args) {
            return value(String.format(fmt, args));
        }
        public Builder value(Date date) {
            return value(date != null ? dateFormat.format(date) : "-None-");
        }
    }
}
