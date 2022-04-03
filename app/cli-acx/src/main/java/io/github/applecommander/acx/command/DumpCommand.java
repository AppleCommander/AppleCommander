/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2019-2022 by Robert Greene and others
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
package io.github.applecommander.acx.command;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.webcodepro.applecommander.util.AppleUtil;

import io.github.applecommander.acx.arggroup.CoordinateSelection;
import io.github.applecommander.acx.base.ReadOnlyDiskImageCommandOptions;
import io.github.applecommander.acx.converter.IntegerTypeConverter;
import io.github.applecommander.disassembler.api.Disassembler;
import io.github.applecommander.disassembler.api.Instruction;
import io.github.applecommander.disassembler.api.mos6502.InstructionSet6502;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "dump", description = "Dump a block or sector.")
public class DumpCommand extends ReadOnlyDiskImageCommandOptions {
    @ArgGroup(heading = "%nOutput Selection:%n")
    private OutputSelection output = new OutputSelection();

    @Mixin
    private Options options = new Options();

    @Override
    public int handleCommand() throws Exception {
        byte[] data = options.coordinate.read(disk);
        System.out.println(output.format(options, data));
        return 0;
    }
    
    public static class OutputSelection {
        private BiFunction<Options,byte[],String> fn = this::formatHexDump;
        public String format(Options options, byte[] data) {
            return fn.apply(options, data);
        }
        
        @Option(names = "--hex", description = "Hex dump.")
        public void selectHexDump(boolean flag) {
            fn = this::formatHexDump;
        }
        
        @Option(names = "--disassembly", description = "Disassembly.")
        public void selectDisassembly(boolean flag) {
            fn = this::formatDisassembly;
        }
        
        public String formatHexDump(Options options, byte[] data) {
            return AppleUtil.getHexDump(options.address, data);
        }
        
        public String formatDisassembly(Options options, byte[] data) {
            // If the offset is given, use that. If not, use 0 except for the boot sector and then use 1.
            int calculatedOffset = options.offset.orElse(options.coordinate.includesBootSector() ? 1 : 0);
            return Disassembler.with(data)
                    .startingAddress(options.address)
                    .bytesToSkip(calculatedOffset)
                    .use(InstructionSet6502.for6502())
                    .decode()
                    .stream()
                    .map(this::formatInstruction)
                    .collect(Collectors.joining());

        }
        private String formatInstruction(Instruction instruction) {
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
    
    public static class Options {
        @ArgGroup(multiplicity = "1", heading = "%nCoordinate Selection:%n")
        private CoordinateSelection coordinate = new CoordinateSelection();
        
        @Option(names = { "-a", "--address" }, converter = IntegerTypeConverter.class, 
                description = "Starting address.")
        private int address = 0x800;
        
        @Option(names = { "-o", "--offset" }, converter = IntegerTypeConverter.class, 
                description = "Number of bytes to skip into file before disassembling.")
        private Optional<Integer> offset = Optional.empty();
    }
}
