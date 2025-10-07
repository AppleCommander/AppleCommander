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
import org.applecommander.util.DataBuffer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Provide standardized list mechanism for Apple Pascal CODEFILE.
 * This allows minimal configuration in the disassemble function for pcode and 6502.
 */
public class CodeFileLister {
    private final DisassemblyFunc disassemblyFunc;

    public CodeFileLister() {
        this.disassemblyFunc = this::disassemble;
    }

    public CodeFileLister(DisassemblyFunc disassemblyFunc) {
        this.disassemblyFunc = disassemblyFunc;
    }

    public void list(PrintWriter pw, CodeFile codeFile) {
        // Reset components that are implied in the CodeFile itself:
        if (codeFile.comment() != null && !codeFile.comment().isEmpty()) {
            pw.printf("Comment:  %s\n", codeFile.comment());
        }
        for (Segment segment : codeFile.segments()) {
            if (segment != null) list(pw, segment);
        }
    }

    public void list(PrintWriter pw, Segment segment) {
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
            switch (proc) {
                case PCodeProcedure pcode -> list(pw, pcode);
                case AssemblyProcedure asm -> list(pw, asm);
                default -> throw new RuntimeException("Unexpected procedure type: " + proc.getClass().getName());
            }
        }
    }

    public void list(PrintWriter pw, PCodeProcedure pcode) {
        pw.printf(">  Proc#%d, Lex Lvl %d, Enter $%04x, Exit $%04x, Param %d, Data %d, JTAB=$%04x\n",
                pcode.procNum(), pcode.lexLevel(), pcode.enterIC(), pcode.exitIC(),
                pcode.paramsSize(), pcode.dataSize(), pcode.jumpTable());

        // We want to indent the resulting assembly, so a temporary new PrintWriter so indentation can be applied
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter, true);
        disassemblyFunc.disassemble(printWriter, InstructionSetPCode.forApplePascal(), pcode.enterIC(), pcode.codeBytes());
        pw.println(stringWriter.toString().indent(5));
    }

    public void list(PrintWriter pw, AssemblyProcedure asm) {
        pw.printf(">  ASM Proc#%d, Relocation Segment #%d, Enter $%04x\n",
                asm.procNum(), asm.relocSegNum(), asm.enterIC());

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

        var db = DataBuffer.wrap(asm.codeBytes());
        for (int addr : asm.procRelativeReloc()) {
            int offset = addr - asm.enterIC();
            db.putUnsignedShort(offset, db.getUnsignedShort(offset) + asm.enterIC());
        }

        // We want to indent the resulting assembly, so a temporary new PrintWriter so indentation can be applied
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter, true);
        disassemblyFunc.disassemble(printWriter, InstructionSet6502.for6502(), asm.enterIC(), db.asBytes());
        pw.println(stringWriter.toString().indent(5));
    }

    public interface DisassemblyFunc {
        void disassemble(PrintWriter pw, InstructionSet instructionSet, int startAddress, byte[] procedureCode);
    }

    public void disassemble(PrintWriter pw, InstructionSet instructionSet, int startAddress, byte[] procedureCode) {
        Map<Integer, String> labels = new HashMap<>();
        List<Instruction> assembly = Disassembler.with(procedureCode)
                .startingAddress(startAddress)
                .use(instructionSet)
                .decode(labels);

        final int bytesPerLine = instructionSet.defaults().bytesPerInstruction();
        assembly.forEach(instruction -> {
            pw.printf("%04X- ", instruction.address());

            byte[] instructionCode = instruction.code();
            for (int i = 0; i < bytesPerLine; i++) {
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
                        } else {
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
                for (int i = bytesPerLine; i < instructionCode.length; i++) {
                    if (i % bytesPerLine == 0) {
                        if (i > bytesPerLine) pw.println();
                        pw.printf("%04X- ", instruction.address() + i);
                    }
                    pw.printf("%02X ", instructionCode[i]);
                }
                pw.println();
            }
        });
    }
}
