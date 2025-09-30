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

import java.nio.ByteBuffer;

public record AssemblyProcedure(ByteBuffer data, int procNum, int relocSegNum, int enterIC, int attrs, int endIC,
                                int[] baseRelativeReloc, int[] segRelativeReloc, int[] procRelativeReloc,
                                int[] interpRelativeReloc) {

    public byte[] codeBytes() {
        byte[] bytes = new byte[endIC - enterIC];
        data.get(enterIC, bytes);
        return bytes;
    }

    public static AssemblyProcedure load(ByteBuffer data, int attrs) {
        int relocSegNum = data.get(attrs);
        int procNum = data.get(attrs-1);
        assert procNum == 0;
        int enterIC = attrs - data.getShort(attrs-3) - 3;
        // SELF RELATIVE POINTERS contains the _distance_ between low-order byte of pointer
        // A little bit stinky because Java doesn't allow multiple returns.
        // which would allow '(attrs,baseRelativeReloc) = selfRelativeTable(attrs)'
        var pair = loadSelfRelativeTable(data, attrs-3);
        int[] baseRelativeReloc = pair.table();
        pair = loadSelfRelativeTable(data, pair.attrs());
        int[] segRelativeReloc = pair.table();
        pair = loadSelfRelativeTable(data, pair.attrs());
        int[] procRelativeReloc = pair.table();
        pair = loadSelfRelativeTable(data, pair.attrs());
        int[] interpRelativeReloc = pair.table();
        int endIC = pair.attrs();
        return new AssemblyProcedure(data, procNum, relocSegNum, enterIC, attrs, endIC,
                baseRelativeReloc, segRelativeReloc, procRelativeReloc, interpRelativeReloc);
    }

    private static Result<Integer,int[]> loadSelfRelativeTable(ByteBuffer data, int base) {
        base -= 2;
        int n = data.getShort(base);
        int[] table = new int[n];
        for (int i=0; i<n; i++) {
            base -= 2;
            table[i] = base - data.getShort(base);
        }
        return new Result<>(base, table);
    }

    record Result<I,J>(I attrs, J table) {}
}
