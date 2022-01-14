package io.github.applecommander.acx.command;

import java.util.logging.Logger;

import com.webcodepro.applecommander.util.filestreamer.FileTuple;

import io.github.applecommander.acx.base.ReadWriteDiskCommandWithGlobOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "delete", description = "Delete file(s) from a disk image.",
        aliases = { "del", "rm" })
public class DeleteCommand extends ReadWriteDiskCommandWithGlobOptions {
    private static Logger LOG = Logger.getLogger(DeleteCommand.class.getName());
    
    @Option(names = { "-f", "--force" }, description = "Force delete locked files.")
    private boolean forceFlag;

    public void fileHandler(FileTuple tuple) {
    	if (tuple.fileEntry.isLocked()) {
    		if (forceFlag) {
    			LOG.info(() -> String.format("File '%s' is locked, but 'force' specified; ignoring lock.",
    					tuple.fileEntry.getFilename()));
    		} else {
	    		LOG.warning(() -> String.format("File '%s' is locked.", tuple.fileEntry.getFilename()));
	    		return;
    		}
    	}
    	tuple.fileEntry.delete();
    	LOG.info(() -> String.format("File '%s' deleted.", tuple.fileEntry.getFilename()));
    }
}
