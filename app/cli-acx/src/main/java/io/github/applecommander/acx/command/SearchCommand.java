/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2026 by Robert Greene and others
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

import com.webcodepro.applecommander.storage.*;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFileEntry;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;
import io.github.applecommander.acx.base.ReusableCommandOptions;
import io.github.applecommander.acx.converter.IntegerTypeConverter;
import org.applecommander.source.Source;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import static picocli.CommandLine.*;

@Command(name = "search", description = { "Search archives for a file.", "", """
            This is akin to the Unix 'find' command but twisted to support the Apple II.
            Note that all options are ANDed. The all options default to equals but can be
            prefixed with comparisons ('<', '<=', '>', '>=', '<>', '!=').
            Number value support allows the '$' or '0x' prefix for hex numbers.
            Warning: Shell quoting will mess up the '$'. Double check that!
            """ })
public class SearchCommand extends ReusableCommandOptions {
    private static final IntegerTypeConverter integerConverter = new IntegerTypeConverter();
    @Parameters(arity = "*", description = "Directories to scan.", paramLabel = "DIRECTORY")
    private List<Path> directories;

    @Option(names = "--quiet", description = "Suppress error messages.")
    private boolean suppressErrorMessages;

    @Option(names = "--name", description = "Filename (case insensitive).", paramLabel = "FILENAME")
    private void applyNameFilter(String filename) {
        Objects.requireNonNull(filename);
        addCriteria(String.CASE_INSENSITIVE_ORDER, filename, s -> s, FileEntry::getFilename);
    }
    @Option(names = "--size", description = "File size.", paramLabel = "SIZE")
    private void applySizeFilter(String[] filesizes) {
        Objects.requireNonNull(filesizes);
        for (String filesize : filesizes) {
            addCriteria(Integer::compare, filesize, integerConverter::convert, FileEntry::getSize);
        }
    }
    @Option(names = "--type", description = "File type.", paramLabel = "TYPE")
    private void applyTypeFilter(String filetype) {
        Objects.requireNonNull(filetype);
        addCriteria(String.CASE_INSENSITIVE_ORDER, filetype, s -> s, FileEntry::getFiletype);
    }
    @Option(names = "--aux-type", description = "Aux. type", paramLabel = "AUXTYPE")
    private void applyAuxTypeFilter(String auxtype) {
        Objects.requireNonNull(auxtype);
        addCriteria(Integer::compare, auxtype, integerConverter::convert, fileEntry -> {
            if (fileEntry instanceof ProdosFileEntry prodos) {
                return prodos.getAuxiliaryType();
            }
            return -1;
        });
    }
    @Option(names = { "--addr", "--address" }, description = "Address", paramLabel = "ADDR")
    private void applyAddressFilter(String address) {
        Objects.requireNonNull(address);
        addCriteria(Integer::compare, address, integerConverter::convert, FileEntry::getAddress);
    }

    private Predicate<FileEntry> criteria = e -> true;
    private <T> void addCriteria(Comparator<T> comparator, String inputValue, Function<String,T> inputValueFn, Function<FileEntry,T> actualValueFn) {
        Objects.requireNonNull(comparator);
        Objects.requireNonNull(inputValue);
        Objects.requireNonNull(inputValueFn);
        Objects.requireNonNull(actualValueFn);
        // Identify operator
        List<String> prefixes = List.of("<=", ">=", "<>", "!=", "<", ">");
        String operator = "==";
        for (String prefix : prefixes) {
            if (inputValue.startsWith(prefix)) {
                operator = prefix;
                inputValue = inputValue.substring(prefix.length());
                break;
            }
        }
        // Apply operator
        String finalInputValue = inputValue;
        Predicate<FileEntry> nextOperation = switch (operator) {
            case "<=" -> e -> comparator.compare(actualValueFn.apply(e), inputValueFn.apply(finalInputValue)) <= 0;
            case ">=" -> e -> comparator.compare(actualValueFn.apply(e), inputValueFn.apply(finalInputValue)) >= 0;
            case "<" -> e -> comparator.compare(actualValueFn.apply(e), inputValueFn.apply(finalInputValue)) < 0;
            case ">" -> e -> comparator.compare(actualValueFn.apply(e), inputValueFn.apply(finalInputValue)) > 0;
            case "<>", "!=" -> e -> comparator.compare(actualValueFn.apply(e), inputValueFn.apply(finalInputValue)) != 0;
            default -> e -> comparator.compare(actualValueFn.apply(e), inputValueFn.apply(finalInputValue)) == 0;
        };
        criteria = criteria.and(nextOperation);
    }

    @Override
    public int handleCommand() throws Exception {
        if (directories != null) {
            FileVisitor visitor = new FileVisitor(criteria, suppressErrorMessages);
            for (Path path : directories) {
                if (path.toFile().isDirectory()) {
                    Files.walkFileTree(path, visitor);
                }
                else {
                    visitor.visitFile(path, null);
                }
            }
            if (visitor.fileEntryCount == 0) {
                System.out.println("No files found to match criteria.");
            }
            System.out.printf("Searched %d disks. Encountered %d errors.\n", visitor.diskCount, visitor.errorCount);
            return 0;
        }
        else {
            System.err.println("Please specify at least one directory.");
            return 1;
        }
    }

    public static class FileVisitor extends SimpleFileVisitor<Path> {
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

        private final Predicate<FileEntry> criteria;
        private final boolean suppressErrorMessages;
        private int diskCount;
        private int fileEntryCount;
        private int errorCount;
        boolean firstFileFoundOnDisk = true;

        public FileVisitor(Predicate<FileEntry> criteria, boolean suppressErrorMessages) {
            this.criteria = criteria;
            this.suppressErrorMessages = suppressErrorMessages;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (globMatcher.matches(file)) {
                String imageName = file.toString();
                try {
                    Source source = Sources.create(imageName).orElseThrow();
                    DiskFactory.Context ctx = Disks.inspect(source);
                    firstFileFoundOnDisk = true;
                    for (FormattedDisk disk : ctx.disks) {
                        searchAllFiles(disk, disk.getDirname());
                    }
                    diskCount++;
                } catch (Throwable t) {
                    if (!suppressErrorMessages) {
                        System.err.printf("Disk: %s\n", file);
                        System.err.printf("Error: %s\n", t.getMessage());
                    }
                    errorCount++;
                }
            }
            return FileVisitResult.CONTINUE;
        }

        public void searchAllFiles(DirectoryEntry directory, String fullDirectoryName) throws DiskException {
            boolean firstFileInDirectory = true;
            for (FileEntry file : directory.getFiles()) {
                if (file.isDeleted()) continue;
                if (file instanceof DirectoryEntry subdirectory) {
                    // Somewhat of a cheat to manage the slashes (this is only ProDOS). Should be ok...
                    searchAllFiles(subdirectory, Path.of(fullDirectoryName, subdirectory.getDirname()).toString());
                }
                if (criteria.test(file)) {
                    fileEntryCount++;
                    if (firstFileFoundOnDisk) {
                        System.out.printf("Disk: %s\n", directory.getFormattedDisk().getFilename());
                        String auxOrAddr = "Addr";
                        if (directory.getFormattedDisk() instanceof ProdosFormatDisk) {
                            auxOrAddr = "Aux";
                        }
                        System.out.printf("  %-30s %-5s %-8s %-5s\n", "Filename", "Type", "Size", auxOrAddr);
                        System.out.println("  =============== ===== ======== =====");
                    }
                    if (file instanceof ProdosFileEntry prodosFile) {
                        if (firstFileInDirectory) {
                            System.out.println(fullDirectoryName);
                        }
                        System.out.printf("* %-30s %-5s %8d $%04X\n", file.getFilename(), file.getFiletype(),
                                file.getSize(), prodosFile.getAuxiliaryType());
                    }
                    else {
                        System.out.printf("* %-30s %-5s %8d $%04X\n", file.getFilename(), file.getFiletype(),
                                file.getSize(), file.getAddress());
                    }
                    firstFileInDirectory = false;
                    firstFileFoundOnDisk = false;
                }
            }
        }
    }
}
