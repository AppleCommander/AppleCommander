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
package org.applecommander.transfer;

import com.google.common.base.Strings;
import org.applecommander.util.FileMagic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// The `ProdosAttributePreservation` class is a partial implementation of the ProDOS Attribute Preservation
/// documented on the NuLib site.
/// @see [ProDOS Attribute Preservation](https://nulib.com/library/nulib2-preserve.htm)
public record ProdosAttributePreservation(String fileName, int fileType, int auxType, Path dataPath, Path resourcePath) {
    public static final Pattern PATTERN = Pattern.compile("^(.*)#(\\p{XDigit}{2})(\\p{XDigit}{4})([Rr]?).*$");

    public ProdosAttributes toProdosAttributes() throws IOException {
        ProdosAttributes.Builder builder = new ProdosAttributes.Builder()
                .name(fileName)
                .fileType(fileType)
                .auxType(auxType);
        if (dataPath != null && dataPath.toFile().exists()) {
            builder.dataFork(Files.readAllBytes(dataPath));
        }
        if (resourcePath != null && resourcePath.toFile().exists()) {
            builder.resourceFork(Files.readAllBytes(resourcePath));
        }
        return builder.get();
    }

    /// Test if this file matches the expected filename pattern.
    public static boolean test(Path path) {
        String filename = path.getFileName().toString();
        Matcher matcher = PATTERN.matcher(filename);
        return matcher.matches();
    }

    /// Given a filename that is in the correct format, create the `ProdosAttributePreservation` settings.
    /// @throws IllegalArgumentException if an unexpected filename format is given
    public static ProdosAttributePreservation parse(Path path) {
        Matcher matcher = PATTERN.matcher(path.getFileName().toString());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format("filename '%s' is not in attribution preservation format", path.getFileName()));
        }
        String fileName = matcher.group(1);
        int fileType = Integer.parseInt(matcher.group(2), 16);
        int auxType = Integer.parseInt(matcher.group(3), 16);
        boolean resourceFork = !Strings.isNullOrEmpty(matcher.group(4));
        int numberSign = path.getFileName().toString().lastIndexOf('#');
        int period = path.getFileName().toString().lastIndexOf('.');
        boolean hasExtension = numberSign < period;

        Builder builder = ProdosAttributePreservation.builder()
                .includeExtension(hasExtension)
                .fileName(fileName)
                .fileType(fileType)
                .auxType(auxType);
        // Here is where we over-ride the computed paths with the real Path given
        if (resourceFork) {
            builder.resourcePath(path);
        } else {
            builder.dataPath(path);
        }
        return builder.get();
    }

    /// Initiate the builder.
    public static Builder builder() {
        return new Builder();
    }

    /// Build the ProdosAttributePreservation configuration.
    /// Note that as attributes are set, the data fork and resource fork filenames
    /// are computed. They can be over-ridden if required.
    public static class Builder {
        private boolean includeExtension = true;
        private String fileName;
        private int fileType;
        private int auxType;
        private boolean pathOverride = false;
        private Path dataPath;
        private Path resourcePath;

        public Builder includeExtension(boolean includeExtension) {
            this.includeExtension = includeExtension;
            return this;
        }
        public Builder fileName(String fileName) {
            this.fileName = fileName;
            updatePaths();
            return this;
        }
        public Builder fileType(int fileType) {
            this.fileType = fileType;
            updatePaths();
            return this;
        }
        public Builder auxType(int auxType) {
            this.auxType = auxType;
            updatePaths();
            return this;
        }
        public Builder dataPath(Path dataPath) {
            Objects.requireNonNull(dataPath);
            this.dataPath = dataPath;
            this.pathOverride = true;
            this.resourcePath = this.dataPath.resolveSibling(resourcePath.getFileName());
            return this;
        }
        public Builder resourcePath(Path resourcePath) {
            Objects.requireNonNull(resourcePath);
            this.resourcePath = resourcePath;
            this.pathOverride = true;
            this.dataPath = this.resourcePath.resolveSibling(dataPath.getFileName());
            return this;
        }
        private void updatePaths() {
            if (!pathOverride && fileName != null) {
                String dataFilename = String.format("%s#%02x%04x", fileName, fileType, auxType);
                String resourceFilename = String.format("%s#%02x%04xr", fileName, fileType, auxType);
                if (includeExtension) {
                    String ext = FileMagic.getProdosFileTypeText(fileType, auxType);
                    if (fileName.indexOf('.') != -1) {
                        ext = fileName.substring(fileName.lastIndexOf('.') + 1);
                    }
                    dataFilename = String.format("%s.%s", dataFilename, ext);
                    resourceFilename = String.format("%s.%s", resourceFilename, ext);
                }
                dataPath = Path.of(dataFilename);
                resourcePath = Path.of(resourceFilename);
            }
        }
        public ProdosAttributePreservation get() {
            return new ProdosAttributePreservation(fileName, fileType, auxType, dataPath, resourcePath);
        }
    }
}
