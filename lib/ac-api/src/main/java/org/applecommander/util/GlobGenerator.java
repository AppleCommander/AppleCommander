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

import java.util.Set;

/// Utility methods to support glob-style patterns with Apple file names.
///
/// Glob patterns supported:
///
///   - `*` = matches zero or more characters
///   - `?` = matches exactly one character
///   - `**` = match any number of directories
///   - `\x` = escape character
public class GlobGenerator {
    private static final Set<Character> REGEX_SPECIAL_CHARS = Set.of('?', '*', '^', '$');

    /// Transform glob pattern into a regex expression. This needs to take into effect the "valid"
    /// character set used in the filesystem as well as if directories are supported.
    public static String globToRegex(String glob, boolean supportsDirectories) {
        StringBuilder regex = new StringBuilder();
        regex.append("(?i)^");
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            char nextc = 0;
            if (i+1 < glob.length()) nextc = glob.charAt(i + 1);
            switch (c) {
                case '*' -> {
                    // '**' directory construct
                    if (nextc == '*') {
                        regex.append(".*");
                        i++;
                    }
                    // if we have directories (currently just ProDOS) exclude the separator '/'
                    else if (supportsDirectories) {
                        regex.append("[^/]*");
                    }
                    // otherwise we want everything
                    else {
                        regex.append(".*");
                    }
                }
                case '.' -> regex.append("\\.");
                case '?' -> regex.append(".");
                case '\\' -> {
                    if (nextc != 0) {
                        if (REGEX_SPECIAL_CHARS.contains(nextc)) regex.append("\\\\");
                        regex.append(nextc);
                        i++;
                    }
                }
                default -> regex.append(c);
            }
        }
        regex.append("$");
        return regex.toString();
    }
}
