package io.github.applecommander.acx.command;

import java.util.logging.Logger;

import com.webcodepro.applecommander.util.filestreamer.FileTuple;

import io.github.applecommander.acx.base.ReadWriteDiskCommandWithGlobOptions;
import picocli.CommandLine.Command;

@Command(name = "lock", description = "Lock file(s) on a disk image.")
public class LockCommand extends ReadWriteDiskCommandWithGlobOptions {
    private static Logger LOG = Logger.getLogger(LockCommand.class.getName());

    public void fileHandler(FileTuple tuple) {
    	tuple.fileEntry.setLocked(true);
    	LOG.info(() -> String.format("File '%s' locked.", tuple.fileEntry.getFilename()));
    }
}
