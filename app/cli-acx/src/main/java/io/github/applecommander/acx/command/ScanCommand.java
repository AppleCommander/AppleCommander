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
import com.webcodepro.applecommander.storage.*;
import com.webcodepro.applecommander.storage.FilenameFilter;
import com.webcodepro.applecommander.storage.os.cpm.CpmFormatDisk;
import com.webcodepro.applecommander.storage.os.dos33.DosFormatDisk;
import com.webcodepro.applecommander.storage.os.gutenberg.GutenbergFormatDisk;
import com.webcodepro.applecommander.storage.os.nakedos.NakedosFormatDisk;
import com.webcodepro.applecommander.storage.os.pascal.PascalFormatDisk;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;
import com.webcodepro.applecommander.storage.os.rdos.RdosFormatDisk;
import io.github.applecommander.acx.base.ReusableCommandOptions;
import org.applecommander.device.BlockDevice;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.logging.Logger;

import static picocli.CommandLine.*;

@Command(name = "scan", description = "Scan directory and report on disks found and understood.")
public class ScanCommand extends ReusableCommandOptions {
    private static Logger LOG = Logger.getLogger(ScanCommand.class.getName());

    @Parameters(arity = "*", description = "directories to scan")
    private List<Path> directories;

    @Option(names = { "-c", "--compare-to" }, description = "Compare to existing report")
    private Path priorReportPath;

    @Option(names = { "-o", "--output" }, description = "Name of report file")
    private Path reportPath;

    @Option(names = { "--progress" }, description = "Show progress be listing each image as it is processed",
            defaultValue = "false")
    private boolean progress;

    private BiPredicate<Report,Report> degradationFn = this::degradationBySuccessFlag;
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

        FileVisitor visitor = new FileVisitor(output, progress);
        if (priorReportPath != null) {
            compare(visitor);
        }

        if (directories != null) {
            for (Path dir : directories) {
                Files.walkFileTree(dir, visitor);
            }
            showReportData(visitor.reportData);
            System.out.printf("Scanned %d disk images.\n", visitor.getCounter());
        }
        return 0;
    }

    public void compare(FileVisitor visitor) {
        try (Reader reader = new FileReader(priorReportPath.toFile())) {
            Gson gson = new GsonBuilder().create();
            JsonStreamParser parser = new JsonStreamParser(reader);
            ReportData oldData = new ReportData("Old");
            ReportData newData = new ReportData("New");
            int degradationCount = 0;
            int improvementCount = 0;
            while (parser.hasNext()) {
                Report oldReport = gson.fromJson(parser.next(), Report.class);
                Report newReport = visitor.scanFile(Path.of(oldReport.imageName));
                oldData.tallyData(oldReport);
                newData.tallyData(newReport);
                if (degradationFn.test(oldReport,newReport)) {
                    degradationCount++;
                    List<String> diffs = diffReport(oldReport, newReport);
                    if (!diffs.isEmpty()) {
                        System.out.printf("Degradation with: %s (%s)\n", oldReport.imageName, String.join(",", diffs));
                    }
                }
                else if (!oldReport.success && newReport.success) {
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
    public boolean degradationBySuccessFlag(Report oldReport, Report newReport) {
        return oldReport.success && !newReport.success;
    }
    public boolean degradationByNumbers(Report oldReport, Report newReport) {
        return (oldReport.logicalDisks > newReport.logicalDisks)
            || (oldReport.deletedFiles > newReport.deletedFiles)
            || (oldReport.directoriesVisited > newReport.directoriesVisited)
            || (oldReport.filesVisited > newReport.filesVisited)
            || (oldReport.filesRead > newReport.filesRead)
            || (oldReport.dataType.equals(newReport.dataType) && oldReport.dataRead > newReport.dataRead);
    }

    public List<String> diffReport(Report oldReport, Report newReport) {
        List<String> diffs = new ArrayList<>();
        diffBoolean(diffs, "success", r->r.success, oldReport, newReport);
        diffString(diffs, "type", r->r.imageType, oldReport, newReport);
        diffInt(diffs, "disks", r->r.logicalDisks, oldReport, newReport);
        diffInt(diffs, "deleted", r->r.deletedFiles, oldReport, newReport);
        diffInt(diffs, "dirs", r->r.directoriesVisited, oldReport, newReport);
        diffInt(diffs, "visited", r->r.filesVisited, oldReport, newReport);
        diffInt(diffs, "read", r->r.filesRead, oldReport, newReport);
        diffString(diffs, "geometry", r->r.dataType, oldReport, newReport);
        diffInt(diffs, "georead", r->r.dataRead, oldReport, newReport);
        diffInt(diffs, "errors", r->r.errors.size(), oldReport, newReport);
        return diffs;
    }
    public void diffBoolean(List<String> diffs, String title, Function<Report,Boolean> boolFn, Report oldReport, Report newReport) {
        boolean oldValue = boolFn.apply(oldReport);
        boolean newValue = boolFn.apply(newReport);
        if (oldValue != newValue) {
            diffs.add(String.format("%s %s<>%s", title, oldValue, newValue));
        }
    }
    public void diffString(List<String> diffs, String title, Function<Report,String> strFn, Report oldReport, Report newReport) {
        String oldValue = strFn.apply(oldReport);
        String newValue = strFn.apply(newReport);
        if (!Objects.equals(oldValue, newValue)) {
            diffs.add(String.format("%s '%s'<>'%s'", title, oldValue, newValue));
        }
    }
    public void diffInt(List<String> diffs, String title, Function<Report,Integer> intFn, Report oldReport, Report newReport) {
        int oldValue = intFn.apply(oldReport);
        int newValue = intFn.apply(newReport);
        if (oldValue != newValue) {
            diffs.add(String.format("%s %d<>%d", title, oldValue, newValue));
        }
    }

    public void showReportData(ReportData... data) {
        System.out.println();
        showString("Title", ReportData::getTitle, data);
        showInteger("Total Images", ReportData::getReportCount, data);
        showInteger("Successes", ReportData::getSuccesses, data);
        showCounts("Image Types", ReportData::getImageTypes, data);
        showInteger("Logical Disks", ReportData::getLogicalDisks, data);
        showInteger("Deleted Files", ReportData::getDeletedFiles, data);
        showInteger("Directories Visited", ReportData::getDirectoriesVisited, data);
        showInteger("Files Visited", ReportData::getFilesVisited, data);
        showInteger("Files Read", ReportData::getFilesRead, data);
        showCounts("Data Types Read", ReportData::getDataTypesRead, data);
        showInteger("Error Count", ReportData::getErrorCount, data);
        System.out.println();
    }
    private void showString(String heading, Function<ReportData,String> stringFn, ReportData... data) {
        System.out.printf("%-20s ", heading);
        for (ReportData r : data) {
            System.out.printf("%10s ", stringFn.apply(r));
        }
        System.out.println();
    }
    private void showInteger(String heading, Function<ReportData,Integer> intFn, ReportData... data) {
        System.out.printf("%-20s ", heading);
        for (ReportData r : data) {
            System.out.printf("%10d ", intFn.apply(r));
        }
        System.out.println();
    }
    private void showCounts(String heading, Function<ReportData,Map<String,Integer>> mapFn, ReportData... data) {
        System.out.println(heading);
        Set<String> keys = new TreeSet<>();
        for (ReportData r : data) {
            Map<String,Integer> map = mapFn.apply(r);
            keys.addAll(map.keySet());
        }
        for (String key : keys) {
            System.out.printf("* %-18s ", key);
            for (ReportData r : data) {
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
            for (String ext : FilenameFilter.getAllExtensions()) {
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
        private final boolean progress;
        private ReportData reportData = new ReportData("Scan");

        public FileVisitor(PrintStream output, boolean progress) {
            this.output = output;
            this.progress = progress;
        }

        public int getCounter() {
            return counter;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (globMatcher.matches(file)) {
                counter++;
                if (progress) {
                    System.out.printf("#%05d: %s\n", counter, file.toString());
                }
                Report report = scanFile(file);
                reportData.tallyData(report);
                output.println(gson.toJson(report));
            }
            return FileVisitResult.CONTINUE;
        }

        public Report scanFile(Path file) {
            try {
                return new Report(file);
            } catch (Throwable t) {
                return new Report(file, t);
            }
        }
    }

    public static class ReportData {
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

        ReportData(String title) {
            this.title = title;
        }

        void tallyData(Report report) {
            reportCount++;
            if (report.success) successes++;
            imageTypes.merge(report.imageType, 1, Integer::sum);
            logicalDisks += report.logicalDisks;
            deletedFiles += report.deletedFiles;
            directoriesVisited += report.directoriesVisited;
            filesVisited += report.filesVisited;
            filesRead += report.filesRead;
            dataTypesRead.merge(report.dataType, report.dataRead, Integer::sum);
            errorCount += report.errors.size();
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
    }

    public static class Report {
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

        /** A failure report. */
        Report(Path file, Throwable t) {
            this.imageName = file.toString();
            this.errors.add(t.getMessage());
        }

        Report(Path file) {
            try {
                imageName = file.toString();
                Source source = Sources.create(imageName).orElseThrow();
                DiskFactory.Context ctx = Disks.inspect(source);
                for (FormattedDisk fdisk : ctx.disks) {
                    logicalDisks++;
                    imageType = fdisk.getFormat();
                    readAllFiles(fdisk);
                    // Read all data
                    switch (fdisk) {
                        case CpmFormatDisk cpm -> readAllCPMBlocks(cpm);
                        case DosFormatDisk dos -> readAllSectors(dos);
                        case GutenbergFormatDisk gutenberg -> readAllSectors(gutenberg);
                        case NakedosFormatDisk nakedos -> readAllSectors(nakedos);
                        case PascalFormatDisk pascal -> readAllBlocks(pascal);
                        case ProdosFormatDisk prodos -> readAllBlocks(prodos);
                        case RdosFormatDisk rdos -> readAllRDOSBlocks(rdos);
                        default -> throw new RuntimeException("Unexpected disk type: " + fdisk.getFormat());
                    }
                }
                success = errors.isEmpty();
            } catch (Throwable t) {
                success = false;
                String msg = t.getMessage();
                if (msg == null) msg = t.getClass().getName();
                errors.add(msg);
            }
        }

        /** Read all files, capturing errors. */
        private void readAllFiles(DirectoryEntry dir) {
            directoriesVisited++;
            List<FileEntry> files;
            try {
                files = dir.getFiles();
            } catch (Throwable t) {
                errors.add(String.format("Unable to read directory %d/%s", dir.getFormattedDisk().getLogicalDiskNumber(),
                        dir.getDirname()));
                return;
            }
            for (FileEntry fe : files) {
                if (errors.size() > MAX_ERRORS) return;
                if (fe.isDeleted()) {
                    deletedFiles++;
                } else if (fe instanceof DirectoryEntry directoryEntry) {
                    readAllFiles(directoryEntry);
                } else {
                    filesVisited++;
                    try {
                        if (fe.getFileData() != null) {
                            filesRead++;
                        }
                    } catch (Throwable t) {
                        errors.add(String.format("Unable to read file %d/%s/%s: %s", dir.getFormattedDisk().getLogicalDiskNumber(),
                                dir.getDirname(), fe.getFilename(), t.getMessage()));
                    }
                }
            }
        }

        private void readAllCPMBlocks(CpmFormatDisk cpm) {
            dataType = "CPM blocks";
            // This adjusts for the start. The CPM filesystem ignores the first 3 tracks on disk.
            int blocksToRead = cpm.getBitmapLength() -
                    (CpmFormatDisk.PHYSICAL_BLOCK_TRACK_START * CpmFormatDisk.CPM_BLOCKS_PER_TRACK);
            for (int b=0; b<blocksToRead && errors.size() < MAX_ERRORS; b++) {
                try {
                    cpm.readCpmBlock(b);
                    dataRead++;
                } catch (Throwable t) {
                    errors.add(String.format("Unable to read CPM block #%d for disk #%d", b, cpm.getLogicalDiskNumber()));
                }
            }
        }

        private void readAllBlocks(FormattedDisk fdisk) {
            BlockDevice device = BlockDeviceAdapter.from(fdisk);
            dataType = "blocks";
            for (int b = 0; b < fdisk.getBitmapLength() && errors.size() < MAX_ERRORS; b++) {
                try {
                    device.readBlock(b);
                    dataRead++;
                } catch (Throwable t) {
                    errors.add(String.format("Unable to read block #%d for disk #%d", b, fdisk.getLogicalDiskNumber()));
                }
            }
        }

        private void readAllRDOSBlocks(RdosFormatDisk rdos) {
            dataType = "RDOS blocks";
            for (int b = 0; b < rdos.getBitmapLength() && errors.size() < MAX_ERRORS; b++) {
                try {
                    rdos.readRdosBlock(b);
                    dataRead++;
                } catch (Throwable t) {
                    errors.add(String.format("Unable to read RDOS block #%d for disk #%d", b, rdos.getLogicalDiskNumber()));
                }
            }
        }

        private void readAllSectors(FormattedDisk fdisk) {
            TrackSectorDevice device = TrackSectorDeviceAdapter.from(fdisk);
            dataType = "sectors";
            int[] dims = fdisk.getBitmapDimensions();
            for (int track = 0; track < dims[0] && errors.size() < MAX_ERRORS; track++) {
                for (int sector = 0; sector < dims[1] && errors.size() < MAX_ERRORS; sector++) {
                    try {
                        device.readSector(track, sector);
                        dataRead++;
                    } catch (Throwable t) {
                        errors.add(String.format("Unable to read sector T%d,S%s for disk #%d",
                                track, sector, fdisk.getLogicalDiskNumber()));
                    }
                }
            }
        }
    }
}
