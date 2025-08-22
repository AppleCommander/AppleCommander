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
package io.github.applecommander.acx.command;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

import io.github.applecommander.acx.arggroup.CoordinateSelection;
import io.github.applecommander.acx.base.ReadOnlyDiskImageCommandOptions;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "read", description = "Read a block or sector.")
public class ReadCommand extends ReadOnlyDiskImageCommandOptions {
    @ArgGroup(multiplicity = "1", heading = "%nCoordinate Selection:%n")
    private CoordinateSelection coordinate = new CoordinateSelection();
    
    @ArgGroup(heading = "%nOutput Selection:%n")
    private OutputSelection output = new OutputSelection();
    
    @Option(names = { "-f", "--force" }, description = "Overwrite existing file (combine with '-o').")
    private void selectForceFile(boolean flag) {
        openOptions = new OpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE };
    }
    private static OpenOption[] openOptions = { StandardOpenOption.CREATE_NEW };
    
    @Override
    public int handleCommand() throws Exception {
        byte[] data = coordinate.read(disk);
        output.write(data);
        return 0;
    }
    
    public static class OutputSelection {
        private Consumer<byte[]> sink = this::writeToStdout;
        private String filename;
        
        public void write(byte[] data) {
            sink.accept(data);
        }
        
        @Option(names = "--stdout", description = "Write raw data to stdout. (default)")
        public void selectStdout(boolean flag) {
            sink = this::writeToStdout;
        }
        @Option(names = { "-o", "--output" }, description = "Write raw data to file.")
        public void selectFile(String filename) {
            this.filename = filename;
            this.sink = this::writeToFile;
        }
        
        public void writeToStdout(byte[] data) {
            try {
                System.out.write(data);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        public void writeToFile(byte[] data) {
            try {
                Files.write(Path.of(filename), data, openOptions);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
