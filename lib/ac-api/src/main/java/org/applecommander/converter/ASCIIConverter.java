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

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

/// Handle conversion of ASCII data.
public class ASCIIConverter implements TextConverter {
    private final boolean highBitOn;

    public ASCIIConverter(boolean highBitOn) {
        this.highBitOn = highBitOn;
    }

    /// Convert the file entry's data to text.
    @Override
    public String read(FileEntry fileEntry) {
        DataBuffer data = fileEntry.getDataFork();
        return toText(data);
    }

    /// Convert the given text to the text data format and write to the file entry.
    @Override
    public void write(WritableFileEntry fileEntry, String text) {
        fileEntry.setDataFork(toData(text));
    }

    /// Convert the data to text.
    public String toText(DataBuffer data) {
        StringWriter sw = new StringWriter();
        data.position(0);
        while (data.hasRemaining()) {
            int ch = data.readUnsignedByte() & 0x7f;
            if (ch != 0) {
                if (ch == 0x0d) {
                    sw.append("\n");
                } else {
                    sw.append((char)ch);
                }
            }
        }
        return sw.toString();
    }

    /// Convert the given text to the text data format.
    public DataBuffer toData(String text) {
        final int mask = highBitOn ? 0x80 : 0x00;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (int ch : text.toCharArray()) {
            if (ch == '\n') {
                output.write(0x0d | mask);
            } else {
                output.write(ch | mask);
            }
        }
        return DataBuffer.wrap(output.toByteArray());
    }
}
