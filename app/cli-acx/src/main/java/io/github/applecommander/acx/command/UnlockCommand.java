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

import java.util.logging.Logger;
import java.util.Arrays;
import java.util.List;

import com.webcodepro.applecommander.util.filestreamer.FileTuple;

import io.github.applecommander.acx.base.ReadWriteDiskCommandWithGlobOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "unlock", description = "Unlock file(s) on a disk image.")
public class UnlockCommand extends ReadWriteDiskCommandWithGlobOptions {
    private static Logger LOG = Logger.getLogger(UnlockCommand.class.getName());
	
	@Parameters(arity = "1..*", description = "File glob(s) to unlock (default = '*') - be cautious of quoting!")
    private List<String> globs = Arrays.asList("*");
	
	@Override
	protected List<String> getGlobs(){return globs;}
    
	public void fileHandler(FileTuple tuple) {
    	tuple.fileEntry.setLocked(false);
    	LOG.info(() -> String.format("File '%s' unlocked.", tuple.fileEntry.getFilename()));
    }
}
