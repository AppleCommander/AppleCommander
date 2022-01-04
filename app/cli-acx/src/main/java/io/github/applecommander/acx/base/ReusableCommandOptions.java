package io.github.applecommander.acx.base;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.Disk;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(descriptionHeading = "%n",
         optionListHeading = "%nOptions:%n",
         parameterListHeading = "%nParameters:%n")
public abstract class ReusableCommandOptions implements Callable<Integer> {
    private static Logger LOG = Logger.getLogger(ReusableCommandOptions.class.getName());

    @Option(names = { "-h", "--help" }, description = "Show help for subcommand.", usageHelp = true)
    private boolean helpFlag;
    
    @Override
    public Integer call() throws Exception {
        return handleCommand();
    }
    
    public abstract int handleCommand() throws Exception;

    public void saveDisk(Disk disk) {
        try {
            // Only save if there are changes.
            if (disk.getDiskImageManager().hasChanged()) {
                LOG.fine(() -> String.format("Saving disk '%s'", disk.getFilename()));
                disk.save();
            } else {
                LOG.fine(() -> String.format("Disk '%s' has not changed; not saving.", disk.getFilename()));
            }
        } catch (IOException e) {
            LOG.severe(e.getMessage());
        }
    }
}
