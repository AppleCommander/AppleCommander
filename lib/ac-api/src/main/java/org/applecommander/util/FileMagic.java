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
package org.applecommander.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.applecommander.filestore.ContentType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/// Provide some shared file identification across FileStores.
public class FileMagic {
    public static final ProdosFileType[] PRODOS_FILE_TYPES;
    static {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(Integer.class, new IntegerTypeAdapter())
            .registerTypeAdapter(int.class, new IntegerTypeAdapter())
            .create();
        TypeToken<List<ProdosFileType>> typeToken = new TypeToken<>() {};
        try (InputStream inputStream = FileMagic.class.getResourceAsStream("/file-type-magic.json")) {
            Objects.requireNonNull(inputStream);
            List<ProdosFileType> prodosFileTypes = gson.fromJson(new InputStreamReader(inputStream), typeToken);
            PRODOS_FILE_TYPES = new ProdosFileType[256];
            prodosFileTypes.forEach(fileType -> {
                PRODOS_FILE_TYPES[fileType.code] = fileType;
            });
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /// Given a prodos `file_type` and `aux_type`, identify the 3-letter abbreviation to use.
    public static String getProdosFileTypeText(int fileType, int auxType) {
        return findProdosFileType(fileType, auxType).map(FileTypeSummary::abbreviation).orElse("???");
    }

    /// Given a prodos `file_type` and `aux_type`, identify the content type it represents.
    public static ContentType getProdosContentType(int fileType, int auxType) {
        return findProdosFileType(fileType, auxType).map(FileTypeSummary::contentType).orElse(ContentType.UNKNOWN);
    }

    /// Given a filename, try to identify the content type.
    public static ContentType identifyContentType(String fileName) {
        ContentType contentType = ContentType.UNKNOWN;
        if (FileExtensions.ARCHIVE_IMAGE_EXTENSIONS.stream().anyMatch(e -> fileName.toLowerCase().endsWith(e))) {
            contentType = ContentType.ARCHIVE_IMAGE;
        } else if (FileExtensions.DISK_IMAGE_EXTENSIONS.stream().anyMatch(e -> fileName.toLowerCase().endsWith(e))) {
            contentType = ContentType.DISK_IMAGE;
        }

        return contentType;
    }

    /// Filter through the ProDOS metadata and generate a file type summary using the `aux_type` as primary
    /// and then falling back to the ProDOS `file_type`.
    public static Optional<FileTypeSummary> findProdosFileType(int fileType, int auxType) {
        if (PRODOS_FILE_TYPES[fileType] == null) {
            return Optional.empty();
        }
        ProdosFileType prodosFileType = PRODOS_FILE_TYPES[fileType];
        String abbreviation = null;
        String description = null;
        ContentType contentType = null;
        // Figure out if we have a "closer" aux. type and use those values.
        if (prodosFileType.auxTypes() != null) {
            for (ProdosAuxType prodosAuxType : prodosFileType.auxTypes()) {
                if (prodosAuxType.code == auxType) {
                    abbreviation = prodosAuxType.abbreviation();
                    description = prodosAuxType.description;
                    contentType = prodosAuxType.contentType;
                }
            }
        }
        // For any values that are UNSET, over-ride from the file type itself.
        if (abbreviation == null) {
            abbreviation = prodosFileType.abbreviation;
            if (abbreviation == null) {
                abbreviation = String.format("$%02X", fileType);
            }
        }
        if (description == null) {
            description = prodosFileType.description;
        }
        if (contentType == null) {
            contentType = prodosFileType.contentType;
            if (contentType == null) {
                contentType = ContentType.UNKNOWN;
            }
        }
        return Optional.of(new FileTypeSummary(abbreviation, description, contentType));
    }

    public record FileTypeSummary(String abbreviation, String description, ContentType contentType) {}

    public record ProdosFileType(int code, String abbreviation, String description, List<ProdosAuxType> auxTypes, ContentType contentType) {}
    public record ProdosAuxType(int code, String abbreviation, String description, ContentType contentType) {}
}
