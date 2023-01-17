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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.FormattedDisk.FileColumnHeader;
import com.webcodepro.applecommander.ui.DirectoryLister.CsvListingStrategy;
import com.webcodepro.applecommander.ui.DirectoryLister.JsonListingStrategy;
import com.webcodepro.applecommander.ui.DirectoryLister.ListingStrategy;
import com.webcodepro.applecommander.util.filestreamer.FileStreamer;
import com.webcodepro.applecommander.util.filestreamer.FileTuple;
import com.webcodepro.applecommander.util.filestreamer.TypeOfFile;

import io.github.applecommander.acx.base.ReadOnlyDiskImageCommandOptions;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "list", description = "List directory of disk image(s).",
        aliases = { "ls" })
public class ListCommand extends ReadOnlyDiskImageCommandOptions {
    @ArgGroup(exclusive = true, multiplicity = "0..1", heading = "%nFile display formatting:%n")
    private FileDisplay fileDisplay = new FileDisplay();
    
    @Option(names = { "-r", "--recursive"}, description = "Display directory recursively.", negatable = true, defaultValue = "false")
    private boolean recursiveFlag;
    
    @Option(names = { "--deleted" }, description = "Show deleted files.")
    private boolean deletedFlag;
    
    @ArgGroup(exclusive = true, multiplicity = "0..1")
    private TypeOfFileSelection typeOfFile = new TypeOfFileSelection();
    
    @ArgGroup(exclusive = true, multiplicity = "0..1", heading = "%nOutput format:%n")
    private OutputType outputType = new OutputType();

    @Option(names = "--header", negatable = true, description = "Show header.")
    private boolean headerFlag = true;

    @Option(names = "--column", negatable = true, description = "Show column headers.")
    private boolean columnFlag = true;

    @Option(names = "--footer", negatable = true, description = "Show footer.")
    private boolean footerFlag = true;

    @Option(names = "--globs", defaultValue = "*", split = ",", description = "File glob(s) to match.")
    private List<String> globs = new ArrayList<String>();

    @Override
    public int handleCommand() throws Exception {
        int display = fileDisplay.format();
        ListingStrategy listingStrategy = outputType.create(display);
        
        listingStrategy.first(disk);

        FormattedDisk formattedDisk = disk.getFormattedDisks()[0];
        listingStrategy.beforeDisk(formattedDisk);
        
        FileStreamer.forDisk(disk)
                    .ignoreErrors(true)
                    .includeDeleted(deletedFlag)
                    .recursive(recursiveFlag)
                    .includeTypeOfFile(typeOfFile.typeOfFile())
                    .matchGlobs(globs)
                    .stream()
                    .forEach(listingStrategy::forEach);

        listingStrategy.afterDisk(formattedDisk);
        
        listingStrategy.last(disk);
        
        return 0;
    }
    
    public static class FileDisplay {
        public int format() {
            if (standardFormat) {
                return FormattedDisk.FILE_DISPLAY_STANDARD;
            }
            if (longFormat) {
                return FormattedDisk.FILE_DISPLAY_DETAIL;
            }
            return FormattedDisk.FILE_DISPLAY_NATIVE;
        }
        
        @Option(names = { "-n", "--native" }, description = "Use native directory format (default).")
        private boolean nativeFormat;

        @Option(names = { "-s", "--short", "--standard" }, description = "Use brief directory format.")
        private boolean standardFormat;
        
        @Option(names = { "-l", "--long",  "--detail" }, description = "Use long/detailed directory format.")
        private boolean longFormat;
    }
    
    public static class OutputType {
        private OutputStrategy outputStrategy = OutputStrategy.TEXT;
        public ListingStrategy create(int display) {
            return outputStrategy.create(display);
        }

        private enum OutputStrategy { 
            TEXT(FormattedTextListingStrategy::new), 
            CSV(CsvListingStrategy::new),
            JSON(JsonListingStrategy::new);
            
            private Function<Integer,ListingStrategy> constructorFn;
            
            private OutputStrategy(Function<Integer,ListingStrategy> constructorFn) {
                this.constructorFn = constructorFn;
            }
            
            public ListingStrategy create(int display) {
                return constructorFn.apply(display);
            }

        };
        
        @Option(names = "--text", description = "Formatted text (default).")
        public void selectTextOutput(boolean flag) {
            this.outputStrategy = OutputStrategy.TEXT;
        }
        
        @Option(names = "--json", description = "JSON output.")
        public void selectJsonOutput(boolean flag) {
            this.outputStrategy = OutputStrategy.JSON;
        }
        
        @Option(names = "--csv", description = "CSV output.")
        public void selectCsvOutput(boolean flag) {
            this.outputStrategy = OutputStrategy.CSV;
        }
    }
    
    public static class FormattedTextListingStrategy extends ListingStrategy {
        private List<String> fmtSpec;
        
        public FormattedTextListingStrategy(int display) {
            super(display);
        }
        @Override
        public void beforeDisk(FormattedDisk disk) {
            List<FileColumnHeader> headers = disk.getFileColumnHeaders(display);
            fmtSpec = createFormatSpec(headers);
        
            System.out.println();
            System.out.printf("File: %s\n", disk.getFilename());
            System.out.printf("Name: %s\n", disk.getDiskName());
        }
        @Override
        public void forEach(FileTuple tuple) {
            List<String> data = tuple.fileEntry.getFileColumnData(display);
            for (int i=0; i<tuple.paths.size(); i++) { 
                System.out.print("  ");
            }
            for (int d = 0; d < data.size(); d++) {
                System.out.printf(fmtSpec.get(d), data.get(d));
            }
            if (tuple.fileEntry.isDeleted()) {
                System.out.print("[deleted]");
            }
            System.out.println();
        }
        @Override
        public void afterDisk(FormattedDisk disk) {
            System.out.printf("%s format; %,d bytes free; %,d bytes used.\n",
                      disk.getFormat(),
                      disk.getFreeSpace(),
                      disk.getUsedSpace());
        }
        
        private List<String> createFormatSpec(List<FileColumnHeader> fileColumnHeaders) {
            List<String> fmtSpec = new ArrayList<>();
            for (FileColumnHeader h : fileColumnHeaders) {
                String spec = String.format("%%%s%ds ", h.isRightAlign() ? "" : "-", 
                        h.getMaximumWidth());
                fmtSpec.add(spec);
            }
            return fmtSpec;
        }
    }
    
    public static class TypeOfFileSelection {
        public TypeOfFile typeOfFile() {
            if (filesOnly) {
                return TypeOfFile.FILE;
            }
            if (directoriesOnly) {
                return TypeOfFile.DIRECTORY;
            }
            return TypeOfFile.BOTH;
        }
        
        @Option(names = "--file", description = "Only include files.")
        private boolean filesOnly;
        
        @Option(names = "--directory", description = "Only include directories.")
        private boolean directoriesOnly;
    }
}
