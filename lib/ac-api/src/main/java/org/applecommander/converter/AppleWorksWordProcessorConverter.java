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
                                sb.append((char) ch);
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
                case 0xd5 -> handler.control(Control.END_PAGE_HEADER);
                case 0xd6 -> handler.control(Control.END_PAGE_FOOTER);
                case 0xd7 -> handler.control(Control.ALIGN_RIGHT);
                case 0xde -> handler.text(Strings.repeat(" ", byte0));
                case 0xdf -> handler.control(Control.ALIGN_JUSTIFY);
                case 0xe0 -> handler.control(Control.ALIGN_LEFT);
                case 0xe1 -> handler.control(Control.ALIGN_CENTER);
                case 0xe6 -> handler.control(Control.SPACING_SINGLE);
                case 0xe7 -> handler.control(Control.SPACING_DOUBLE);
                case 0xe8 -> handler.control(Control.SPACING_TRIPLE);
                case 0xe9 -> handler.control(Control.PAGE_BREAK);
                case 0xec -> handler.control(Control.BEGIN_PAGE_HEADER);
                case 0xed -> handler.control(Control.BEGIN_PAGE_FOOTER);
                case 0xef -> handler.control(Control.SET_PAGE_NUMBER, byte0);
                case 0xfe -> handler.control(Control.SET_PAGE_NUMBER, byte0 + 256);
                default -> {}
            }
        }
    }
}
