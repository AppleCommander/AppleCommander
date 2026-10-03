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
package org.applecommander.javafx.settings;

import org.applecommander.applesingle.AppleSingle;
import org.applecommander.filestore.FileEntry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

public enum ExportOption {
    RAW_BINARY("Raw Binary (filename)", ExportOption::copyToRawBinary),
    APPLE_SINGLE("AppleSingle (filename.as)", ExportOption::copyToAppleSingle),
    ATTRIBUTE_PRESERVATION("ProDOS Attribute Preservation (filename#TTAAAA.ext)", ExportOption::copyWithAttributePreservation);

    private final String description;
    private final BiFunction<FileEntry,Path,List<Path>> copyFn;

    ExportOption(String description, BiFunction<FileEntry,Path,List<Path>> copyFn) {
        Objects.requireNonNull(description, "description cannot be null");
        Objects.requireNonNull(copyFn, "copyFn cannot be null");
        this.description = description;
        this.copyFn = copyFn;
    }

    public String getDescription() {
        return description;
    }

    public List<Path> copyToPath(FileEntry fileEntry, Path destination) {
        return copyFn.apply(fileEntry, destination);
    }

    public static Path addToFilename(Path path, String suffix) {
        String name = path.getFileName().toString();
        int idx = name.lastIndexOf('.');
        if (idx == -1) {
            return path.resolveSibling(name.concat(suffix));
        }
        name = name.substring(0, idx).concat(suffix).concat(name.substring(idx + 1));
        return path.resolve(name);
    }

    public static List<Path> copyToRawBinary(FileEntry fileEntry, Path destination) {
        try {
            List<Path> paths = new ArrayList<>();
            paths.add(destination);
            Files.write(destination, fileEntry.getDataFork().asBytes(), StandardOpenOption.CREATE);
            if (fileEntry.getResourceFork().isPresent()) {
                Files.write(addToFilename(destination, "-rsrc"), fileEntry.getResourceFork().get().asBytes(),
                        StandardOpenOption.CREATE);
            }
            return paths;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static List<Path> copyToAppleSingle(FileEntry fileEntry, Path destination) {
        try {
            List<Path> paths = new ArrayList<>();
            destination = destination.resolveSibling(destination.getFileName() + ".as");
            paths.add(destination);
            // TODO need to get ProDOS-ified file type + some mechanism for access bits + dates. Maybe it fits in FileStore??
            AppleSingle.Builder builder = AppleSingle.builder()
                    .realName(fileEntry.getName())
                    .dataFork(fileEntry.getDataFork().asBytes());
            fileEntry.getResourceFork().ifPresent(resourceFork -> builder.resourceFork(resourceFork.asBytes()));
            builder.build().save(destination);
            return paths;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static List<Path> copyWithAttributePreservation(FileEntry fileEntry, Path destination) {
        try {
            List<Path> paths = new ArrayList<>();
            String name = fileEntry.getName();
            String ext = fileEntry.getFiletype();
            int idx = name.lastIndexOf('.');
            if (idx > -1) {
                ext = name.substring(idx + 1);
                name = name.substring(0, idx);
            }
            Path dataForkPath = destination.resolveSibling(String.format("%s#TTAAAA.%s", name, ext));
            Files.write(dataForkPath, fileEntry.getDataFork().asBytes(), StandardOpenOption.CREATE);
            if (fileEntry.getResourceFork().isPresent()) {
                Path resourceForkPath = destination.resolve(String.format("%s#TTAAAA_rsrc_.%s", name, ext));
                Files.write(resourceForkPath, fileEntry.getResourceFork().get().asBytes(), StandardOpenOption.CREATE);
            }
            return paths;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
