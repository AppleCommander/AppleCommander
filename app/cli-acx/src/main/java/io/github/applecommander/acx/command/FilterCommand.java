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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.util.readerwriter.FileEntryReader;
import com.webcodepro.applecommander.util.readerwriter.OverrideFileEntryReader;

import io.github.applecommander.acx.FilterMethod;
import io.github.applecommander.acx.base.ReusableCommandOptions;
import io.github.applecommander.acx.converter.FilterMethodConverter;
import io.github.applecommander.acx.converter.FilterMethodConverter.FilterMethodCandidates;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "filter", description = "Filter on-disk file (that are not in a disk image).")
public class FilterCommand extends ReusableCommandOptions {
    @Spec
    private CommandSpec spec;
    
    @ArgGroup(exclusive = true, heading = "%nInput source:%n")
    private InputData inputData = new InputData();
    
    @ArgGroup(exclusive = true, heading = "%nFile filter methods:%n")
    private FileFilterMethods extraction = new FileFilterMethods();

    @ArgGroup(exclusive = true, heading = "%nOutput destination:%n")
    private OutputData outputData = new OutputData();

    @Override
    public int handleCommand() throws Exception {
        FileEntry fileEntry = inputData.asFileEntry();
        byte[] data = extraction.filter(fileEntry);
        outputData.write(data);

        return 0;
    }
    
    private static class InputData {
        private Supplier<FileEntryReader> fileEntryReaderSupplier = this::fromStdin;
        
        public FileEntry asFileEntry() {
            return new FileEntryMimic(fileEntryReaderSupplier.get());
        }

        @Option(names = { "--stdin" }, description = "Read from standard input (default).")
        public void stdinFlag(boolean flag) {
            fileEntryReaderSupplier = this::fromStdin;
        }
        private FileEntryReader fromStdin() {
            try {
                byte[] data = System.in.readAllBytes();
                return OverrideFileEntryReader.builder()
                        .fileData(data)
                        .build();
            } catch (IOException cause) {
                throw new UncheckedIOException(cause);
            }
        }
        
        @Option(names = { "--in", "--input" }, description = "File to read. (Required if specifying two file names.)")
        public void inputFileFlag(final String filename) {
            fromFileParameter(filename);
        }
        
        @Parameters(description = "File to read.")
        public void fromFileParameter(final String filename) {
            fileEntryReaderSupplier = () -> {
                try {
                    Path path = Path.of(filename);
                    byte[] data = Files.readAllBytes(path);
                    return OverrideFileEntryReader.builder()
                            .fileData(data)
                            .filename(filename)
                            .build();
                } catch (IOException cause) {
                    throw new UncheckedIOException(cause);
                }
            };
        }
    }

    private static class FileFilterMethods {
        private FilterMethod filterMethod = FilterMethod.TEXT; 
        public byte[] filter(FileEntry fileEntry) {
            return filterMethod.create().filter(fileEntry);
        }

        @Option(names = { "--method" }, converter = FilterMethodConverter.class,
                completionCandidates = FilterMethodCandidates.class,
                description = "Select a specific export method type (${COMPLETION-CANDIDATES}).")
        public void selectFilterMethod(final FilterMethod filterMethod) {
            this.filterMethod = filterMethod;
        }

        // Short-cuts to some of the more common, non-suggested, filters
        @Option(names = { "--text" }, description = "Treat file as Apple II text file (default).")
        public void setTextFilter(boolean flag) {
            selectFilterMethod(FilterMethod.TEXT);
        }
        @Option(names = { "--disassembly" }, description = "Disassemble input file.")
        public void setDisassemblyFilter(boolean flag) {
            selectFilterMethod(FilterMethod.DISASSEMBLY);
        }
        @Option(names = { "--applesoft" }, description = "De-tokenize Applesoft input file.")
        public void setApplesoftFilter(boolean flag) {
            selectFilterMethod(FilterMethod.DISASSEMBLY);
        }
    }
    
    private static class OutputData {
        private Consumer<byte[]> outputDataConsumer = this::writeToStdout;
        public void write(byte[] data) {
            outputDataConsumer.accept(data);
        }
        
        @Option(names = { "--stdout" }, description = "Write to standard output (default).")
        public void stdoutFlag(boolean flag) {
            outputDataConsumer = this::writeToStdout;
        }
        private void writeToStdout(byte[] data) {
            try {
                System.out.write(data);
            } catch (IOException cause) {
                throw new UncheckedIOException(cause);
            }
        }
        
        @Option(names = { "--out", "--output" }, description = "File to write. (Required if using stdin but writing to file.)")
        public void outputFile(final String filename) {
            toFile(filename);
        }
        
        @Parameters(description = "File to write.")
        public void toFile(final String filename) {
            outputDataConsumer = (data) -> {
                try {
                    Files.write(Path.of(filename), data);
                } catch (IOException cause) {
                    throw new UncheckedIOException(cause);
                }
            };
        }
    }
    
    public static class FileEntryMimic implements FileEntry {
        private FileEntryReader fileEntryReader;
        
        public FileEntryMimic(FileEntryReader fileEntryReader) {
            Objects.requireNonNull(fileEntryReader);
            this.fileEntryReader = fileEntryReader;
        }

        @Override
        public String getFilename() { 
            return fileEntryReader.getFilename().orElse("UNKNOWN"); 
        }

        @Override
        public void setFilename(String filename) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getFiletype() {
            return fileEntryReader.getProdosFiletype().orElse("BIN");
        }

        @Override
        public void setFiletype(String filetype) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isLocked() {
            return fileEntryReader.isLocked().orElse(false);
        }

        @Override
        public void setLocked(boolean lock) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getSize() {
            return getFileData().length;
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public boolean isDeleted() {
            return false;
        }

        @Override
        public void delete() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> getFileColumnData(int displayMode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] getFileData() {
            return fileEntryReader.getFileData().orElse(new byte[0]);
        }

        @Override
        public void setFileData(byte[] data) throws DiskFullException {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileFilter getSuggestedFilter() {
            throw new UnsupportedOperationException();
        }

        @Override
        public FormattedDisk getFormattedDisk() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getMaximumFilenameLength() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean needsAddress() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setAddress(int address) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getAddress() {
            return fileEntryReader.getBinaryAddress().orElse(0x800);
        }

        @Override
        public boolean canCompile() {
            throw new UnsupportedOperationException();
        }
    }
}
