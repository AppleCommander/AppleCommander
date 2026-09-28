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

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/// A GSON `Integer` or `int` adapter that converts a typical hex formatted string
/// and converts it to the integer. Supports `$00` and `0x00` formats for hexadecimal
/// as well as a normal base-10 representation.
public class IntegerTypeAdapter extends TypeAdapter<Integer> {
    @Override
    public void write(JsonWriter out, Integer value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.value(String.format("$%d", value));
    }

    @Override
    public Integer read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        String value = in.nextString();
        if (value == null) {
            return null;
        } else if (value.startsWith("$")) {
            return Integer.valueOf(value.substring(1), 16);
        } else if (value.startsWith("0x") || value.startsWith("0X")) {
            return Integer.valueOf(value.substring(2), 16);
        } else {
            return Integer.valueOf(value);
        }
    }
}
