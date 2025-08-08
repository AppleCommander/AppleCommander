package io.github.applecommander.acx.command;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.util.filestreamer.FileStreamer;
import com.webcodepro.applecommander.util.filestreamer.FileTuple;
import io.github.applecommander.acx.base.ReusableCommandOptions;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.logging.Logger;

import static picocli.CommandLine.*;

@Command(name = "find-file", description = "Scan directory and look for file(s) in every image.")
public class FindFileCommand extends ReusableCommandOptions {
    private static Logger LOG = Logger.getLogger(FindFileCommand.class.getName());

    @Option(names = { "-d", "--directory" }, description = "Directory to search.", defaultValue = ".")
    private Path directory;

    @Parameters(arity = "1..*", description = "File names or file globs to look for.")
    private List<String> fileGlobs;

    @Override
    public int handleCommand() throws Exception {
        FileVisitor visitor = new FileVisitor();
        Files.walkFileTree(directory, visitor);
        return 0;
    }

    private class FileVisitor extends SimpleFileVisitor<Path> {
        private static final PathMatcher globMatcher;
        static {
            // Build "glob:**.{do,po,dsk,...}"
            StringBuilder globs = new StringBuilder("glob:**.{");
            boolean first = true;
            for (String ext : Disk.getAllExtensions()) {
                if (!first) globs.append(",");
                ext = ext.substring(1); // skip the "." - lots of assumptions here!
                globs.append(ext);
                first = false;
            }
            globs.append("}");

            FileSystem fs = FileSystems.getDefault();
            globMatcher = fs.getPathMatcher(globs.toString());
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (globMatcher.matches(file)) {
                try {
                    Disk disk = new Disk(file.toString());
                    FileStreamer.forDisk(disk)
                        .ignoreErrors(true)
                        .beforeDisk(fd ->
                            LOG.info(() -> String.format("Scanning: %s", fd.getDiskName())))
                        .matchGlobs(fileGlobs)
                        .stream()
                        .filter(FileTuple::isFile)
                        .forEach(ft -> System.out.printf("Found: %s - %s\n",
                            ft.formattedDisk.getFilename(),
                            ft.fileEntry.getFilename()));
                } catch (Throwable t) {
                    LOG.info(() -> String.format("%s - %s", file, t.getMessage()));
                }
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
