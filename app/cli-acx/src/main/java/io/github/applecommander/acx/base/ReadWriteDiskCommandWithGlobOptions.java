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
package io.github.applecommander.acx.base;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.webcodepro.applecommander.util.filestreamer.FileStreamer;
import com.webcodepro.applecommander.util.filestreamer.FileTuple;
import com.webcodepro.applecommander.util.filestreamer.TypeOfFile;

import picocli.CommandLine.Parameters;

public abstract class ReadWriteDiskCommandWithGlobOptions extends ReadWriteDiskCommandOptions {
    private static Logger LOG = Logger.getLogger(ReadWriteDiskCommandWithGlobOptions.class.getName());

    @Parameters(arity = "1..*", description = "File glob(s) to unlock (default = '*') - be cautious of quoting!")
    private List<String> globs = Arrays.asList("*");

    @Override
    public int handleCommand() throws Exception {
        List<FileTuple> files = FileStreamer.forDisk(disk)
			        .ignoreErrors(true)
			        .includeTypeOfFile(TypeOfFile.FILE)
			        .matchGlobs(globs)
			        .stream()
			        .collect(Collectors.toList());

        if (files.isEmpty()) {
        	LOG.warning(() -> String.format("No matches found for %s.", String.join(",", globs)));
        } else {
        	files.forEach(this::fileHandler);
        }
        
        return 0;
    }
    
    public abstract void fileHandler(FileTuple tuple);
}
