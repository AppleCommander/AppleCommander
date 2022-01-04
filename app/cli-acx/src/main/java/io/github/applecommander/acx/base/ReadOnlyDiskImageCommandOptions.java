package io.github.applecommander.acx.base;

import com.webcodepro.applecommander.storage.Disk;

import io.github.applecommander.acx.converter.DiskConverter;
import picocli.CommandLine.Option;

public abstract class ReadOnlyDiskImageCommandOptions extends ReusableCommandOptions {
    @Option(names = { "-d", "--disk" }, description = "Image to process [$ACX_DISK_NAME].", required = true,
            converter = DiskConverter.class, defaultValue = "${ACX_DISK_NAME}")
    protected Disk disk;
}
