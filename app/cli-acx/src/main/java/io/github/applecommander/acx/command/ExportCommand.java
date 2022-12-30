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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.filters.BinaryFileFilter;
import com.webcodepro.applecommander.storage.filters.HexDumpFileFilter;
import com.webcodepro.applecommander.util.filestreamer.FileStreamer;
import com.webcodepro.applecommander.util.filestreamer.FileTuple;
import com.webcodepro.applecommander.util.filestreamer.TypeOfFile;

import io.github.applecommander.acx.FilterMethod;
import io.github.applecommander.acx.base.ReadOnlyDiskImageCommandOptions;
import io.github.applecommander.acx.converter.FilterMethodConverter;
import io.github.applecommander.acx.converter.FilterMethodConverter.FilterMethodCandidates;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "export", description = "Export file(s) from a disk image.",
        aliases = { "x", "get" })
public class ExportCommand extends ReadOnlyDiskImageCommandOptions {
    private static Logger LOG = Logger.getLogger(ExportCommand.class.getName());

    @Spec
    private CommandSpec spec;
    
    @ArgGroup(exclusive = true, heading = "%nFile extract methods:%n")
    private FileExtractMethods extraction = new FileExtractMethods();
    
    @Option(names = { "--deleted" }, description = "Include deleted files (use at your own risk!)")
    private boolean deletedFlag;
    
    @Option(names = { "-o", "--output" }, description = "Extract to file or to directory (default is stdout).")
    private File outputFile;
    
    @Parameters(arity = "*", description = "File glob(s) to extract (default = '*') - be cautious of quoting!")
    private List<String> globs = Arrays.asList("*");

    public void validate() {
        List<String> errors = new ArrayList<>();
        // multiple files require --output
        if (isMultipleFiles()) {
            if (outputFile == null) {
                errors.add("--output directory must be specified with multiple files");
            } else if (!outputFile.isDirectory()) {
                errors.add("--output must be a directory");
            }
        }
        if (!errors.isEmpty()) {
            throw new ParameterException(spec.commandLine(), String.join(", ", errors));
        }
    }
    
    @Override
    public int handleCommand() throws Exception {
        validate();
        
        Consumer<FileTuple> fileHandler = 
                (outputFile == null) ? this::writeToStdout : this::writeToOutput;
        
        FileStreamer.forDisk(disk)
                    .ignoreErrors(true)
                    .includeDeleted(deletedFlag)
                    .includeTypeOfFile(TypeOfFile.FILE)
                    .matchGlobs(globs)
                    .stream()
                    .forEach(fileHandler);
                    
        return 0;
    }
    
    public boolean hasFiles() {
        return globs != null && globs.size() > 1;
    }
    public boolean isAllFiles() {
        return globs == null || globs.isEmpty();
    }
    public boolean isMultipleFiles() {
        return hasFiles() || isAllFiles();
    }

    public void writeToStdout(FileTuple tuple) {
        try {
            FileFilter ff = extraction.extractFunction.apply(tuple.fileEntry);
            System.out.write(ff.filter(tuple.fileEntry));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    public void writeToOutput(FileTuple tuple) {
        File file = outputFile;
        FileFilter ff = extraction.extractFunction.apply(tuple.fileEntry);
        if (file.isDirectory()) {
            if (!tuple.paths.isEmpty()) {
                file = new File(outputFile, String.join(File.pathSeparator, tuple.paths));
                boolean created = file.mkdirs();
                if (created) LOG.info(String.format("Directory created: %s", file.getPath()));
            }
            file = new File(file, ff.getSuggestedFileName(tuple.fileEntry));
        }
        LOG.info(String.format("Writing to '%s'", file.getPath()));
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(ff.filter(tuple.fileEntry));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static class FileExtractMethods {
        private Function<FileEntry,FileFilter> extractFunction = this::asSuggestedFile; 

        @Option(names = { "--method" }, converter = FilterMethodConverter.class,
                completionCandidates = FilterMethodCandidates.class,
                description = "Select a specific export method type (${COMPLETION-CANDIDATES}).")
        public void selectFilterMethod(final FilterMethod filterMethod) {
            this.extractFunction = fileFilter -> filterMethod.create();
        }

        // Short-cuts to some of the more common, non-suggested, filters
        @Option(names = { "--raw", "--binary" }, description = "Extract file in native format.")
        public void setBinaryExtraction(boolean flag) {
            selectFilterMethod(FilterMethod.BINARY);
        }
        @Option(names = { "--hex", "--dump" }, description = "Extract file in hex dump format.")
        public void setHexDumpExtraction(boolean flag) {
            selectFilterMethod(FilterMethod.HEX_DUMP);
        }
        @Option(names = { "--suggested" }, description = "Extract file as suggested by AppleCommander (default)")
        public void setSuggestedExtraction(boolean flag) {
            this.extractFunction = this::asSuggestedFile;
        }
		@Option(names = { "--as", "--applesingle" }, description = "Extract file to AppleSingle file.")
		public void setAppleSingleExtraction(boolean flag) {
		    selectFilterMethod(FilterMethod.APPLESINGLE);
		}
		@Option(names = { "--disassembly" }, description = "Dissassembly file.")
		public void setDisassemblyExtraction(boolean flag) {
		    selectFilterMethod(FilterMethod.DISASSEMBLY);
		}
        
        public FileFilter asSuggestedFile(FileEntry entry) {
            FileFilter ff = entry.getSuggestedFilter();
            if (ff instanceof BinaryFileFilter) {
                ff = new HexDumpFileFilter();
            }
            return ff;
        }
    }
}
