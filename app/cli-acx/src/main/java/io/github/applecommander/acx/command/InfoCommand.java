package io.github.applecommander.acx.command;

import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.FormattedDisk.DiskInformation;

import io.github.applecommander.acx.base.ReadOnlyDiskImageCommandOptions;
import picocli.CommandLine.Command;

@Command(name = "info", description = "Show information on a disk image(s).",
        aliases = "i")
public class InfoCommand extends ReadOnlyDiskImageCommandOptions {
    private static Logger LOG = Logger.getLogger(InfoCommand.class.getName());
    
    @Override
    public int handleCommand() throws Exception {
        LOG.info(() -> "Path: " + disk.getFilename());
        FormattedDisk[] formattedDisks = disk.getFormattedDisks();
        for (int i = 0; i < formattedDisks.length; i++) {
            FormattedDisk formattedDisk = formattedDisks[i];
            LOG.info(() -> String.format("Disk: %s (%s)", formattedDisk.getDiskName(), formattedDisk.getFormat()));
            for (DiskInformation diskinfo : formattedDisk.getDiskInformation()) {
                System.out.printf("%s: %s\n", diskinfo.getLabel(), diskinfo.getValue());
            }
            System.out.println();
        }
        return 0;
    }
}
