package io.github.applecommander.acx.converter;

import java.nio.file.Files;
import java.nio.file.Path;

import com.webcodepro.applecommander.storage.Disk;

import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

public class DiskConverter implements ITypeConverter<Disk> {
    @Override
    public Disk convert(String filename) throws Exception {
        if (Files.exists(Path.of(filename))) {
            return new Disk(filename);
        }
        throw new TypeConversionException(String.format("Disk '%s' not found", filename));
    }
}
