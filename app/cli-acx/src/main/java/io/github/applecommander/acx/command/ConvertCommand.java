package io.github.applecommander.acx.command;

import java.io.File;

import com.webcodepro.applecommander.storage.Disk;

import io.github.applecommander.acx.base.ReusableCommandOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "convert", description = {
            "Uncompress a ShrinkIt or Binary II file; ",
            "or convert a DiskCopy 4.2 image into a ProDOS disk image." })
public class ConvertCommand extends ReusableCommandOptions {
    @Option(names = { "-d", "--disk" }, description = "Image to create [$ACX_DISK_NAME].", required = true,
            defaultValue = "${ACX_DISK_NAME}")
    private String diskName;
    
    @Option(names = { "-f", "--force" }, description = "Allow existing disk image to be replaced.")
    private boolean overwriteFlag;

    @Parameters(description = "Archive to convert.", arity = "1")
    private String archiveName;

    @Override
    public int handleCommand() throws Exception {
        File targetFile = new File(diskName);
        if (targetFile.exists() && !overwriteFlag) {
            throw new RuntimeException("File exists and overwriting not enabled.");
        }
        
        Disk disk = new Disk(archiveName);
        disk.setFilename(diskName);
        saveDisk(disk);
                    
        return 0;
    }
}
