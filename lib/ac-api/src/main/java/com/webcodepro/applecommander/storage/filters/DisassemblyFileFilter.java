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
import java.util.List;
import java.util.stream.Collectors;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;

import io.github.applecommander.disassembler.api.Disassembler;
import io.github.applecommander.disassembler.api.Instruction;
import io.github.applecommander.disassembler.api.mos6502.InstructionSet6502;

/**
 * Disassemble the given set of bytes.
 */
public class DisassemblyFileFilter implements FileFilter {
	public byte[] filter(FileEntry fileEntry) {
        List<Instruction> instructions = Disassembler.with(fileEntry.getFileData())
                .startingAddress(fileEntry.getAddress())
                .use(InstructionSet6502.for6502())
                .decode();

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
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.printf("%04X- ", instruction.getAddress());
        
        byte[] code = instruction.getBytes();
        for (int i=0; i<3; i++) {
            if (i >= code.length) {
                pw.printf("   ");
            } else {
                pw.printf("%02X ", code[i]);
            }
        }
        pw.printf(" %s\n", instruction.formatOperandWithValue());
        return sw.toString();
    }
}
