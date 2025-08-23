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

import java.util.List;
import java.util.logging.Logger;

import com.webcodepro.applecommander.util.filestreamer.FileStreamer;
import com.webcodepro.applecommander.util.filestreamer.FileTuple;
import com.webcodepro.applecommander.util.filestreamer.TypeOfFile;

import io.github.applecommander.acx.base.ReadWriteDiskCommandOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "rename", description = "Rename file on a disk image.",
        aliases = { "ren" })
public class RenameFileCommand extends ReadWriteDiskCommandOptions {
    private static Logger LOG = Logger.getLogger(RenameFileCommand.class.getName());
    
    @Option(names = { "-m", "--multiple" }, description = "Force rename when multiple files found.")
    private boolean multipleOverride;
    
    @Option(names = { "-f", "--force" }, description = "Rename locked files.")
    private boolean lockOverride;
    
    @Parameters(index = "0", description = "Original file name (include path).")
    private String originalFilename;
    
    @Parameters(index = "1", description = "New file name (just the new filename).")
    private String newFilename;

    @Override
    public int handleCommand() throws Exception {
        List<FileTuple> files = FileStreamer.forDisks(disks)
			        .ignoreErrors(true)
			        .includeTypeOfFile(TypeOfFile.FILE)
			        .matchGlobs(originalFilename)
			        .stream()
			        .toList();

        if (files.isEmpty()) {
        	LOG.warning(() -> String.format("File not found for %s.", originalFilename));
        }
        else if (!multipleOverride && files.size() > 1) {
        	LOG.severe(() -> String.format("Multile files with %s found (count = %d).", 
        			originalFilename, files.size()));
        } 
    	else {
        	files.forEach(this::fileHandler);
        }
        return 0;
    }
    
    public void fileHandler(FileTuple tuple) {
    	if (tuple.fileEntry.isLocked()) {
    		if (lockOverride) {
    			LOG.info(() -> String.format("File '%s' is locked, but 'force' specified; ignoring lock.",
    					tuple.fileEntry.getFilename()));
    		} else {
	    		LOG.warning(() -> String.format("File '%s' is locked.", tuple.fileEntry.getFilename()));
	    		return;
    		}
    	}
    	tuple.fileEntry.setFilename(newFilename);
    	LOG.info(() -> String.format("File '%s' renamed to '%s'.", originalFilename, newFilename));
    }
}
