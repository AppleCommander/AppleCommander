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
import org.applecommander.util.DataBuffer;

import java.util.ArrayList;
import java.util.List;

public class AppleWorksGSWordProcessorConverter implements WordProcessorConverter {
    private final DataBuffer data;

    public AppleWorksGSWordProcessorConverter(FileEntry fileEntry) {
        this.data = fileEntry.getDataFork();
    }

    /// Convert the document using the given event handler callback.
    @Override
    public void convert(EventHandler handler) {
        // Note that this completely skips the header and footer
        data.position(0);
        data.readBuffer(282);   // Skip docHeader.
        final DataBuffer globals = data.readBuffer(386);
        final int startPage = globals.getUnsignedShort(52);
        handler.control(Control.SET_PAGE_NUMBER, startPage);
        final int docSACount = data.readUnsignedShort();
        System.out.printf("docSACount = $%04X\n", docSACount);
        int maxRulerNum = 0;
        List<SaveArray> saveArrays = new ArrayList<>();
        System.out.printf("docSaveArray = $%04X\n", data.position());
        while (saveArrays.size() < docSACount) {
            DataBuffer saData = data.readBuffer(12);
            int textBlock = saData.readUnsignedShort();
            int offset = saData.readUnsignedShort();
            int attributes = saData.readUnsignedShort();
            int rulerNum = saData.readUnsignedShort();
            saData.readUnsignedShort();
            int numLines = saData.readUnsignedShort();
            System.out.printf("SaveArray #%d: textBlock=%d, offset=$%04X, attributes=%d, rulerNum=%d, numLines=%d\n",
                    saveArrays.size(), textBlock, offset, attributes, rulerNum, numLines);
            saveArrays.add(new SaveArray(textBlock, offset, attributes, numLines));
            if (attributes == 0) {
                maxRulerNum = Math.max(maxRulerNum, rulerNum);
            }
        }
        maxRulerNum++;  // they're zero based
        System.out.printf("maxRulerNum = %d\n", maxRulerNum);
        List<Ruler> rulers = new ArrayList<>();
        System.out.printf("docRulers = $%04X\n", data.position());
        while (rulers.size() < maxRulerNum) {
            DataBuffer rulerData = data.readBuffer(52);
            int numParagraphs = rulerData.readUnsignedShort();
            int statusBits = rulerData.readUnsignedShort();
            int leftMargin = rulerData.readUnsignedShort();
            int indentMargin = rulerData.readUnsignedShort();
            int rightMargin = rulerData.readUnsignedShort();
            int numTabs = rulerData.readUnsignedShort();
            System.out.printf("Ruler #%d: numParagraphs=%d, statusBits=$%04X, leftMargin=%d, indentMargin=%d, rightMargin=%d, numTabs=%d\n",
                    rulers.size(), numParagraphs, statusBits, leftMargin, indentMargin, rightMargin, numTabs);
            rulers.add(new Ruler(numParagraphs, statusBits));
        }
        System.out.printf("docTextBlocks = $%04X\n", data.position());
        int textBlockRecord = data.readInt();    // the Text Block Record is redundant per documentation!
        System.out.printf("Text Block Record = $%08X\n", textBlockRecord);
        System.out.printf("Text Block @ $%04X\n", data.position());
        int blockSize = data.readUnsignedShort();
        data.readUnsignedShort();   // skipping blockUsed
        DataBuffer textBlock = data.readBuffer(blockSize - 4);
        int textBlockCount = 0;
        Ruler ruler = rulers.get(textBlockCount);
        handler.control(ruler.getJustification());
        handler.control(ruler.getSpacing());
        while (textBlock.hasRemaining()) {
            System.out.printf("Reading Text Block @ $%04X\n", textBlock.position());
            DataBuffer header = textBlock.readBuffer(7);
            int firstFont = header.readUnsignedShort();
            if (firstFont != 0) System.out.printf("First font = $%04X (%d)\n", firstFont, firstFont);
            StyleByte firstStyle = new StyleByte(header.readUnsignedByte());
            firstStyle.getStyles().forEach(handler::control);
            int firstSize = header.readUnsignedByte();
            if (firstSize != 0) {
                System.out.printf("firstSize=%d\n", firstSize);
                handler.control(Control.FONT_SIZE, firstSize);
            }
            StringBuilder sb = new StringBuilder();
            while (textBlock.peekUnsignedByte() != 0x0d) {
                int ch = textBlock.readUnsignedByte();
                if (ch >= 0x01 && ch <= 0x07) {
                    if (!sb.isEmpty()) {
                        handler.text(sb.toString());
                        sb.setLength(0);
                    }
                    switch (ch) {
                        // Font change (ignored)
                        case 0x01 -> textBlock.readUnsignedShort();
                        // Style change
                        case 0x02 -> {
                            StyleByte style = new StyleByte(textBlock.readUnsignedByte());
                            style.getStyles().forEach(handler::control);
                        }
                        // Size change
                        case 0x03 -> {
                            int fontSize = textBlock.readUnsignedByte();
                            if (fontSize != 0) handler.control(Control.FONT_SIZE, fontSize);
                        }
                        // Color change (ignored)
                        case 0x04 -> textBlock.readUnsignedByte();
                        // Page token
                        case 0x05 -> handler.control(Control.SHOW_PAGE_NUMBER);
                        // Date token
                        case 0x06 -> handler.control(Control.PRINT_DATE);
                        // Time token
                        case 0x07 -> handler.control(Control.PRINT_TIME);
                    }
                }
                else if (ch == 0x09) {
                    sb.append("\t");
                }
                else {
                    sb.append(Character.toString(ch));
                }
            }
            textBlock.readUnsignedByte();   // consume the $0D
            sb.append("\n");
            handler.text(sb.toString());
        }
    }

    public record SaveArray(int textBlock, int offset, int attributes, int numLines) {}
    public record Ruler(int numParagraphs, int statusBits) {
        public Control getJustification() {
            return switch (statusBits & 0xf0) {
                case 0x80 -> Control.ALIGN_JUSTIFY;
                case 0x40 -> Control.ALIGN_RIGHT;
                case 0x20 -> Control.ALIGN_CENTER;
                default -> Control.ALIGN_LEFT;
            };
        }
        public Control getSpacing() {
            return switch (statusBits & 0x07) {
                case 0x04 -> Control.SPACING_TRIPLE;
                case 0x02 -> Control.SPACING_DOUBLE;
                default -> Control.SPACING_SINGLE;
            };
        }
    }
    public record StyleByte(int styleByte) {
        public List<Control> getStyles() {
            List<Control> styles = new ArrayList<>();
            styles.add(toggle(0x80, Control.BEGIN_SUBSCRIPT, Control.END_SUBSCRIPT));
            styles.add(toggle(0x40, Control.BEGIN_SUPERSCRIPT, Control.END_SUPERSCRIPT));
            styles.add(toggle(0x04, Control.BEGIN_UNDERLINE, Control.END_UNDERLINE));
            styles.add(toggle(0x02, Control.BEGIN_ITALIC, Control.END_ITALIC));
            styles.add(toggle(0x01, Control.BEGIN_BOLD, Control.END_BOLD));
            return styles;
        }
        private Control toggle(int flag, Control on, Control off) {
            return (styleByte & flag) != 0 ? on : off;
        }
    }
}
