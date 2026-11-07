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

import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.WritableFileEntry;
import org.applecommander.util.DataBuffer;

/// A text converter handles conversion of text formats between the native
/// representation and typical (modern) text files. This can include handling
/// high ASCII, Pascal text files, or a variety of source code formats.
/// Since text is simple, we convert directly between a `String` and a
/// `DataBuffer`.
///
/// The `TextConverter` interface is a little bit unique in that some of the
/// encodings are a bit different, depending on the context. Specifically,
/// Apple Pascal has a 1024-byte header for the text editors but not all usages
/// of the Apple Pascal TEXT format use that (the CODE has text which uses
/// this encoding but does not have the 1024-byte header). Thus, two sets of
/// interfaces to make working with files distinctive.
public interface TextConverter {
    /// Convert the file entry's data to text.
    String read(FileEntry fileEntry);
    /// Convert the given text to the text data format and write to the file entry.
    void write(WritableFileEntry fileEntry, String text);

    /// Convert the data to text.
    String toText(DataBuffer data);
    /// Convert the given text to the text data format.
    DataBuffer toData(String text);
}
