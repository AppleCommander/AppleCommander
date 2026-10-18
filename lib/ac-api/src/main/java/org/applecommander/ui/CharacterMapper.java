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
package org.applecommander.ui;

/// `CharacterMapper` utility class that is used to convert an input string into a set of
/// valid characters. This can handle both lower-case to upper-case mappings and
/// restricting input characters or doing things like mapping a ' ' to a '.'.
///
/// Example usage:
/// ```
/// CharacterMapper hex = CharacterMapper.builder()
///     .mapRange('a', 'f', true)
///     .mapRange('A', 'F', false)
///     .mapRange('0', '9', false)
///     .map('$', '$')
///     .build();
///
/// System.out.println(hex.map(">>> $12af <<<"));
/// ```
/// ... yields a `$12AF` output.
public class CharacterMapper {
    private final String fromChars;
    private final String toChars;

    private CharacterMapper(String fromChars, String toChars) {
        this.fromChars = fromChars;
        this.toChars = toChars;
    }

    public String map(String inputString) {
        // No restrictions
        if (fromChars.isBlank() && toChars.isBlank()) {
            return inputString;
        }
        // Copy valid characters ("fromChars") over to the modified string ("toChars")
        StringBuilder sb = new StringBuilder();
        for (char ch : inputString.toCharArray()) {
            int pos = fromChars.indexOf(ch);
            if (pos != -1) {
                sb.append(toChars.charAt(pos));
            }
        }
        return sb.toString();
    }

    public static Builder builder() {
        return new Builder();
    }
    public static class Builder {
        private final StringBuilder fromChars = new StringBuilder();
        private final StringBuilder toChars =  new StringBuilder();

        /// Add a range of characters
        public Builder mapRange(char first, char last, boolean forceUppercase) {
            StringBuilder string = new StringBuilder();
            for (char ch = first; ch <= last; ch++) string.append(ch);
            String alpha = string.toString();

            fromChars.append(alpha);
            if (forceUppercase) alpha = alpha.toUpperCase();
            toChars.append(alpha);
            return this;
        }
        /// Add a single character mapping
        public Builder map(char from, char to) {
            fromChars.append(from);
            toChars.append(to);
            return this;
        }
        public CharacterMapper build() {
            if (fromChars.length() != toChars.length()) {
                throw new RuntimeException("developer error - character mappings don't match");
            }
            return new CharacterMapper(fromChars.toString(), toChars.toString());
        }
    }
}
