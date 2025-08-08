/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2021-2022 by Robert Greene and others
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
package com.webcodepro.applecommander.util.filestreamer;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.DiskUnrecognizedException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;

/**
 * FileStreamer is utility class that will (optionally) recurse through all directories and
 * feed a Java Stream of useful directory walking detail (disk, directory, file, and the 
 * textual path to get there).
 * <p>
 * Sample usage:
 * <pre>
 * FileStreamer.forDisk(image)
 *             .ignoreErrors(true)
 *             .stream()
 *             .filter(this::fileFilter)
 *             .forEach(fileHandler);
 * </pre>
 * 
 * @author rob
 */
public class FileStreamer {
    private static final Consumer<FormattedDisk> NOOP_CONSUMER = d -> {};

    public static FileStreamer forDisk(File file) throws IOException, DiskUnrecognizedException {
        return forDisk(file.getPath());
    }
    public static FileStreamer forDisk(String fileName) throws IOException, DiskUnrecognizedException {
        return new FileStreamer(new Disk(fileName));
    }
    public static FileStreamer forDisk(Disk disk) throws DiskUnrecognizedException {
        return new FileStreamer(disk);
    }
    public static FileStreamer forFormattedDisks(FormattedDisk... disks) {
        return new FileStreamer(disks);
    }
    
    private FormattedDisk[] formattedDisks = null;
    
    // Processor flags (used in gathering)
    private boolean ignoreErrorsFlag = false;
    private boolean recursiveFlag = true;
    
    // Processor events
    private Consumer<FormattedDisk> beforeDisk = NOOP_CONSUMER;
    private Consumer<FormattedDisk> afterDisk = NOOP_CONSUMER;
    
    // Filters
    private Predicate<FileTuple> filters = this::deletedFileFilter;
    private boolean includeDeletedFlag = false;
    private List<PathMatcher> pathMatchers = new ArrayList<>();
    
    private FileStreamer(Disk disk) throws DiskUnrecognizedException {
        this(disk.getFormattedDisks());
    }
    private FileStreamer(FormattedDisk... disks) {
        this.formattedDisks = disks;
    }
    
    public FileStreamer ignoreErrors(boolean flag) {
        this.ignoreErrorsFlag = flag;
        return this;
    }
    public FileStreamer recursive(boolean flag) {
        this.recursiveFlag = flag;
        return this;
    }
    public FileStreamer matchGlobs(List<String> globs) {
        if (globs != null && !globs.isEmpty()) {
            FileSystem fs = FileSystems.getDefault();
            for (String glob : globs) {
                pathMatchers.add(fs.getPathMatcher("glob:" + glob));
            }
            this.filters = filters.and(this::globFilter);
        }
        return this;
    }
    public FileStreamer matchGlobs(String... globs) {
        return matchGlobs(Arrays.asList(globs));
    }
    public FileStreamer includeTypeOfFile(TypeOfFile type) {
        this.filters = filters.and(type.predicate);
        return this;
    }
    public FileStreamer includeDeleted(boolean flag) {
        this.includeDeletedFlag = flag;
        return this;
    }
    public FileStreamer beforeDisk(Consumer<FormattedDisk> consumer) {
        this.beforeDisk = consumer;
        return this;
    }
    public FileStreamer afterDisk(Consumer<FormattedDisk> consumer) {
        this.afterDisk = consumer;
        return this;
    }
    
    public Stream<FileTuple> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), 0), false)
                            .filter(filters);
    }
    public Iterator<FileTuple> iterator() {
        return new FileTupleIterator();
    }
    
    protected boolean deletedFileFilter(FileTuple tuple) {
        return includeDeletedFlag || !tuple.fileEntry.isDeleted();
    }
    protected boolean globFilter(FileTuple tuple) {
        if (recursiveFlag && tuple.fileEntry.isDirectory()) {
            // If we don't match directories, no files can be listed.
            return true;
        }
        // This may cause issues, but Path is a "real" filesystem construct, so the delimiters
        // vary by OS (likely just "/" and "\"). However, Java also erases them to some degree,
        // so using "/" (as used in ProDOS) will likely work out.
        // Also note that we check the single file "PARMS.S" and full path "SOURCE/PARMS.S" since
        // the user might have entered "*.S" or something like "SOURCE/PARMS.S".
        FileSystem fs = FileSystems.getDefault();
        Path filePath = Paths.get(tuple.fileEntry.getFilename());
        Path fullPath = Paths.get(String.join(fs.getSeparator(), tuple.paths), 
                tuple.fileEntry.getFilename());
        for (PathMatcher pathMatcher : pathMatchers) {
            if (pathMatcher.matches(filePath) || pathMatcher.matches(fullPath)) return true;
        }
        return false;
    }
    
    private class FileTupleIterator implements Iterator<FileTuple> {
        private LinkedList<FileTuple> files = new LinkedList<>();
        private FormattedDisk currentDisk;
        
        private FileTupleIterator() {
            for (FormattedDisk formattedDisk : formattedDisks) {
                files.add(FileTuple.of(formattedDisk));
            }
        }

        @Override
        public boolean hasNext() {
            boolean hasNext = !files.isEmpty();
            if (hasNext) {
                FileTuple tuple = files.peek();
                // Was there a disk switch?
                if (tuple.formattedDisk != currentDisk) {
                    if (currentDisk != null) {
                        afterDisk.accept(currentDisk);
                    }
                    currentDisk = tuple.formattedDisk;
                    beforeDisk.accept(currentDisk);
                }
                // Handle disks independently and guarantee disk events fire for empty disks
                if (tuple.isDisk()) {
                    tuple = files.removeFirst();
                    files.addAll(0, toTupleList(tuple));
                    return hasNext();
                }
            } else {
                if (currentDisk != null) {
                    afterDisk.accept(currentDisk);
                }
                currentDisk = null;
            }
            return hasNext;
        }

        @Override
        public FileTuple next() {
            if (hasNext()) {
                FileTuple tuple = files.removeFirst();
                if (recursiveFlag && tuple.isDirectory()) {
                    FileTuple newTuple = tuple.pushd(tuple.fileEntry);
                    files.addAll(0, toTupleList(newTuple));
                }
                return tuple;
            } else {
                throw new NoSuchElementException();
            }
        }
        
        private List<FileTuple> toTupleList(FileTuple tuple) {
            List<FileTuple> list = new ArrayList<>();
            try {
                for (FileEntry fileEntry : tuple.directoryEntry.getFiles()) {
                    FileTuple temp = tuple.of(fileEntry);
                    if (filters.test(temp)) {
                        list.add(temp);
                    }
                }
            } catch (DiskException e) {
                if (!ignoreErrorsFlag) {
                    throw new RuntimeException(e);
                }
            }
            return list;
        }
    }
}
