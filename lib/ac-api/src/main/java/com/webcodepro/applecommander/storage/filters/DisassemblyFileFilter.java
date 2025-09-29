/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2022 by Robert Greene
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
package com.webcodepro.applecommander.storage.filters;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;

import com.webcodepro.applecommander.storage.os.cpm.CpmFileEntry;
import com.webcodepro.applecommander.storage.os.nakedos.NakedosFileEntry;
import com.webcodepro.applecommander.storage.os.pascal.PascalFileEntry;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFileEntry;
import org.applecommander.disassembler.api.Disassembler;
import org.applecommander.disassembler.api.Instruction;
import org.applecommander.disassembler.api.InstructionSet;
import org.applecommander.disassembler.api.mos6502.InstructionSet6502;
import org.applecommander.disassembler.api.z80.InstructionSetZ80;

/**
 * Disassemble the given set of bytes.
 */
public class DisassemblyFileFilter implements FileFilter {
    private final InstructionSet instructionSet;
    private final Map<Integer,String> labels = new HashMap<>();

    public DisassemblyFileFilter(FileEntry fileEntry) {
        // Figure out the likely InstructionSet.
        this.instructionSet = switch (fileEntry) {
            case CpmFileEntry ignored -> InstructionSetZ80.forZ80();
            case NakedosFileEntry ignored -> InstructionSet6502.for65C02();
            // TODO likely should detect codefile and do that if applicable
            case PascalFileEntry ignored -> InstructionSet6502.for6502();
            case ProdosFileEntry ignored -> InstructionSet6502.for65C02();
            default -> InstructionSet6502.for6502();
        };
    }

	public byte[] filter(FileEntry fileEntry) {
        labels.clear();
        List<String> libraries = instructionSet.defaults().libraryLabels();
        // TODO once this is rolled into the disassembler API, clean it up
        if (libraries.contains("None")) {
            libraries = Collections.emptyList();
        }
        else if (libraries.contains("All")) {
            libraries = new ArrayList<>(Disassembler.labelGroups());
        }
        List<Instruction> instructions = Disassembler.with(fileEntry.getFileData())
                .startingAddress(fileEntry.getAddress())
                .section(libraries)
                .use(instructionSet)
                .decode(labels);

        String code = instructions.stream()
            .map(this::emitRaw)
            .collect(Collectors.joining());
        
        return code.getBytes();
	}

	public String getSuggestedFileName(FileEntry fileEntry) {
		String fileName = fileEntry.getFilename().trim();
		if (!fileName.toLowerCase().endsWith(".asm")) {
			fileName = fileName + ".asm";
		}
		return fileName;
	}

    public String emitRaw(Instruction instruction) {
        // TODO once standard formatting is in disassembler API, use that instead
        final int bytesPerLine = instructionSet.defaults().bytesPerInstruction();

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.printf("%04X- ", instruction.address());

        byte[] code = instruction.code();
        for (int i=0; i<bytesPerLine; i++) {
            if (i >= code.length) {
                pw.print("   ");
            } else {
                pw.printf("%02X ", code[i]);
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

        if (code.length > bytesPerLine) {
            for (int i=bytesPerLine; i<code.length; i++) {
                if (i % bytesPerLine == 0) {
                    if (i > bytesPerLine) pw.println();
                    pw.printf("%04X- ", instruction.address()+i);
                }
                pw.printf("%02X ", code[i]);
            }
            pw.println();
        }
        return sw.toString();
    }
}
