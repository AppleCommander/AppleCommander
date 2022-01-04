package io.github.applecommander.acx.command;

import java.util.logging.Logger;

import io.github.applecommander.acx.base.ReadWriteDiskCommandWithGlobOptions;
import io.github.applecommander.filestreamer.FileTuple;
import picocli.CommandLine.Command;

@Command(name = "unlock", description = "Unlock file(s) on a disk image.")
public class UnlockCommand extends ReadWriteDiskCommandWithGlobOptions {
    private static Logger LOG = Logger.getLogger(UnlockCommand.class.getName());

    public void fileHandler(FileTuple tuple) {
    	tuple.fileEntry.setLocked(false);
    	LOG.info(() -> String.format("File '%s' unlocked.", tuple.fileEntry.getFilename()));
    }
}
