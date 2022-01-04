package io.github.applecommander.acx.command;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.github.applecommander.acx.base.ReadWriteDiskCommandOptions;
import io.github.applecommander.filestreamer.FileStreamer;
import io.github.applecommander.filestreamer.FileTuple;
import io.github.applecommander.filestreamer.TypeOfFile;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "rename", description = "Rename file on a disk image.",
        aliases = { "ren" })
public class RenameFileCommand extends ReadWriteDiskCommandOptions {
    private static Logger LOG = Logger.getLogger(RenameFileCommand.class.getName());
    
    @Option(names = { "-m", "--multiple" }, description = "Force rename when multiple files found.")
    private boolean multipleOverride;
    
    @Option(names = { "-f", "--force" }, description = "Rename locked files.")
    private boolean lockOverride;
    
    @Parameters(index = "0", description = "Original file name (include path).")
    private String originalFilename;
    
    @Parameters(index = "1", description = "New file name (just the new filename).")
    private String newFilename;

    @Override
    public int handleCommand() throws Exception {
        List<FileTuple> files = FileStreamer.forDisk(disk)
			        .ignoreErrors(true)
			        .includeTypeOfFile(TypeOfFile.FILE)
			        .matchGlobs(originalFilename)
			        .stream()
			        .collect(Collectors.toList());

        if (files.isEmpty()) {
        	LOG.warning(() -> String.format("File not found for %s.", originalFilename));
        }
        else if (!multipleOverride && files.size() > 1) {
        	LOG.severe(() -> String.format("Multile files with %s found (count = %d).", 
        			originalFilename, files.size()));
        } 
    	else {
        	files.forEach(this::fileHandler);
        }
        return 0;
    }
    
    public void fileHandler(FileTuple tuple) {
    	if (tuple.fileEntry.isLocked()) {
    		if (lockOverride) {
    			LOG.info(() -> String.format("File '%s' is locked, but 'force' specified; ignoring lock.",
    					tuple.fileEntry.getFilename()));
    		} else {
	    		LOG.warning(() -> String.format("File '%s' is locked.", tuple.fileEntry.getFilename()));
	    		return;
    		}
    	}
    	tuple.fileEntry.setFilename(newFilename);
    	LOG.info(() -> String.format("File '%s' renamed to '%s'.", originalFilename, newFilename));
    }
}
