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

import org.applecommander.filestore.ContentType;
import org.applecommander.filestore.FileEntry;

import java.util.Optional;

/// The `ConverterRepository` is used to translate `ContentType` to specific types of converters.
public class ConverterRepository {
    /// Identify the appropriate type of `TextConverter` for the given `ContentType`.
    public static Optional<TextConverter> getTextConverter(ContentType contentType) {
        return switch (contentType) {
            case HIGH_ASCII -> Optional.of(new ASCIIConverter(true));
            case LOW_ASCII -> Optional.of(new ASCIIConverter(false));
            case PASCAL_TEXT -> Optional.of(new PascalTextConverter());
            default -> Optional.empty();
        };
    }

    /// Get the variable file converter.
    public static Optional<ApplesoftVariables> getVariables(FileEntry fileEntry) {
        return switch (fileEntry.getContentType()) {
            case APPLESOFT_VARIABLES -> Optional.of(new ApplesoftVariables(fileEntry));
            default -> Optional.empty();
        };
    }
}
