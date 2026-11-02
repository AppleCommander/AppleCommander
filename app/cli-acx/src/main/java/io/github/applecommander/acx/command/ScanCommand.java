/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2025 by Robert Greene and others
 * robgreene at users.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package io.github.applecommander.acx.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonStreamParser;
import io.github.applecommander.acx.base.ReusableCommandOptions;
import org.applecommander.device.BlockDevice;
import org.applecommander.device.Device;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.filestore.*;
import org.applecommander.filestore.FileStore;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;
import org.applecommander.util.FileExtensions;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.logging.Logger;

import static picocli.CommandLine.*;

@Command(name = "scan", description = { "Scan directory and report on disks found and understood.", "", """
                This subcommand is intended to assist in identifying either discrepancies between
                differing versions of AppleCommander and/or to locate likely candidates for investigation
                if that are not 'understood' (either by the discovery mechanism or by the image
                implementation itself). Finally, it *might* be useful to discover actual 'bad' images
                in a collection.
                """ })
public class ScanCommand extends ReusableCommandOptions {
    private static final Logger LOG = Logger.getLogger(ScanCommand.class.getName());

    @Parameters(arity = "*", description = "directories to scan")
    private List<Path> directories;

    @Option(names = { "-c", "--compare-to" }, description = "Compare to existing report")
    private Path priorReportPath;

    @Option(names = { "-o", "--output" }, description = "Name of report file")
    private Path reportPath;

    @Option(names = { "--progress" }, description = "Show progress by listing each image as it is processed",
            defaultValue = "false")
    private boolean progress;

    @Option(names = { "--strict" }, description = "Make all blocks/sectors failures an error", defaultValue = "false")
    private boolean strict;

    @Option(names = { "--quiet" }, description = "Suppress summary output and just output the JSON stream.", defaultValue = "false")
    private boolean quiet;

    private BiPredicate<ImageReport, ImageReport> degradationFn = this::degradationBySuccessFlag;
    @Option(names = { "--numbers" }, description = "Compare reports by numbers instead of success flag")
    private void detectDegradationByNumbers(boolean flag) {
        degradationFn = this::degradationByNumbers;
    }

    @Override
    public int handleCommand() throws Exception {
        PrintStream output = System.out;
        if (reportPath != null) {
            output = new PrintStream(Files.newOutputStream(reportPath));
        }

        FileVisitor visitor = new FileVisitor(output, progress, strict);
        if (priorReportPath != null) {
            compare(visitor);
        }

        if (directories != null) {
            for (Path dir : directories) {
                Files.walkFileTree(dir, visitor);
            }
            if (!quiet) {
                showReportData(visitor.reportSummary);
                System.out.printf("Scanned %d disk images.\n", visitor.getCounter());
            }
        }
        return 0;
    }

    public void compare(FileVisitor visitor) {
        try (Reader reader = new FileReader(priorReportPath.toFile())) {
            Gson gson = new GsonBuilder().create();
            JsonStreamParser parser = new JsonStreamParser(reader);
            ReportSummary oldData = new ReportSummary("Old");
            ReportSummary newData = new ReportSummary("New");
            int degradationCount = 0;
            int improvementCount = 0;
            while (parser.hasNext()) {
                ImageReport oldImageReport = gson.fromJson(parser.next(), ImageReport.class);
                oldImageReport.fixup();
                if (progress) System.out.printf("Scanning: '%s'\n", oldImageReport.imageName);
                ImageReport newImageReport = visitor.scanFile(Path.of(oldImageReport.imageName));
                oldData.tallyData(oldImageReport);
                newData.tallyData(newImageReport);
                if (degradationFn.test(oldImageReport, newImageReport)) {
                    degradationCount++;
                    List<String> diffs = diffReport(oldImageReport, newImageReport);
                    if (!diffs.isEmpty()) {
                        System.out.printf("Degradation with: %s (%s)\n", oldImageReport.imageName, String.join(",", diffs));
                    }
                }
                else if (!oldImageReport.success && newImageReport.success) {
                    improvementCount++;
                }
            }
            System.out.println();
            System.out.printf("Recognition degraded by %d disks and improved by %d disks.\n", degradationCount, improvementCount);
            System.out.println();
            showReportData(oldData, newData);
        } catch (IOException e) {
            LOG.severe(e.getMessage());
        }
    }
    public boolean degradationBySuccessFlag(ImageReport oldImageReport, ImageReport newImageReport) {
        return oldImageReport.success && !newImageReport.success;
    }
    public boolean degradationByNumbers(ImageReport oldImageReport, ImageReport newImageReport) {
        return (oldImageReport.logicalDisks > newImageReport.logicalDisks)
            || (oldImageReport.deletedFiles > newImageReport.deletedFiles)
            || (oldImageReport.directoriesVisited > newImageReport.directoriesVisited)
            || (oldImageReport.filesVisited > newImageReport.filesVisited)
            || (oldImageReport.filesRead > newImageReport.filesRead)
            || (oldImageReport.dataType.equals(newImageReport.dataType) && oldImageReport.dataRead > newImageReport.dataRead);
    }

    public List<String> diffReport(ImageReport oldImageReport, ImageReport newImageReport) {
        List<String> diffs = new ArrayList<>();
        diffBoolean(diffs, "success", r->r.success, oldImageReport, newImageReport);
        diffString(diffs, "type", r->r.imageType, oldImageReport, newImageReport);
        diffInt(diffs, "disks", r->r.logicalDisks, oldImageReport, newImageReport);
        diffInt(diffs, "deleted", r->r.deletedFiles, oldImageReport, newImageReport);
        diffInt(diffs, "dirs", r->r.directoriesVisited, oldImageReport, newImageReport);
        diffInt(diffs, "visited", r->r.filesVisited, oldImageReport, newImageReport);
        diffInt(diffs, "read", r->r.filesRead, oldImageReport, newImageReport);
        diffString(diffs, "geometry", r->r.dataType, oldImageReport, newImageReport);
        diffInt(diffs, "georead", r->r.dataRead, oldImageReport, newImageReport);
        diffInt(diffs, "errors", r->r.errors.size(), oldImageReport, newImageReport);
        return diffs;
    }
    public void diffBoolean(List<String> diffs, String title, Function<ImageReport,Boolean> boolFn, ImageReport oldImageReport, ImageReport newImageReport) {
        boolean oldValue = boolFn.apply(oldImageReport);
        boolean newValue = boolFn.apply(newImageReport);
        if (oldValue != newValue) {
            diffs.add(String.format("%s %s<>%s", title, oldValue, newValue));
        }
    }
    public void diffString(List<String> diffs, String title, Function<ImageReport,String> strFn, ImageReport oldImageReport, ImageReport newImageReport) {
        String oldValue = strFn.apply(oldImageReport);
        String newValue = strFn.apply(newImageReport);
        if (!Objects.equals(oldValue, newValue)) {
            diffs.add(String.format("%s '%s'<>'%s'", title, oldValue, newValue));
        }
    }
    public void diffInt(List<String> diffs, String title, Function<ImageReport,Integer> intFn, ImageReport oldImageReport, ImageReport newImageReport) {
        int oldValue = intFn.apply(oldImageReport);
        int newValue = intFn.apply(newImageReport);
        if (oldValue != newValue) {
            diffs.add(String.format("%s %d<>%d", title, oldValue, newValue));
        }
    }

    public void showReportData(ReportSummary... data) {
        System.out.println();
        showString("Title", ReportSummary::getTitle, data);
        showInteger("Total Images", ReportSummary::getReportCount, data);
        showInteger("Successes", ReportSummary::getSuccesses, data);
        showCounts("Image Types", ReportSummary::getImageTypes, data);
        showInteger("Logical Disks", ReportSummary::getLogicalDisks, data);
        showInteger("Deleted Files", ReportSummary::getDeletedFiles, data);
        showInteger("Directories Visited", ReportSummary::getDirectoriesVisited, data);
        showInteger("Files Visited", ReportSummary::getFilesVisited, data);
        showInteger("Files Read", ReportSummary::getFilesRead, data);
        showCounts("Data Types Read", ReportSummary::getDataTypesRead, data);
        showInteger("Error Count", ReportSummary::getErrorCount, data);
        showInteger("Warning Count", ReportSummary::getWarningCount, data);
        System.out.println();
    }
    private void showString(String heading, Function<ReportSummary,String> stringFn, ReportSummary... data) {
        System.out.printf("%-20s ", heading);
        for (ReportSummary r : data) {
            System.out.printf("%10s ", stringFn.apply(r));
        }
        System.out.println();
    }
    private void showInteger(String heading, Function<ReportSummary,Integer> intFn, ReportSummary... data) {
        System.out.printf("%-20s ", heading);
        for (ReportSummary r : data) {
            System.out.printf("%10d ", intFn.apply(r));
        }
        System.out.println();
    }
    private void showCounts(String heading, Function<ReportSummary,Map<String,Integer>> mapFn, ReportSummary... data) {
        System.out.println(heading);
        Set<String> keys = new TreeSet<>();
        for (ReportSummary r : data) {
            Map<String,Integer> map = mapFn.apply(r);
            keys.addAll(map.keySet());
        }
        for (String key : keys) {
            System.out.printf("* %-18s ", key);
            for (ReportSummary r : data) {
                Map<String,Integer> map = mapFn.apply(r);
                System.out.printf("%10d ", map.getOrDefault(key, 0));
            }
            System.out.println();
        }
    }

    public static class FileVisitor extends SimpleFileVisitor<Path> {
        private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        private static final PathMatcher globMatcher;
        static {
            // Build "glob:**.{do,po,dsk,...}"
            StringBuilder globs = new StringBuilder("glob:**.{");
            boolean first = true;
            for (String ext : FileExtensions.ALL_IMAGE_EXTENSIONS) {
                if (!first) globs.append(",");
                ext = ext.substring(1); // skip the "." - lots of assumptions here!
                // Unix is case-sensitive, so we need to make the pattern case-insensitive (yuck)
                for (char ch : ext.toCharArray()) {
                    globs.append("[");
                    globs.append(Character.toLowerCase(ch));
                    globs.append(Character.toUpperCase(ch));
                    globs.append("]");
                }
                first = false;
            }
            globs.append("}");

            FileSystem fs = FileSystems.getDefault();
            globMatcher = fs.getPathMatcher(globs.toString());
        }

        private int counter;
        private final PrintStream output;
        private final boolean strict;
        private final boolean progress;
        private final ReportSummary reportSummary = new ReportSummary("Scan");

        public FileVisitor(PrintStream output, boolean progress, boolean strict) {
            this.output = output;
            this.progress = progress;
            this.strict = strict;
        }

        public int getCounter() {
            return counter;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (globMatcher.matches(file)) {
                counter++;
                if (progress) {
                    System.out.printf("#%05d: %s\n", counter, file.toString());
                }
                ImageReport imageReport = scanFile(file);
                reportSummary.tallyData(imageReport);
                output.println(gson.toJson(imageReport));
            }
            return FileVisitResult.CONTINUE;
        }

        public ImageReport scanFile(Path file) {
            try {
                return new ImageReport(file, strict);
            } catch (Throwable t) {
                return new ImageReport(file, t);
            }
        }
    }

    public static class ReportSummary {
        final String title;
        int reportCount = 0;
        int successes = 0;
        Map<String,Integer> imageTypes = new HashMap<>();
        int logicalDisks = 0;
        int deletedFiles = 0;
        int directoriesVisited = 0;
        int filesVisited = 0;
        int filesRead = 0;
        Map<String,Integer> dataTypesRead = new HashMap<>();
        int errorCount = 0;
        int warningCount = 0;

        ReportSummary(String title) {
            this.title = title;
        }

        void tallyData(ImageReport imageReport) {
            // Patch data where we were counting unrecognized disks as successful because there were no errors...
            if ("unknown".equals(imageReport.imageType)) imageReport.success = false;

            reportCount++;
            if (imageReport.success) successes++;
            imageTypes.merge(imageReport.imageType, 1, Integer::sum);
            logicalDisks += imageReport.logicalDisks;
            deletedFiles += imageReport.deletedFiles;
            directoriesVisited += imageReport.directoriesVisited;
            filesVisited += imageReport.filesVisited;
            filesRead += imageReport.filesRead;
            dataTypesRead.merge(imageReport.dataType, imageReport.dataRead, Integer::sum);
            errorCount += imageReport.errors.size();
            warningCount += imageReport.warnings.size();
        }

        public String getTitle() {
            return title;
        }
        public int getReportCount() {
            return reportCount;
        }
        public int getSuccesses() {
            return successes;
        }
        public Map<String, Integer> getImageTypes() {
            return imageTypes;
        }
        public int getLogicalDisks() {
            return logicalDisks;
        }
        public int getDeletedFiles() {
            return deletedFiles;
        }
        public int getDirectoriesVisited() {
            return directoriesVisited;
        }
        public int getFilesVisited() {
            return filesVisited;
        }
        public int getFilesRead() {
            return filesRead;
        }
        public Map<String, Integer> getDataTypesRead() {
            return dataTypesRead;
        }
        public int getErrorCount() {
            return errorCount;
        }
        public int getWarningCount() {
            return warningCount;
        }
    }

    public static class ImageReport {
        static final int MAX_ERRORS = 20;

        String imageName;
        boolean success = false;
        String imageType = "unknown";
        int logicalDisks = 0;
        int deletedFiles = 0;
        int directoriesVisited = 0;
        int filesVisited = 0;
        int filesRead = 0;
        String dataType = "unknown";
        int dataRead = 0;
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        /** A failure report. */
        ImageReport(Path file, Throwable t) {
            this.imageName = file.toString();
            this.errors.add(t.getMessage());
        }

        ImageReport(Path file, boolean strict) {
            try {
                imageName = file.toString();
                Source source = Sources.create(imageName).orElseThrow();
                FileStoreFactory.Context ctx = FileStores.inspect(source);
                for (FileStore fileStore : ctx.fileStores) {
                    logicalDisks++;
                    imageType = fileStore.getType();
                    readAllFiles(fileStore.getRootDirectory());
                    // Read all data
                    var list = strict ? errors : warnings;
                    // Read a device in its entirety
                    Optional<Device> opt = fileStore.get(Device.class);
                    opt.ifPresent(device -> {
                        if (device instanceof BlockDevice blockDevice) {
                            readAllBlocks(blockDevice, list);
                        }
                        else if (device instanceof TrackSectorDevice trackSectorDevice) {
                            readAllSectors(trackSectorDevice, list);
                        }
                        else {
                            dataType = "N/A";
                        }
                    });
                }
                // No errors found -AND- we identified at least one disk
                success = errors.isEmpty() && !ctx.fileStores.isEmpty();
            } catch (Throwable t) {
                success = false;
                String msg = t.getMessage();
                if (msg == null) msg = t.getClass().getName();
                errors.add(msg);
            }
        }

        void fixup() {
            // Gson doesn't seem to have the ability to create an empty list for a non-existent field, so we do it ourselves!
            if (errors == null) errors = new ArrayList<>();
            if (warnings == null) warnings = new ArrayList<>();
        }

        /** Read all files, capturing errors. */
        private void readAllFiles(Directory dir) {
            directoriesVisited++;
            List<FileEntry> files;
            try {
                files = dir.getFiles();
            } catch (Throwable t) {
                errors.add(String.format("Unable to read directory %s/%s", dir.getFileStore().getLabel(),
                        dir.getName()));
                return;
            }
            for (FileEntry file : files) {
                if (errors.size() > MAX_ERRORS) return;
                if (file.isDeleted()) {
                    deletedFiles++;
                } else {
                    file.get(Directory.class).ifPresentOrElse(this::readAllFiles, () -> {
                        filesVisited++;
                        try {
                            if (file.getDataFork() != null) {
                                filesRead++;
                            }
                        } catch (Throwable t) {
                            errors.add(String.format("Unable to read file %s/%s/%s: %s", dir.getFileStore().getLabel(),
                                    dir.getName(), file.getName(), t.getMessage()));
                        }
                    });
                }
            }
        }

        private void readAllBlocks(BlockDevice device, List<String> list) {
            dataType = "blocks";
            for (int b = 0; b < device.getGeometry().blocksOnDevice() && list.size() < MAX_ERRORS; b++) {
                try {
                    device.readBlock(b);
                    dataRead++;
                } catch (Throwable t) {
                    list.add(String.format("Unable to read block #%d", b));
                }
            }
        }
        private void readAllSectors(TrackSectorDevice device, List<String> list) {
            dataType = "sectors";
            for (int track = 0; track < device.getGeometry().tracksOnDisk() && list.size() < MAX_ERRORS; track++) {
                for (int sector = 0; sector < device.getGeometry().sectorsPerTrack() && list.size() < MAX_ERRORS; sector++) {
                    try {
                        device.readSector(track, sector);
                        dataRead++;
                    } catch (Throwable t) {
                        list.add(String.format("Unable to read sector T%d,S%s", track, sector));
                    }
                }
            }
        }
    }
}
