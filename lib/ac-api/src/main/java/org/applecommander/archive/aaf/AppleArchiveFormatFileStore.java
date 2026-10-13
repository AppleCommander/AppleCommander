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
package org.applecommander.archive.aaf;

import org.applecommander.capability.Capability;
import org.applecommander.filestore.*;
import org.applecommander.source.Source;
import org.applecommander.util.Container;
import org.applecommander.util.InformationGroup;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AppleArchiveFormatFileStore implements FileStore {
    private final Source source;
    private final List<FileEntry> files = new ArrayList<>();

    public AppleArchiveFormatFileStore(Source source) {
        this.source = source;
        parseContents();
    }

    public void parseContents() {
        files.clear();
        try (
            ByteArrayInputStream inputStream = new ByteArrayInputStream(source.readAllBytes().asBytes());
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        ) {
            AppleArchiveFormatFileEntry fileEntry = null;
            boolean atEnd = false;
            for (String line : reader.readAllLines()) {
                if (line == null || atEnd) break;
                if (line.isEmpty()) continue;
                switch (line.charAt(0)) {
                    case '=' -> {
                        String name = line.substring(1);
                        fileEntry = new AppleArchiveFormatFileEntry(this, name);
                        files.add(fileEntry);
                    }
                    case '-' -> {
                        if (fileEntry == null) {
                            throw new IOException("unexpected construct in AAF file (found '-' before '=')");
                        }
                        fileEntry.appendText(line.substring(1));
                    }
                    case '+' -> atEnd = true;
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /// The label is a mechanism to distinguish multiple file stores in a single file.
    /// For instance, UniDOS has two file stores in the file, so it would have two
    /// different labels such as "Disk 1" and "Disk 2".
    @Override
    public String getLabel() {
        return source.getName();
    }

    /// All FileStores support a "root" directory that references all files.
    @Override
    public Directory getRootDirectory() {
        return new GenericDirectory<>(this, files);
    }

    /// This is the path separator character.
    @Override
    public String getPathSeparator() {
        return "";
    }

    /// Yields a list of columns to be used in this display mode.
    @Override
    public List<DisplayColumn> getDisplayColumns() {
        return DisplayColumn.builder(AppleArchiveFormatFileEntry.class)
                .addStringField("Name", AppleArchiveFormatFileEntry::getName)
                .addLongField("Size", AppleArchiveFormatFileEntry::getSize)
                .toList();
    }

    @Override
    public boolean can(Capability capability) {
        return false;
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, source);
    }

    @Override
    public List<InformationGroup> information() {
        return InformationGroup.builder("Apple Archive Format")
                .item("Files").value(files.size())
                .get();
    }
}
