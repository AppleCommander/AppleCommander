package io.github.applecommander.acx.command;

import java.util.ArrayList;
import java.util.List;

import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.FormattedDisk.FileColumnHeader;

import io.github.applecommander.acx.base.ReadOnlyDiskImageCommandOptions;
import io.github.applecommander.filestreamer.FileStreamer;
import io.github.applecommander.filestreamer.FileTuple;
import io.github.applecommander.filestreamer.TypeOfFile;
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

    @Option(names = "--header", negatable = true, description = "Show header.")
    private boolean headerFlag = true;

    @Option(names = "--column", negatable = true, description = "Show column headers.")
    private boolean columnFlag = true;

    @Option(names = "--footer", negatable = true, description = "Show footer.")
    private boolean footerFlag = true;

    @Option(names = "--globs", defaultValue = "*", split = ",", description = "File glob(s) to match.")
    private List<String> globs = new ArrayList<String>();

    private List<String> fmtSpec;
    
    @Override
    public int handleCommand() throws Exception {
        FileStreamer.forDisk(disk)
                    .ignoreErrors(true)
                    .includeDeleted(deletedFlag)
                    .recursive(recursiveFlag)
                    .includeTypeOfFile(typeOfFile.typeOfFile())
                    .matchGlobs(globs)
                    .beforeDisk(this::header)
                    .afterDisk(this::footer)
                    .stream()
                    .forEach(this::list);
        return 0;
    }
    
    protected void header(FormattedDisk disk) {
        List<FileColumnHeader> headers = disk.getFileColumnHeaders(fileDisplay.format());
        fmtSpec = createFormatSpec(headers);
    
        System.out.println();
        System.out.printf("File: %s\n", disk.getFilename());
        System.out.printf("Name: %s\n", disk.getDiskName());
    }
    
    protected void list(FileTuple tuple) {
        if (!deletedFlag && tuple.fileEntry.isDeleted()) {
            return;
        }

        List<String> data = tuple.fileEntry.getFileColumnData(fileDisplay.format());
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
    
    protected void footer(FormattedDisk disk) {
        System.out.printf("%s format; %d bytes free; %d bytes used.\n",
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
