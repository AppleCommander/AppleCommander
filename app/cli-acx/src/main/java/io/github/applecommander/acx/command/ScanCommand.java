package io.github.applecommander.acx.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.os.cpm.CpmFormatDisk;
import com.webcodepro.applecommander.storage.os.dos33.DosFormatDisk;
import com.webcodepro.applecommander.storage.os.gutenberg.GutenbergFormatDisk;
import com.webcodepro.applecommander.storage.os.nakedos.NakedosFormatDisk;
import com.webcodepro.applecommander.storage.os.pascal.PascalFormatDisk;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;
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

    @Override
    public int handleCommand() throws Exception {
        FileVisitor visitor = new FileVisitor();
        for (Path dir : directories) {
            Files.walkFileTree(dir, visitor);
        }
        LOG.info(() -> String.format("Scanned %d files.", visitor.getReports().size()));
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(visitor.getReports()));
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
            try {
                return new Report(file);
            } catch (Throwable t) {
                return new Report(file, t);
            }
        }
    }

    private static class Report {
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
                Disk disk = new Disk(file.toString());
                for (FormattedDisk fdisk : disk.getFormattedDisks()) {
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
                } else if (fe.isDirectory()) {
                    readAllFiles(dir);
                } else {
                    filesVisited++;
                    try {
                        if (fe.getFileData() != null) {
                            filesRead++;
                        }
                    } catch (Throwable t) {
                        errors.add(String.format("Unable to read file %d/%s/%s", dir.getFormattedDisk().getLogicalDiskNumber(),
                                dir.getDirname(), fe.getFilename()));
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
            dataType = "blocks";
            for (int b = 0; b < fdisk.getBitmapLength() && errors.size() < MAX_ERRORS; b++) {
                try {
                    fdisk.readBlock(b);
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
            dataType = "sectors";
            int[] dims = fdisk.getBitmapDimensions();
            for (int track = 0; track < dims[0] && errors.size() < MAX_ERRORS; track++) {
                for (int sector = 0; sector < dims[1] && errors.size() < MAX_ERRORS; sector++) {
                    try {
                        fdisk.readSector(track, sector);
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
