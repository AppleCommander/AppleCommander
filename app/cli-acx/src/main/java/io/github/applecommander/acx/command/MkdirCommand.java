package io.github.applecommander.acx.command;

import java.util.Optional;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;

import io.github.applecommander.acx.base.ReadWriteDiskCommandOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "mkdir", description = "Create a directory on disk.",
         aliases = { "md" })
public class MkdirCommand extends ReadWriteDiskCommandOptions {
    @Option(names = { "-p" }, description = "Create intermediate subdirectories.")
    private boolean prefixFlag;
    
    @Parameters(description = "Directory name to create (use '/' as divider).")
    private String fullPath;

    @Override
    public int handleCommand() throws Exception {
        FormattedDisk formattedDisk = disk.getFormattedDisks()[0];
        DirectoryEntry directory = formattedDisk;
        
        String[] paths = fullPath.split("/");
        for (int i=0; i<paths.length; i++) {
            final String pathName = formattedDisk.getSuggestedFilename(paths[i]);
            Optional<FileEntry> optEntry = directory.getFiles().stream()
                    .filter(entry -> entry.getFilename().equalsIgnoreCase(pathName))
                    .findFirst();
            
            if (optEntry.isPresent()) {
                FileEntry fileEntry = optEntry.get();
                if (fileEntry instanceof DirectoryEntry) {
                    directory = (DirectoryEntry)fileEntry;
                }
                else {
                    throw new RuntimeException(String.format("Not a directory: '%s'", pathName));
                }
            }
            else {
                if (prefixFlag || i == paths.length-1) {
                    directory = directory.createDirectory(pathName);
                } 
                else {
                    throw new RuntimeException(String.format("Directory does not exist: '%s'", pathName));
                }
            }
        }
        return 0;
    }
}
