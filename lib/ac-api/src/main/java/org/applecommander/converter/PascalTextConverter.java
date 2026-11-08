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

import java.io.*;

/// Filter the given file as a Pascal `.text` file.
///
/// From the Apple Pascal v1.2 Operating System Reference manual:
/// **Text files in Apple Pascal**.
///
/// > At the beginning of each textfile is a 1024-byte (two blocks on
/// diskette) header page, which contains information for the text editor.
/// This space is reserved for use by the text editor, and is respected
/// by all portions of the system. When a user program opens a text file,
/// and REWRITEs or RESETs it with a title ending in `.TEXT`, the I/O
/// subsystem will create and skip over the initial header page. This is
/// done to facilitate users editing their input and/or output data. The
/// file-handler will transfer the header page only on a disk-to-disk
/// transfer, and will omit it on a transfer to a serial device (thus
/// transfers to PRINTER: and CONSOLE: will omit the header page).
///
/// > Following the initial header page, the text itself appears in
/// subsequent 1024-byte text pages (two blocks each, on diskette),
/// where a text page is defined:
///
///   ```
///   [DLE] [indent] [text] [cr] [dle] [indent] [text] [CR] .. [nulls]
///   ```
///
/// > DLE's (Data Link Escapes) are followed by an indent-code, which is a
/// byte containing the value `32+(number to indent)`. The nulls at the
/// end of the page follow a `CR` in all cases, and are padded to the end
/// of a 1024-byte page (because the compiler wants integral numbers of
/// lines on a page). The Data Link Escape and corresponding indentation
/// code are optional. In a given text file, some lines will have the
/// codes, and some won't.
///
/// (Thanks to Hans Otten for sending in this information.)
public class PascalTextConverter implements TextConverter {
    public static final char NUL = 0x00;
    public static final char CR = 0x0d;
    public static final char DLE = 0x10;
    public static final int HEADER_SIZE = 1024;
    public static final int INDENT_BASE_VALUE = 0x20;

    private int spaceEncodingThreshold = 3;

    /// Get the current threshold for encoding spaces. Defaults to 3 but Pascal actually appears to use 0.
    public int getSpaceEncodingThreshold() {
        return spaceEncodingThreshold;
    }
    /// Set the threshold for encoding spaces.
    public void setSpaceEncodingThreshold(int spaceEncodingThreshold) {
        this.spaceEncodingThreshold = spaceEncodingThreshold;
    }

    /// Convert the file entry's data to text.
    @Override
    public String read(FileEntry fileEntry) {
        DataBuffer data = fileEntry.getDataFork();
        data.readBuffer(HEADER_SIZE);   // Skip the header section
        return toText(data);
    }

    /// Convert the given text to the text data format and write to the file entry.
    @Override
    public void write(WritableFileEntry fileEntry, String text) {
        // Grab the file's data and either preserve the existing header or just use 1024 $00 bytes.
        DataBuffer dataFork = fileEntry.getDataFork();
        DataBuffer header = DataBuffer.wrap(1024);
        if (dataFork.limit() > 1024) {
            header = dataFork.slice(0, HEADER_SIZE);
        }
        fileEntry.setDataFork(DataBuffer.concat(header, toData(text)));
    }

    /// Convert the data to text.
    public String toText(DataBuffer data) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        while (data.hasRemaining()) {
            char c = (char)(data.readUnsignedByte() & 0x7f);
            switch (c) {
                case NUL -> { /* ignore */ }
                case CR -> pw.println();
                case DLE -> {
                    if (data.hasRemaining()) {
                        int indent = data.readUnsignedByte() - INDENT_BASE_VALUE;
                        while (indent-- > 0) {
                            pw.print(' ');
                        }
                    }
                }
                default -> pw.print(c);
            }
        }
        return sw.toString();
    }

    /// Convert the given text to the text data format.
    public DataBuffer toData(String text) {
        // Track the content
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        // Track each 1024-byte page of data
        ByteArrayOutputStream page = new ByteArrayOutputStream();
        text.lines().forEach(line -> {
            try {
                ByteArrayOutputStream data = new ByteArrayOutputStream();
                // Figure out how many spaces lead this line
                int count = 0;
                for (char ch : line.toCharArray()) {
                    if (ch != ' ') break;
                    count++;
                }
                // If we have spaces, write the DLE <indent code>
                if (count >= spaceEncodingThreshold) {
                    count = Math.min(255 - INDENT_BASE_VALUE, count);
                    data.write(DLE);
                    data.write(INDENT_BASE_VALUE + count);
                    line = line.substring(count);
                }
                // Copy the characters over.
                for (char ch : line.toCharArray()) {
                    data.write(ch);
                }
                data.write(CR);
                // Figure out if it fits into the current page and flush it out if it doesn't
                if (page.size() + data.size() > 1024) {
                    if (page.size() < 1024) {
                        page.write(new byte[1024 - page.size()]);
                    }
                    page.writeTo(content);
                    page.reset();
                }
                data.writeTo(page);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
        // Flush out the last page of data, if there is any.
        if (page.size() > 0) {
            try {
                if (page.size() < 1024) {
                    page.write(new byte[1024 - page.size()]);
                }
                page.writeTo(content);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
        return DataBuffer.wrap(content.toByteArray());
    }
}
