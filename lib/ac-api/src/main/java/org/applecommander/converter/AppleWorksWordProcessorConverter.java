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

import com.google.common.base.Strings;
import org.applecommander.filestore.FileEntry;
import org.applecommander.util.DataBuffer;

/// Converts an AppleWorks Word Processor (AWP) document to a word processor event stream.
public class AppleWorksWordProcessorConverter implements WordProcessorConverter {
    private final DataBuffer data;

    public AppleWorksWordProcessorConverter(FileEntry fileEntry) {
        this.data = fileEntry.getDataFork();
    }

    /// Convert the document using the given event handler callback.
    @Override
    public void convert(EventHandler handler) {
        final int sfMinVers = data.getUnsignedByte(183);
        data.position(300);
        boolean first = true;
        while (data.hasRemaining()) {
            int byte0 = data.readUnsignedByte();
            int byte1 = data.readUnsignedByte();
            if (first && sfMinVers > 0) {
                first = false;
                continue;
            }
            first = false;
            if (byte0 == 0xff && byte1 == 0xff) {
                break;
            }
            switch (byte1) {
                case 0x00 -> {
                    DataBuffer record = data.readBuffer(byte0);
                    // These are adjusted since we already read bytes 0 and 1.
                    if (record.getUnsignedByte(0) != 0xff) {
                        int len = record.getUnsignedByte(1);
                        boolean addCR = len >= 0x80;
                        len &= 0x7f;
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < len; i++) {
                            int ch = record.getUnsignedByte(i + 2);
                            if (ch >= 0x20) {
                                // Trying to translate a few of the MouseText characters following
                                // https://www.kreativekorp.com/charset/encoding/x-apple2gs/
                                switch (ch) {
                                    case 0xc0 -> {
                                        sb.append(Character.toChars(0x1FBE9));
                                        sb.append(Character.toChars(0x1FBEB));
                                    }
                                    case 0xc1 -> {
                                        sb.append(Character.toChars(0x1FBE1));
                                        sb.append(Character.toChars(0x1FBE3));
                                    }
                                    case 0xc2 -> sb.append(Character.toChars(0x1fbb0));
                                    case 0xc3 -> {
                                        sb.append(Character.toChars(0x231B));
                                        sb.append(Character.toChars(0xFE0E));
                                    }
                                    case 0xc4 -> sb.append(Character.toChars(0x2713));
                                    case 0xc5 -> sb.append(Character.toChars(0x1FBB1));
                                    case 0xc6 -> sb.append(Character.toChars(0x1FBB4));
                                    case 0xc7 -> sb.append(Character.toChars(0x1FB81));
                                    case 0xc8 -> sb.append(Character.toChars(0x2190));
                                    case 0xc9 -> sb.append(Character.toChars(0x2026));
                                    case 0xca -> sb.append(Character.toChars(0x2193));
                                    case 0xcb -> sb.append(Character.toChars(0x2191));
                                    case 0xcc -> sb.append(Character.toChars(0x2594));
                                    case 0xcd -> sb.append(Character.toChars(0x21B2));
                                    case 0xce -> sb.append(Character.toChars(0x2589));
                                    case 0xcf -> sb.append(Character.toChars(0x1FBB5));
                                    case 0xd0 -> sb.append(Character.toChars(0x1FBB6));
                                    case 0xd1 -> sb.append(Character.toChars(0x1FBB7));
                                    case 0xd2 -> sb.append(Character.toChars(0x1FBB8));
                                    case 0xd3 -> sb.append(Character.toChars(0x2500));
                                    case 0xd4 -> sb.append(Character.toChars(0x1FB7C));
                                    case 0xd5 -> sb.append(Character.toChars(0x2192));
                                    case 0xd6 -> sb.append(Character.toChars(0x1FB95));
                                    case 0xd7 -> sb.append(Character.toChars(0x1FB96));
                                    case 0xd8 -> sb.append(Character.toChars(0x1FBB9));
                                    case 0xd9 -> sb.append(Character.toChars(0x1FBBA));
                                    case 0xda -> sb.append(Character.toChars(0x2595));
                                    case 0xdb -> sb.append(Character.toChars(0x25C6));
                                    case 0xdc -> sb.append(Character.toChars(0x1FB80));
                                    case 0xdd -> sb.append(Character.toChars(0x1FBBB));
                                    case 0xde -> sb.append(Character.toChars(0x1FBBC));
                                    case 0xdf -> sb.append(Character.toChars(0x258F));
                                    default -> sb.append((char) ch);
                                }
                            } else {
                                if (!sb.isEmpty()) {
                                    handler.text(sb.toString());
                                    sb.setLength(0);
                                }
                                switch (ch) {
                                    case 0x01 -> handler.control(Control.BEGIN_BOLD);
                                    case 0x02 -> handler.control(Control.END_BOLD);
                                    case 0x03 -> handler.control(Control.BEGIN_SUPERSCRIPT);
                                    case 0x04 -> handler.control(Control.END_SUPERSCRIPT);
                                    case 0x05 -> handler.control(Control.BEGIN_SUBSCRIPT);
                                    case 0x06 -> handler.control(Control.END_SUBSCRIPT);
                                    case 0x07 -> handler.control(Control.BEGIN_UNDERLINE);
                                    case 0x08 -> handler.control(Control.END_UNDERLINE);
                                    case 0x09 -> handler.control(Control.SHOW_PAGE_NUMBER);
                                    case 0x0e -> handler.control(Control.PRINT_DATE);
                                    case 0x0f -> handler.control(Control.PRINT_TIME);
                                }
                            }
                        }
                        if (addCR) sb.append("\n");
                        if (!sb.isEmpty()) {
                            handler.text(sb.toString());
                            sb.setLength(0);
                        }
                    }
                }
                case 0xd0 -> handler.text("\n");
                case 0xd5,0xd6 -> handler.section(DocumentSection.BODY);
                case 0xd7 -> handler.control(Control.ALIGN_RIGHT);
                case 0xde -> handler.text(Strings.repeat(" ", byte0));
                case 0xdf -> handler.control(Control.ALIGN_JUSTIFY);
                case 0xe0 -> handler.control(Control.ALIGN_LEFT);
                case 0xe1 -> handler.control(Control.ALIGN_CENTER);
                case 0xe6 -> handler.control(Control.SPACING_SINGLE);
                case 0xe7 -> handler.control(Control.SPACING_DOUBLE);
                case 0xe8 -> handler.control(Control.SPACING_TRIPLE);
                case 0xe9 -> handler.control(Control.PAGE_BREAK);
                case 0xec -> handler.section(DocumentSection.HEADER);
                case 0xed -> handler.section(DocumentSection.FOOTER);
                case 0xef -> handler.control(Control.SET_PAGE_NUMBER, byte0);
                case 0xfe -> handler.control(Control.SET_PAGE_NUMBER, byte0 + 256);
                default -> {}
            }
        }
    }
}
