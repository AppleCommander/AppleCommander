package io.github.applecommander.acx.command;

import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.os.pascal.PascalFormatDisk;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;

import io.github.applecommander.acx.base.ReadWriteDiskCommandOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "rename-disk", description = "Rename volume of a disk image.")
public class RenameDiskCommand extends ReadWriteDiskCommandOptions {
    private static Logger LOG = Logger.getLogger(RenameDiskCommand.class.getName());
    
    @Parameters(description = "Disk name.")
    private String diskName;

    @Override
    public int handleCommand() throws Exception {
		FormattedDisk[] formattedDisks = disk.getFormattedDisks();
		FormattedDisk formattedDisk = formattedDisks[0];
    	if (formattedDisk instanceof ProdosFormatDisk || formattedDisk instanceof PascalFormatDisk) {
			formattedDisk.setDiskName(diskName);
	    	return 0;
    	} else {
    		LOG.warning("Disk must be ProDOS or Pascal.");
    		return 1;
    	}
    }
}
