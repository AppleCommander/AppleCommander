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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

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

    /// Given a prodos file_type and aux_type, identify the 3-letter abbreviation to use.
    public static String getProdosFileTypeText(int fileType, int auxType) {
        if (PRODOS_FILE_TYPES[fileType] == null) {
            return String.format("$%02X", fileType);
        }
        ProdosFileType prodosFileType = PRODOS_FILE_TYPES[fileType];
        if (prodosFileType.auxTypes() != null) {
            for (ProdosAuxType prodosAuxType : prodosFileType.auxTypes()) {
                if (prodosAuxType.code == auxType && prodosAuxType.abbreviation() != null) {
                    return prodosAuxType.abbreviation();
                }
            }
        }
        if (prodosFileType.abbreviation() == null) {
            return String.format("$%02X", fileType);
        }
        return prodosFileType.abbreviation();
    }

    public record ProdosFileType(int code, String abbreviation, String description, List<ProdosAuxType> auxTypes) {}
    public record ProdosAuxType(int code, String abbreviation, String description) {}
}
