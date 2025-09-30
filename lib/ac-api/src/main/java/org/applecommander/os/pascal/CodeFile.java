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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record CodeFile(Segment[] segments, String comment) {
    public static final int DISKINFO_LENGTH = 4;
    public static final int SEGNAME_LENGTH = 8;
    public static final int SEGKIND_LENGTH = 2;
    public static final int TEXTADDR_LENGTH = 2;
    public static final int SEGINFO_LENGTH = 2;
    public static final int INTRINS_SEGS_LENGTH = 4;

    public static CodeFile load(Path path) throws IOException {
        return load(Files.readAllBytes(path));
    }
    public static CodeFile load(byte[] data) {
        var diskInfoBuf = DataBuffer.wrap(data, 0, 16*DISKINFO_LENGTH);
        var segNameBuf = DataBuffer.wrap(data, diskInfoBuf.limit(), 16*SEGNAME_LENGTH);
        var segKindBuf = DataBuffer.wrap(data, segNameBuf.limit(), 16*SEGKIND_LENGTH);
        var textAddrBuf = DataBuffer.wrap(data, segKindBuf.limit(), 16*TEXTADDR_LENGTH);
        var segInfoBuf = DataBuffer.wrap(data, textAddrBuf.limit(), 16*SEGINFO_LENGTH);
        var intrinsSegsBuf = DataBuffer.wrap(data, segInfoBuf.limit(), 32*INTRINS_SEGS_LENGTH);   // libraries
        var commentBuf = DataBuffer.wrap(data, intrinsSegsBuf.limit(), 0x50);

        int commentLength = commentBuf.readUnsignedByte();
        var commentData = new byte[commentLength];
        commentBuf.read(commentData);
        String comment = new String(commentData);

        Segment[] segments = new Segment[16];
        for (var i=0; i<16; i++) {
            var blockAddress = diskInfoBuf.readUnsignedShort();
            var lengthInBytes = diskInfoBuf.readUnsignedShort();
            if (blockAddress == 0 && lengthInBytes == 0) continue;  // unused slot
            var name = new byte[8];
            segNameBuf.read(name);
            var start = blockAddress * 512;
            var textAddr = textAddrBuf.readUnsignedShort() * 512;
            DataBuffer textInterfaceBuf = DataBuffer.wrap(new byte[0]);
            if (textAddr > 0) {
                textInterfaceBuf = DataBuffer.wrap(data, textAddr, start-textAddr);
            }
            if (lengthInBytes > 0) {
                segments[i] = Segment.load(new String(name), segKindBuf.readUnsignedShort(), segInfoBuf.readUnsignedShort(),
                    DataBuffer.wrap(data, start, lengthInBytes), textInterfaceBuf);
            }
        }
        return new CodeFile(segments, comment);
    }

    /** Perform a light test of these bytes to verify they "look" like an Apple Pascal CodeFile. */
    public static boolean test(byte[] data) {
        var diskInfoBuf = DataBuffer.wrap(data, 0, 16*DISKINFO_LENGTH);
        var segNameBuf = DataBuffer.wrap(data, diskInfoBuf.limit(), 16*SEGNAME_LENGTH);

        for (int i=0; i<16; i++) {
            // Check DISKINFO for validity
            var blockAddress = diskInfoBuf.readUnsignedShort();
            var lengthInBytes = diskInfoBuf.readUnsignedShort();
            if (blockAddress > 0 && lengthInBytes > 0) {
                // Only check valid slots
                var start = blockAddress * 512;
                if (start + lengthInBytes >= data.length) return false;
            }
            // Check SEGNAME for legal Pascal identifier
            for (int j=0; j<8; j++) {
                int ch = segNameBuf.readUnsignedByte();
                if (ch != '_' && ch != ' ' && !Character.isUpperCase(ch) && !Character.isDigit(ch)) return false;
            }
        }
        return true;
    }
}
