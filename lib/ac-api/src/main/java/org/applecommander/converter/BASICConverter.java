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

import org.applecommander.util.DataBuffer;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/// This is a generalized BASIC converter interface.
public interface BASICConverter {
    void generate(EventHandler handler);

    /// Test if this character is an ASCII character between A and Z. Java's tests are for Unicode.
    default boolean isCharacter(int ch) {
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z');
    }

    /// Test if this character is a digit. Just in case the digits have extra codes for Unicode.
    default boolean isDigit(int ch) {
        return ch >= '0' && ch <= '9';
    }

    /// Scan the program buffer until the end characters are encountered. Includes end character
    /// in the output unless it's a `$00`. Useful for reading an entire string or to find the end of line.
    default String scanUntil(DataBuffer program, String start, int... endChars) {
        Set<Integer> endValues = new HashSet<>();
        for (int endChar : endChars) endValues.add(endChar);
        StringBuilder sb = new StringBuilder(start);
        int value = program.readUnsignedByte();
        while (!endValues.contains(value)) {
            sb.append((char)value);
            value = program.readUnsignedByte();
        }
        if (value != 0) {
            sb.append((char)value);
        }
        return sb.toString();
    }

    /// Scan the program buffer while the test is true. Includes the first character in the
    /// buffer unless it's a `-1`. Rewinds the buffer by a character when the character
    /// test indicates it's read too far. Useful for reading a number in its entirety.
    default String scanWhile(DataBuffer program, int firstChar, Predicate<Integer> includeFn) {
        StringBuilder sb = new StringBuilder();
        if (firstChar != -1) {
            sb.append((char)firstChar);
        }
        int value = program.readUnsignedByte();
        while (includeFn.test(value)) {
            sb.append((char)value);
            value = program.readUnsignedByte();
        }
        // Rewind to the character we don't want
        program.position(program.position()-1);
        return sb.toString();
    }

    interface EventHandler {
        void startOfLine(String lineNumber);
        void rawBytes(byte[] data);
        void token(String text);
        void comment(String comment);
        void endOfLine();
        void string(String string);
        void targetLine(String lineNumber);
        void identifier(String identifier);
        void number(String number);
        void syntax(String syntax);
    }
}
