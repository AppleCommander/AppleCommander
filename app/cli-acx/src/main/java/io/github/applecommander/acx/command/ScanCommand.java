package io.github.applecommander.acx.command;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.os.rdos.RdosFormatDisk;
import io.github.applecommander.acx.base.ReusableCommandOptions;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static picocli.CommandLine.*;

@Command(name = "scan", description = "Scan directory and report on disks found and understood.")
public class ScanCommand extends ReusableCommandOptions {
    private static Logger LOG = Logger.getLogger(ScanCommand.class.getName());

    @Parameters(arity = "*", description = "directories to scan", defaultValue = ".")
    private List<Path> directories;

    @Option(names = { "-o", "--output" }, description = "Name of report file", defaultValue = "report.txt")
    private Path reportPath;

    @Option(names = "--json", description = "Save format in JSON format; changes default to report.json.")
    private boolean jsonFormat;
    private static Gson gson = new Gson();

    @Override
    public int handleCommand() throws Exception {
        FileVisitor visitor = new FileVisitor();
        for (Path dir : directories) {
            Files.walkFileTree(dir, visitor);
        }
        LOG.info(() -> String.format("Scanned %d files.", visitor.getReports().size()));
        if (jsonFormat) {
            JsonArray reports = new JsonArray();
            visitor.getReports().forEach(r -> {
                JsonObject report = new JsonObject();
                report.addProperty("imageName", r.imageName().toString());
                report.addProperty("success", r.success());
                report.addProperty("imageType", r.imageType());
                report.addProperty("deletedFiles", r.deletedFiles());
                report.addProperty("directoriesVisited", r.directoriesVisited());
                report.addProperty("logicalDisks", r.logicalDisks());
                report.addProperty("filesVisited", r.filesVisited());
                report.addProperty("filesRead", r.filesRead());
                report.addProperty("dataRead", r.dataRead());
                report.addProperty("errorText", r.errorText());
                reports.add(report);
            });
            System.out.println(gson.toJson(reports));
        }
        else {
            visitor.getReports().forEach(System.out::println);
        }
        return 0;
    }

    private static class FileVisitor extends SimpleFileVisitor<Path> {
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

        private final List<Report> reports = new ArrayList<>();

        public List<Report> getReports() {
            return reports;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (globMatcher.matches(file)) {
                reports.add(scanFile(file));
            }
            return FileVisitResult.CONTINUE;
        }

        private Report scanFile(Path file) {
            boolean success = false;
            String imageType = "unknown";
            int logicalDisks = 0;
            int deletedFiles = 0;
            int directoriesVisited = 0;
            int filesVisited = 0;
            int filesRead = 0;
            int dataRead = 0;
            String message = "-";

            try {
                Disk disk = new Disk(file.toString());
                for (FormattedDisk fdisk : disk.getFormattedDisks()) {
                    logicalDisks++;
                    imageType = fdisk.getFormat();
                    // Read all files
                    for (FileEntry fe : fdisk.getFiles()) {
                        if (fe.isDeleted()) {
                            deletedFiles++;
                        }
                        else if (fe.isDirectory()) {
                            directoriesVisited++;
                        }
                        else {
                            filesVisited++;
                            if (fe.getFileData() != null) {
                                filesRead++;
                            }
                        }
                    }
                    // Read all data
                    if (fdisk instanceof RdosFormatDisk rdos) {
                        for (int b=0; b<rdos.getBitmapLength(); b++) {
                            rdos.readRdosBlock(b);
                            dataRead++;
                        }
                    }
                    else if (fdisk.getBitmapDimensions() == null) {
                        for (int b=0; b<fdisk.getBitmapLength(); b++) {
                            fdisk.readBlock(b);
                            dataRead++;
                        }
                    }
                    else {
                        int[] dims = fdisk.getBitmapDimensions();
                        for (int t=0; t<dims[0]; t++) {
                            for (int s=0; s<dims[1]; s++) {
                                fdisk.readSector(t,s);
                                dataRead++;
                            }
                        }
                    }
                }
                success = true;
            } catch (Throwable t) {
                success = false;
                message = t.getMessage();
            }
            return new Report(file, success, imageType,
                    logicalDisks, deletedFiles, directoriesVisited,
                    filesVisited, filesRead, dataRead,
                    message);
        }
    }

    private record Report(Path imageName, boolean success, String imageType,
                          int logicalDisks, int deletedFiles, int directoriesVisited,
                          int filesVisited, int filesRead, int dataRead,
                          String errorText) {

    }
}
