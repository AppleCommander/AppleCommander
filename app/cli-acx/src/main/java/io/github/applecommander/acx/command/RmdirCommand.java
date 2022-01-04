package io.github.applecommander.acx.command;

import java.util.Optional;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;

import io.github.applecommander.acx.base.ReadWriteDiskCommandOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "rmdir", description = "Remove a directory on disk.",
         aliases = { "rd" })
public class RmdirCommand extends ReadWriteDiskCommandOptions {
    @Option(names = { "-r", "--recursive" }, description = "Recursively delete subdirectories.")
    private boolean recursiveFlag;
    
    @Option(names = { "-f", "--force" }, description = "Force files to be deleted as well.")
    private boolean forceFlag;
    
    @Parameters(description = "Directory name to delete (use '/' as divider).")
    private String fullPath;
    
    @Override
    public int handleCommand() throws Exception {
        FormattedDisk formattedDisk = disk.getFormattedDisks()[0];
        
        // Locate directory
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
                throw new RuntimeException(String.format("Directory does not exist: '%s'", pathName));
            }
        }
        
        deleteDirectory(directory);
        return 0;
    }
    
    public void deleteDirectory(DirectoryEntry directory) throws DiskException {
        for (FileEntry file : directory.getFiles()) {
            if (file.isDeleted()) {
                // skip
            } else if (recursiveFlag && file.isDirectory()) {
                deleteDirectory((DirectoryEntry)file);
            }
            else if (forceFlag && !file.isDirectory()) {
                file.delete();
            }
            else {
                String message = String.format("Encountered %s '%s'",
                        file.isDirectory() ? "directory" : "file",
                        file.getFilename());
                throw new RuntimeException(message);
            }
        }
        
        FileEntry file = (FileEntry)directory;
        file.delete();
    }
}
