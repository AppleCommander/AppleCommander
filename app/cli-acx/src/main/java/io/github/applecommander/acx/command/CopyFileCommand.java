package io.github.applecommander.acx.command;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.FormattedDisk;

import io.github.applecommander.acx.base.ReadWriteDiskCommandOptions;
import io.github.applecommander.acx.converter.DiskConverter;
import io.github.applecommander.acx.fileutil.FileUtils;
import io.github.applecommander.filestreamer.FileStreamer;
import io.github.applecommander.filestreamer.FileTuple;
import io.github.applecommander.filestreamer.TypeOfFile;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "copy", description = "Copy files between disks.",
         aliases = { "cp" })
public class CopyFileCommand extends ReadWriteDiskCommandOptions {
    private static Logger LOG = Logger.getLogger(CopyFileCommand.class.getName());

    @Option(names = { "-r", "--recursive" }, description = "Copy files recursively.")
    private boolean recursiveFlag;
    
    @Option(names = { "-f", "--force" }, description = "Overwrite existing files.")
    private boolean overwriteFlag;
    
    @Option(names = { "--to", "--directory" }, description = "Specify which directory to place files.")
    private String targetPath;
    
    @Option(names = { "-s", "--from", "--source" }, description = "Source disk for files.", 
            converter = DiskConverter.class, required = true)
    private Disk sourceDisk;
    
    @Parameters(arity = "*", description = "File glob(s) to copy (default = '*')", 
            defaultValue = "*")
    private List<String> globs;

    @Override
    public int handleCommand() throws Exception {
        List<FileTuple> files = FileStreamer.forDisk(sourceDisk)
                .ignoreErrors(true)
                .includeTypeOfFile(TypeOfFile.BOTH)
                .recursive(recursiveFlag)
                .matchGlobs(globs)
                .stream()
                .collect(Collectors.toList());

        if (files.isEmpty()) {
            LOG.warning(() -> String.format("No matches found for %s.", String.join(",", globs)));
        } else {
            files.forEach(this::fileHandler);
        }
        return 0;
    }

    private void fileHandler(FileTuple tuple) {
        try {
            FormattedDisk formattedDisk = disk.getFormattedDisks()[0];
            if (!recursiveFlag && tuple.fileEntry.isDirectory()) {
                formattedDisk.createDirectory(tuple.fileEntry.getFilename());
            } else {
                FileUtils copier = new FileUtils(overwriteFlag);
                copier.copy(formattedDisk, tuple.fileEntry);
            }
        } catch (DiskException ex) {
            LOG.severe(ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
}
