/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2019-2022 by Robert Greene and others
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
package io.github.applecommander.acx.converter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.webcodepro.applecommander.storage.DiskFactory;
import com.webcodepro.applecommander.storage.Disks;
import com.webcodepro.applecommander.storage.FormattedDisk;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

public class DiskConverter implements ITypeConverter<List<FormattedDisk>> {
    @Override
    public List<FormattedDisk> convert(String filename) {
        if (Files.exists(Path.of(filename))) {
            Source source = Sources.create(filename).orElseThrow();
            DiskFactory.Context ctx = Disks.inspect(source);
            if (ctx.disks.isEmpty()) {
                throw new TypeConversionException(String.format("Disk '%s' not recognized", filename));
            }
            return ctx.disks;
        }
        throw new TypeConversionException(String.format("Disk '%s' not found", filename));
    }
}
