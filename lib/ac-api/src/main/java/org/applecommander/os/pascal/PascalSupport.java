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

import org.applecommander.disassembler.api.Disassembler;
import org.applecommander.disassembler.api.Instruction;
import org.applecommander.disassembler.api.InstructionSet;
import org.applecommander.disassembler.api.mos6502.InstructionSet6502;
import org.applecommander.disassembler.api.pcode.InstructionSetPCode;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class PascalSupport {
    private PascalSupport() {
        // prevent construction
    }

    public static String textFile(ByteBuffer textAddrBuf) {
        StringBuilder sb = new StringBuilder();
        while (textAddrBuf.hasRemaining()) {
            var ch = Byte.toUnsignedInt(textAddrBuf.get());
            if (ch == 0) {
                // 0's seem to be the end?
                break;
            }
            else if (ch == 16) {
                // DLE
                var n = Byte.toUnsignedInt(textAddrBuf.get()) - 32;
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

    public static void disassemble(PrintWriter pw, CodeFile codeFile) {
        // Reset components that are implied in the CodeFile itself:
        if (codeFile.comment() != null && !codeFile.comment().isEmpty()) {
            pw.printf("Comment:  %s\n", codeFile.comment());
        }
        for (Segment segment : codeFile.segments()) {
            if (segment != null) disassemble(pw, segment);
        }
    }

    public static void disassemble(PrintWriter pw, Segment segment) {
        pw.printf(">> Seg #%02d: FROM=$%04x, TO=$%04x, N='%s', %-10s, T=$%04x, M=%-10s, Ver=%d\n",
                segment.segNum(), segment.data().position(), segment.data().limit(), segment.name(),
                segment.kind(), segment.textAddr(), segment.machineType(), segment.version());
        if (segment.textInterface() != null && !segment.textInterface().isEmpty()) {
            pw.println(">  Interface text:");
            pw.println(segment.textInterface().indent(5));
        }
        for (var proc : segment.dictionary()) {
            if (proc == null) {
                pw.println(">  Invalid procedure header.");
                continue;
            }
            // We want to indent the resulting assembly, so a temporary new PrintWriter so indentation can be applied
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter, true);
            switch (proc) {
                case PCodeProcedure pcode -> disassemble(printWriter, pcode);
                case AssemblyProcedure asm -> disassemble(printWriter, asm);
                default -> throw new RuntimeException("Unexpected procedure type: " + proc.getClass().getName());
            }
            pw.println(stringWriter.toString().indent(5));
        }
    }

    public static void disassemble(PrintWriter pw, PCodeProcedure pcode) {
        pw.printf(">  Proc#%d, Lex Lvl %d, Enter $%04x, Exit $%04x, Param %d, Data %d, JTAB=$%04x\n",
                pcode.procNum(), pcode.lexLevel(), pcode.enterIC(), pcode.exitIC(),
                pcode.paramsSize(), pcode.dataSize(), pcode.jumpTable());
        disassemble(pw, InstructionSetPCode.forApplePascal(), pcode.enterIC(), pcode.codeBytes());
    }

    public static void disassemble(PrintWriter pw, AssemblyProcedure asm) {
        pw.printf(">  ASM Proc, Relocation Segment #%d, Enter $%04x\n",
                asm.relocSegNum(), asm.enterIC());

        BiConsumer<int[], String> formatter = (table, name) -> {
            if (table.length > 0) {
                pw.printf("\t%s-relative relocation table: ", name);
                for (int addr : table) pw.printf("$%04X ", addr);
                pw.println();
            }
        };
        formatter.accept(asm.baseRelativeReloc(), "base");
        formatter.accept(asm.segRelativeReloc(), "segment");
        formatter.accept(asm.procRelativeReloc(), "procedure");
        formatter.accept(asm.interpRelativeReloc(), "interpreter");

        var bb = ByteBuffer.wrap(asm.codeBytes());
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (int addr : asm.procRelativeReloc()) {
            int offset = addr - asm.enterIC();
            bb.putShort(offset, (short) (bb.getShort(offset) + asm.endIC()));
        }

        disassemble(pw, InstructionSet6502.for6502(), asm.enterIC(), bb.array());
    }

    public static void disassemble(PrintWriter pw, InstructionSet instructionSet, int startAddress, byte[] procedureCode) {
        Map<Integer,String> labels = new HashMap<>();
        List<Instruction> assembly = Disassembler.with(procedureCode)
                .startingAddress(startAddress)
                .use(instructionSet)
                .decode(labels);

        final int bytesPerLine = instructionSet.defaults().bytesPerInstruction();
        assembly.forEach(instruction -> {
            pw.printf("%04X- ", instruction.address());

            byte[] instructionCode = instruction.code();
            for (int i=0; i<bytesPerLine; i++) {
                if (i >= instructionCode.length) {
                    pw.print("   ");
                } else {
                    pw.printf("%02X ", instructionCode[i]);
                }
            }
            pw.printf(" %-10.10s ", labels.getOrDefault(instruction.address(), ""));
            pw.printf("%-5s ", instruction.mnemonic());
            pw.printf("%-30s ", instruction.operands().stream().map(operand -> {
                        if (operand.address().isPresent() && labels.containsKey(operand.address().get())) {
                            return operand.format(labels.get(operand.address().get()));
                        }
                        else {
                            return operand.format();
                        }
                    })
                    .collect(Collectors.joining(",")));
            if (instructionSet.defaults().includeDescription()) {
                instruction.description().ifPresent(description -> {
                    pw.printf("; %s", description);
                });
            }
            pw.println();

            if (instructionCode.length > bytesPerLine) {
                for (int i=bytesPerLine; i<instructionCode.length; i++) {
                    if (i % bytesPerLine == 0) {
                        if (i > bytesPerLine) pw.println();
                        pw.printf("%04X- ", instruction.address()+i);
                    }
                    pw.printf("%02X ", instructionCode[i]);
                }
                pw.println();
            }
        });
    }
}
