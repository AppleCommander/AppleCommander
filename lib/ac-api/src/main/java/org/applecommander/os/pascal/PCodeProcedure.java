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

public record PCodeProcedure(int procNum, int lexLevel, int enterIC, int exitIC, int paramsSize,
                             int dataSize, int jumpTable, DataBuffer data) {

    public byte[] codeBytes() {
        // Sometimes we get a negative value; bypass it...
        int size = jumpTable()-enterIC();
        if (size <= 0 || jumpTable() < 0 || enterIC() < 0) return new byte[0];

        byte[] bytes = new byte[size];
        data.get(enterIC(), bytes);
        return bytes;
    }
    public static PCodeProcedure load(DataBuffer data, int attrs) {
        int lexLevel = data.getUnsignedByte(attrs);
        int procNum = data.getUnsignedByte(attrs-1);
        // SELF RELATIVE POINTERS contains the _distance_ between low-order byte of pointer
        int enterIC = attrs - data.getUnsignedShort(attrs-3) - 3;
        int exitIC = attrs - data.getUnsignedShort(attrs-5) - 5;
        int paramsSize = data.getUnsignedShort(attrs-7);
        int dataSize = data.getUnsignedShort(attrs-9);
        int jumpTable = attrs - 9;
        return new PCodeProcedure(procNum, lexLevel, enterIC, exitIC, paramsSize, dataSize, jumpTable, data);
    }
}
