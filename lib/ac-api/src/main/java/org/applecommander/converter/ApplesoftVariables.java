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
import java.util.Objects;

/// Exposes Applesoft variables that are stored in a VAR file.
///
/// From "Beneath Apple ProDOS 2020", page 205:
///
/// | Offset | Length | Description |
/// | :--- | :--- | :--- |
/// | +0 | 2 bytes | Combined length of simple and array variables. |
/// | +2 | 2 bytes | Length of simple variables only. |
/// | +4 | 1 byte | MSB of HIMEM when these variables were stored. |
/// | +5 | - | Start of memory image. |
///
/// The rest of the file follows typical Applesoft variable structures.
public class ApplesoftVariables {
    private static final int HEADER_SIZE = 5;
    private final DataBuffer data;
    private final int combinedVarLength;
    private final int varLength;
    private final int himemAddr;

    public ApplesoftVariables(FileEntry fileEntry) {
        this(fileEntry.getDataFork());
    }
    public ApplesoftVariables(DataBuffer data) {
        this.data = data;
        this.combinedVarLength = data.getUnsignedShort(0);
        this.varLength = data.getUnsignedShort(2);
        this.himemAddr = data.getUnsignedByte(4) << 8;
    }

    private VariableType identifyTypeAt(int offset) {
        int byte1 = data.getUnsignedByte(offset);
        int byte2 = data.getUnsignedByte(offset+1);
        for (VariableType type : VariableType.values()) {
            if (type.flag1 == (byte1 & 0x80) && type.flag2 == (byte2 & 0x80)) {
                return type;
            }
        }
        throw new UnsupportedOperationException();
    }
    private String getVarName(int i) {
        int byte1 = data.getUnsignedByte(i);
        int byte2 = data.getUnsignedByte(i+1);
        String name = Character.toString(byte1 & 0x7f);
        if ((byte2 & 0x7f) != 0) {
            name += Character.toString(byte2 & 0x7f);
        }
        VariableType type = identifyTypeAt(i);
        return name + type.postfix;
    }

    /// Get a list of all the simple variable names.
    public List<String> getSimpleNames() {
        List<String> names = new ArrayList<>();
        for (int i=HEADER_SIZE; i<varLength; i+= 7) {
            names.add(getVarName(i));
        }
        return names;
    }
    /// Find the location of a simple variable by its name.
    public int findSimpleVar(String varName) {
        for (int offset=HEADER_SIZE; offset<varLength; offset+= 7) {
            if (Objects.equals(varName, getVarName(offset))) {
                return offset;
            }
        }
        throw new UnsupportedOperationException();
    }
    /// Get the value of a simple variable by its name.
    public Object getSimpleValue(String varName) {
        return getValueAt(findSimpleVar(varName));
    }

    private Object getValueAt(int index) {
        return switch (identifyTypeAt(index)) {
            case REAL -> data.getApplesoftReal(index+2);
            case INTEGER -> data.getUnsignedShortBE(index+2);
            case STRING -> {
                int length = data.getUnsignedByte(index+2);
                int address = data.getUnsignedShort(index+3);
                int offset = data.limit() - (himemAddr - address);
                yield data.getFixedLengthString(offset, length);
            }
            case FUNCTION -> "N/A";
        };
    }

    /// Get a list of all array names.
    public List<String> getArrayNames() {
        List<String> names = new ArrayList<>();
        for (int i=HEADER_SIZE+varLength; i<combinedVarLength; i+= data.getUnsignedShort(i+2)) {
            names.add(getVarName(i));
        }
        return names;
    }
    /// Find the location of an array variably by its name.
    public int findArrayVar(String varName) {
        for (int offset=HEADER_SIZE+varLength; offset<combinedVarLength; offset+= data.getUnsignedShort(offset+2)) {
            if (Objects.equals(varName, getVarName(offset))) {
                return offset;
            }
        }
        throw new UnsupportedOperationException();
    }
    /// Get the dimensions of an array variable.
    public List<Integer> getArrayDimensions(String varName) {
        int index = findArrayVar(varName);
        List<Integer> dims = new ArrayList<>();
        int count = data.getUnsignedByte(index+4);
        int offset = index+5;
        while (dims.size() < count) {
            int dim = data.getUnsignedShortBE(offset);
            // Applesoft includes the 0th dimension, but people won't expect to _see_ that.
            dims.addFirst(dim-1);
            offset+= 2;
        }
        return dims;
    }

    /// Information on the variable types. Used for decoding and setting up the name.
    public enum VariableType {
        REAL(0x00, 0x00, "", 5),
        INTEGER(0x80, 0x80, "%", 2),
        STRING(0x00, 0x80, "$", 2),
        FUNCTION(0x80, 0x00, "()", 0);

        public final int flag1;
        public final int flag2;
        public final String postfix;
        public final int bytes;

        VariableType(int flag1, int flag2, String postfix, int bytes) {
            this.flag1 = flag1;
            this.flag2 = flag2;
            this.postfix = postfix;
            this.bytes = bytes;
        }
    }
}
