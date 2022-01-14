package io.github.applecommander.acx.base;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.webcodepro.applecommander.util.filestreamer.FileStreamer;
import com.webcodepro.applecommander.util.filestreamer.FileTuple;
import com.webcodepro.applecommander.util.filestreamer.TypeOfFile;

import picocli.CommandLine.Parameters;

public abstract class ReadWriteDiskCommandWithGlobOptions extends ReadWriteDiskCommandOptions {
    private static Logger LOG = Logger.getLogger(ReadWriteDiskCommandWithGlobOptions.class.getName());

    @Parameters(arity = "1..*", description = "File glob(s) to unlock (default = '*') - be cautious of quoting!")
    private List<String> globs = Arrays.asList("*");

    @Override
    public int handleCommand() throws Exception {
        List<FileTuple> files = FileStreamer.forDisk(disk)
			        .ignoreErrors(true)
			        .includeTypeOfFile(TypeOfFile.FILE)
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
    
    public abstract void fileHandler(FileTuple tuple);
}
