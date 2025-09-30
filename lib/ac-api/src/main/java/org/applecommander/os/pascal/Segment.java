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
import java.nio.ByteOrder;

public record Segment(String name, String textInterface, ByteBuffer data, Kind kind, int textAddr,
                      int segNum, MachineType machineType, int version, Object[] dictionary) {

    public static Segment load(String name, int kind, int segInfo, ByteBuffer data, ByteBuffer textAddrBuf) {
        data.order(ByteOrder.LITTLE_ENDIAN);
        var segKind = Kind.values()[kind];
        var textAddr = textAddrBuf.limit();
        var segNum = segInfo & 0x00ff;
        var machineType = MachineType.values()[(segInfo >> 8) & 0xf];
        var version = (segInfo >> 13) & 0x07;

        var pos = data.limit();
        int numProc = data.get(--pos);
        int segNo = data.get(--pos);
        Object[] dictionary = new Object[numProc];

        var textInterface = PascalSupport.textFile(textAddrBuf);
        final var interfaceKW = "IMPLEMENTATION";
        if (textInterface.contains(interfaceKW)) {
            textInterface = textInterface.substring(0, textInterface.indexOf(interfaceKW)+interfaceKW.length());
        }

        for (var i=0; i<numProc; i++) {
            pos -= 2;
            var offset = data.getShort(pos);
            var attrs = pos - offset + 1;
            // Note that SYSTEM.PASCAL procedure #30 has offset of $4D76 from position $E16 (table begins at $E56)
            // ... which results to an invalid reference. Unable to find anything in manuals yet.
            if (attrs > 0) {
                dictionary[i] = switch (machineType) {
                    case P_CODE_LSB -> PCodeProcedure.load(data, attrs);
                    case MOS6502 -> {
                        // Not everything in an assembly segment is actually assembly
                        if (data.get(attrs-1) > 0) {
                            yield PCodeProcedure.load(data, attrs);
                        }
                        else {
                            yield AssemblyProcedure.load(data, attrs, i+1);
                        }
                    }
                    default -> {
                        System.err.printf("*** WARNING: Unknown machine type: %s - assuming p-code\n", machineType);
                        yield PCodeProcedure.load(data, attrs);
                    }
                };
            }
        }
        return new Segment(name, textInterface, data, segKind, textAddr, segNum, machineType, version, dictionary);
    }

    public enum Kind {
        LINKED, HOSTSEG, SEGPROC, UNITSEG, SEPRTSEG, UNLINKED_INSTRINS, LINKED_INTRINS, DATASEG
    }

    public enum MachineType {
        UNIDENTIFIED, P_CODE_MSB, P_CODE_LSB, PDP11, I8080, Z80, FA440, MOS6502, M6800, TI9900
    }
}
