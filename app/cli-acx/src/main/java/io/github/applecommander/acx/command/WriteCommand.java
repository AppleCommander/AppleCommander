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
import java.nio.file.Path;
import java.util.function.Supplier;

import io.github.applecommander.acx.arggroup.CoordinateSelection;
import io.github.applecommander.acx.base.ReadOnlyDiskImageCommandOptions;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "write", description = "Write a block or sector.")
public class WriteCommand extends ReadOnlyDiskImageCommandOptions {
    @ArgGroup(multiplicity = "1", heading = "%nCoordinate Selection:%n")
    private CoordinateSelection coordinate = new CoordinateSelection();
    
    @ArgGroup(heading = "%nInput Selection:%n")
    private InputSelection input = new InputSelection();
    
    @Override
    public int handleCommand() throws Exception {
        byte[] data = input.read();
        coordinate.write(selectedDisks().getFirst(), data);
        return 0;
    }
    
    public static class InputSelection {
        private Supplier<byte[]> source = this::readFromStdin;
        private String filename;
        
        public byte[] read() {
            return source.get();
        }
        
        @Option(names = "--stdin", description = "Read raw data from stdin. (default)")
        public void selectStdout(boolean flag) {
            source = this::readFromStdin;
        }
        @Option(names = { "-f", "--input" }, description = "Read raw data from file.")
        public void selectFile(String filename) {
            this.filename = filename;
            this.source = this::readFromFile;
        }
        
        public byte[] readFromStdin() {
            try {
                return System.in.readAllBytes();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        public byte[] readFromFile() {
            try {
                return Files.readAllBytes(Path.of(filename));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
