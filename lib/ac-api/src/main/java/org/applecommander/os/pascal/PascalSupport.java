/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2025 by Robert Greene and others
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
package org.applecommander.os.pascal;

import org.applecommander.util.DataBuffer;

/**
 * Provides common Pascal support code.
 */
public class PascalSupport {
    private PascalSupport() {
        // prevent construction
    }

    public static String textFile(DataBuffer textAddrBuf) {
        StringBuilder sb = new StringBuilder();
        while (textAddrBuf.hasRemaining()) {
            var ch = textAddrBuf.readUnsignedByte();
            if (ch == 0) {
                // 0's seem to be the end?
                break;
            }
            else if (ch == 16) {
                // DLE
                var n = textAddrBuf.readUnsignedByte() - 32;
                while (n-- > 0) sb.append(" ");
            }
            else if (ch == 13) {
                // CR
                sb.append('\n');
            }
            else {
                sb.append((char)(ch&0x7f));
            }
        }
        return sb.toString();
    }
}
