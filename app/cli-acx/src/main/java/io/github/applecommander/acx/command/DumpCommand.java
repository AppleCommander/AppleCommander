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
import io.github.applecommander.disassembler.api.InstructionSet;
import io.github.applecommander.disassembler.api.mos6502.InstructionSet6502;
import io.github.applecommander.disassembler.api.sweet16.InstructionSetSWEET16;
import io.github.applecommander.disassembler.api.switching6502.InstructionSet6502Switching;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "dump", description = "Dump a block or sector.", sortOptions = false)
public class DumpCommand extends ReadOnlyDiskImageCommandOptions {
    @ArgGroup(heading = "%nOutput Selection:%n")
    private OutputSelection output = new OutputSelection();

    @Mixin
    private Options options = new Options();

    @Override
    public int handleCommand() throws Exception {
        byte[] data = options.coordinate.read(disk);
		if (output.raw) {
            System.out.write(data);
        } else {
            System.out.println(output.format(options, data));
        }
        return 0;
    }
    
    public static class OutputSelection {
        private BiFunction<Options,byte[],String> fn = this::formatHexDump;
        public String format(Options options, byte[] data) {
            return fn.apply(options, data);
        }
        
        @Option(names = "--hex", description = "Hex dump. (Default)")
        public void selectHexDump(boolean flag) {
            fn = this::formatHexDump;
        }
		
        @Option(names = "--disassembly", description = "Disassembly.")
        public void selectDisassembly(boolean flag) {
            fn = this::formatDisassembly;
        }
		
        @Option(names = "--raw", description = "Raw Binary dump.")
        public boolean raw = false;
        
        public String formatHexDump(Options options, byte[] data) {
            return AppleUtil.getHexDump(data);
        }
        
        public String formatDisassembly(Options options, byte[] data) {
            // If the offset is given, use that. If not, use 0 except for the boot sector and then use 1.
            int calculatedOffset = options.disassemblerOptions.offset.orElse(options.coordinate.includesBootSector() ? 1 : 0);
            return Disassembler.with(data)
                    .startingAddress(options.disassemblerOptions.address)
                    .bytesToSkip(calculatedOffset)
                    .use(options.disassemblerOptions.instructionSet.get())
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
		
        @ArgGroup(heading = "%nDisassembler Options:%n", exclusive = false)
        private DisassemblerOptions disassemblerOptions = new DisassemblerOptions();
    }

    public static class DisassemblerOptions {
        @Option(names = {"-a", "--address"}, converter = IntegerTypeConverter.class,
                         description = "Starting Address.")
        private int address = 0x800;
		
        @Option(names = {"-o", "--offset"}, converter = IntegerTypeConverter.class,
                         description = "Number of bytes to skip into file before disassembling.")
        private Optional<Integer> offset = Optional.empty();
		
        @ArgGroup(multiplicity = "0..1")
        private InstructionSetSelection instructionSet = new InstructionSetSelection();
	
        public static class InstructionSetSelection {
            private InstructionSet instructionSet = InstructionSet6502.for6502();
        
            public InstructionSet get() {
                return this.instructionSet;
            }

	        @Option(names = "--6502", description = "MOS 6502. (Default)")
            public void select6502(boolean flag) {
                this.instructionSet = InstructionSet6502.for6502();
            }
            @Option(names = { "--65C02" }, description = "WDC 65C02.")
            public void select65C02(boolean flag) {
                this.instructionSet = InstructionSet6502.for65C02();
            }
            @Option(names = { "--6502X" }, description = "MOS 6502 + 'illegal' instructions.")
            public void select6502X(boolean flag) {
                this.instructionSet = InstructionSet6502.for6502withIllegalInstructions();
            }
            @Option(names = { "--SWEET16" }, description = "SWEET16.")
            public void selectSWEET16(boolean flag) {
                this.instructionSet = InstructionSetSWEET16.forSWEET16();
            }
            @Option(names = { "--6502S" }, description = "MOS 6502 with SWEET16 switching.")
            public void select6502Switching(boolean flag) {
                this.instructionSet = InstructionSet6502Switching.withSwitching();
            }		
        }	
    }
}
