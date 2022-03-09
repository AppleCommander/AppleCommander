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
