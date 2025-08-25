/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2025 by Robert Greene and others
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
import java.util.Date;

/**
 * This is a simple record to provide information for various user interfaces.
 * Use the builder to assist in construction.
 */
public record Information(String label, String value) {
    public static Builder builder(String label) {
        return new Builder(label);
    }

    public static class Builder {
        private static final SimpleDateFormat dateFormatter = new SimpleDateFormat();
        private final String label;

        private Builder(String label) {
            this.label = label;
        }
        public Information value(String value) {
            return new Information(label, value);
        }
        public Information value(int value) {
            return value("%d", value);
        }
        public Information value(String fmt, Object... args) {
            return new Information(label, String.format(fmt, args));
        }
        public Information value(Date date) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            return new Information(label, date != null ? dateFormat.format(date) : "-None-");
        }
    }
}
