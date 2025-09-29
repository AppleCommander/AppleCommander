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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.webcodepro.applecommander.util.AppleUtil;

import com.webcodepro.applecommander.util.Range;
import io.github.applecommander.acx.base.ReadOnlyDiskContextCommandOptions;
import io.github.applecommander.acx.converter.IntegerTypeConverter;
import io.github.applecommander.acx.converter.RangeTypeConverter;
import org.applecommander.disassembler.api.Disassembler;
import org.applecommander.disassembler.api.Instruction;
import org.applecommander.disassembler.api.InstructionSet;
import org.applecommander.disassembler.api.mos6502.InstructionSet6502;
import org.applecommander.disassembler.api.sweet16.InstructionSetSWEET16;
import org.applecommander.disassembler.api.switching6502.InstructionSet6502Switching;
import org.applecommander.device.BlockDevice;
import org.applecommander.device.nibble.NibbleTrackReaderWriter;
import org.applecommander.device.TrackSectorDevice;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "dump", description = "Dump a block or sector.", sortOptions = false)
public class DumpCommand extends ReadOnlyDiskContextCommandOptions {
    @ArgGroup(heading = "%nOutput Selection:%n")
    private OutputSelection output = new OutputSelection();

    @Mixin
    private Options options = new Options();

    @Override
    public int handleCommand() throws Exception {
        if (options.coordinate.blockRangeSelection != null) {
            BlockDevice device = blockDevice()
                    .orElseThrow(() -> new RuntimeException("there is no block device available"));
            options.coordinate.blockRangeSelection.blocks.stream().forEach(block -> {
                validateBlockNum(device, block);
                options.includesBootSector = block == 0;
                try {
                    byte[] data = device.readBlock(block).asBytes();
                    System.out.printf("Block #%d:\n", block);
                    System.out.println(output.format(options, data));
                } catch (Throwable t) {
                    System.err.println(t.getMessage());
                }
            });
            return 0;
        }
        else if (options.coordinate.trackSectorRangeSelection != null) {
            TrackSectorDevice device = trackSectorDevice()
                    .orElseThrow(() -> new RuntimeException("there is no track/sector device available"));
            options.coordinate.trackSectorRangeSelection.tracks.stream().forEach(track -> {
                options.coordinate.trackSectorRangeSelection.sectors.stream().forEach(sector -> {
                    validateTrackAndSector(device, track, sector);
                    options.includesBootSector = track == 0 && sector == 0;
                    try {
                        byte[] data = device.readSector(track, sector).asBytes();
                        System.out.printf("Track %02d, Sector %02d:\n", track, sector);
                        System.out.println(output.format(options, data));
                    } catch (Throwable t) {
                        System.err.println(t.getMessage());
                    }
                });
            });
            return 0;
        }
        else if (options.coordinate.nibbleTrackRangeSelection != null) {
            if (context().nibbleTrackReaderWriter == null) {
                throw new RuntimeException("This is not a nibble device.");
            }
            NibbleTrackReaderWriter trackReaderWriter = context().nibbleTrackReaderWriter;
            options.coordinate.nibbleTrackRangeSelection.tracks.stream().forEach(track -> {
                final int tracksPerDisk = trackReaderWriter.getTracksOnDevice();
                if (track < 0 || track >= tracksPerDisk) {
                    String errormsg = String.format("The track number(%d) is out of range(0-%d) on this image.", track, tracksPerDisk-1);
                    throw new IllegalArgumentException(errormsg);
                }
                try {
                    byte[] data = trackReaderWriter.readTrackData(track).asBytes();
                    System.out.printf("Track %02d\n", track);
                    System.out.println(output.format(options, data));
                } catch (Throwable t) {
                    System.err.println(t.getMessage());
                }
            });
            return 0;
        }
        System.out.println("Please choose block(s) or track(s) and sector(s).");
        return 1;
    }

    public void validateBlockNum(BlockDevice device, int block) throws IllegalArgumentException {
        final int blocksOnDevice = device.getGeometry().blocksOnDevice();

        if (block < 0 || block >= blocksOnDevice) {
            String errormsg = String.format("The block number(%d) is out of range(0-%d) on this image.", block, blocksOnDevice-1);
            throw new IllegalArgumentException(errormsg);
        }
    }

    public void validateTrackAndSector(TrackSectorDevice device, int track, int sector) throws IllegalArgumentException  {
        final int tracksPerDisk = device.getGeometry().tracksOnDisk();
        final int sectorsPerTrack = device.getGeometry().sectorsPerTrack();

        if (track < 0 || track >= tracksPerDisk) {
            String errormsg = String.format("The track number(%d) is out of range(0-%d) on this image.", track, tracksPerDisk-1);
            throw new IllegalArgumentException(errormsg);
        }

        if (sector < 0 || sector >= sectorsPerTrack) {
            String errormsg = String.format("The sector number(%d) is out of range(0-%d) on this image.", sector, sectorsPerTrack-1);
            throw new IllegalArgumentException(errormsg);
        }
    }

    public static class OutputSelection {
        private BiFunction<Options,byte[],String> fn = this::formatHexDump;
        public String format(Options options, byte[] data) {
            return fn.apply(options, data);
        }
        
        @Option(names = "--hex", description = "Hex dump. (default)")
        public void selectHexDump(boolean flag) {
            fn = this::formatHexDump;
        }

        @Option(names = "--disassembly", description = "Disassembly.")
        public void selectDisassembly(boolean flag) {
            fn = this::formatDisassembly;
        }
        
        public String formatHexDump(Options options, byte[] data) {
            return AppleUtil.getHexDump(data);
        }
        
        public String formatDisassembly(Options options, byte[] data) {
            // If the offset is given, use that. If not, use 0 except for the boot sector and then use 1.
            int calculatedOffset = options.disassemblerOptions.offset.orElse(options.includesBootSector ? 1 : 0);
            final InstructionSet instructionSet = options.disassemblerOptions.instructionSet.get();
            final Map<Integer,String> labels = new HashMap<>();
            return Disassembler.with(data)
                    .startingAddress(options.disassemblerOptions.address)
                    .bytesToSkip(calculatedOffset)
                    .use(instructionSet)
                    .decode(labels)
                    .stream()
                    .map(instruction -> formatInstruction(instruction,labels,instructionSet.defaults()))
                    .collect(Collectors.joining());
        }

        private String formatInstruction(Instruction instruction, Map<Integer,String> labels, InstructionSet.Defaults defaults) {
            final int bytesPerLine = defaults.bytesPerInstruction();

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
            if (defaults.includeDescription()) {
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
    
    public static class Options {
        // Somewhat of a hack; the disassembly listing should know if we are on the boot sector to set the sector offset
        // correctly...
        private boolean includesBootSector;

        @ArgGroup(multiplicity = "1")
        private CoordinateRangeSelection coordinate = new CoordinateRangeSelection();
		
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

            @Option(names = "--6502", description = "MOS 6502. (default)")
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

    public static class CoordinateRangeSelection {
        @ArgGroup(exclusive = false, heading = "%nBlock devices: (use '0' or '0-5' for a range)%n")
        private BlockRangeSelection blockRangeSelection;
        @ArgGroup(exclusive = false, heading = "%nTrack/Sector devices: (use '0' or '0-5' for a range)%n")
        private TrackSectorRangeSelection trackSectorRangeSelection;
        @ArgGroup(exclusive = false, heading = "%nNibble track/sector devices: (use '0' or '0-5' for a range)%n")
        private NibbleTrackRangeSelection nibbleTrackRangeSelection;
    }

    public static class BlockRangeSelection {
        @Option(names = { "-b", "--block" }, description = "Block number(s).",
                converter = RangeTypeConverter.class)
        private Range blocks;
    }
    public static class TrackSectorRangeSelection {
        @Option(names = { "-t", "--track" }, required = true, description = "Track number(s).",
                converter = RangeTypeConverter.class)
        private Range tracks;
        @Option(names = { "-s", "--sector" }, required = true, description = "Sector number(s).",
                converter = RangeTypeConverter.class)
        private Range sectors;
    }
    public static class NibbleTrackRangeSelection {
        @Option(names = "-n", description = "Track number(s).",
                converter = RangeTypeConverter.class)
        private Range tracks;
    }
}
