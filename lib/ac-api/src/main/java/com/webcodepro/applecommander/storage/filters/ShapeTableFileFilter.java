/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2004-2025 by Robert Greene
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
package com.webcodepro.applecommander.storage.filters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;

import io.github.applecommander.bastools.api.shapes.ShapeExporter;
import io.github.applecommander.bastools.api.shapes.ShapeTable;

public class ShapeTableFileFilter implements FileFilter {
    @Override
    public byte[] filter(FileEntry fileEntry) {
        try {
            ShapeTable shapeTable = ShapeTable.read(fileEntry.getFileData());
            
            ShapeExporter exporter = ShapeExporter.image()
                .border(true)
                .maxWidth(512)
                .png()
                .skipEmptyShapes(false)
                .build();
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            exporter.export(shapeTable, outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
    
    @Override
    public String getSuggestedFileName(FileEntry fileEntry) {
        String fileName = fileEntry.getFilename().trim();
        if (!fileName.toLowerCase().endsWith(".png")) {
            fileName += ".png";
        }
        return fileName;
    }
}
