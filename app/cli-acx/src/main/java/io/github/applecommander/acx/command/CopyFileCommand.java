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

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.util.Name;
import com.webcodepro.applecommander.util.filestreamer.FileStreamer;
import com.webcodepro.applecommander.util.filestreamer.FileTuple;
import com.webcodepro.applecommander.util.filestreamer.TypeOfFile;
import io.github.applecommander.acx.base.ReadWriteDiskCommandOptions;
import io.github.applecommander.acx.converter.DiskConverter;
import io.github.applecommander.acx.fileutil.FileUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Command(name = "copy", description = "Copy files between disks.",
         aliases = { "cp" })
public class CopyFileCommand extends ReadWriteDiskCommandOptions {
    private static Logger LOG = Logger.getLogger(CopyFileCommand.class.getName());

    @Option(names = { "-r", "--recursive" }, description = "Copy files recursively.")
    private boolean recursiveFlag;
    
    @Option(names = { "-f", "--force" }, description = "Overwrite existing files.")
    private boolean overwriteFlag;
    
    @Option(names = { "--to", "--directory" }, description = "Specify which directory to place files.")
    private String targetPath;
    
    @Option(names = { "-s", "--from", "--source" }, description = "Source disk for files.", 
            converter = DiskConverter.class, required = true)
    private Disk sourceDisk;
    
    @Parameters(arity = "*", description = "File glob(s) to copy (default = '*')", 
            defaultValue = "*")
    private List<String> globs;

    @Override
    public int handleCommand() throws Exception {
        List<FileTuple> files = FileStreamer.forDisk(sourceDisk)
                .ignoreErrors(true)
                .includeTypeOfFile(TypeOfFile.BOTH)
                .recursive(false)   // we handle recursion in the FileUtils
                .matchGlobs(globs)
                .stream()
                .collect(Collectors.toList());

        if (files.isEmpty()) {
            LOG.warning(() -> String.format("No matches found for %s.", String.join(",", globs)));
        } else {
            DirectoryEntry targetDirectory = disk.getFormattedDisks()[0];
            if (targetPath != null) {
                Name name = new Name(targetPath);
                FileEntry found = name.getEntry(targetDirectory);
                if (found == null || !found.isDirectory()) {
                    throw new RuntimeException("unable to find directory: " + targetPath);
                }
                targetDirectory = (DirectoryEntry) found;
            }
            for (FileTuple tuple : files) {
                fileHandler(targetDirectory, tuple);
            }
        }
        return 0;
    }

    private void fileHandler(DirectoryEntry directoryEntry, FileTuple tuple) {
        try {
            if (!recursiveFlag && tuple.fileEntry.isDirectory()) {
                directoryEntry.createDirectory(tuple.fileEntry.getFilename());
            } else {
                FileUtils copier = new FileUtils(overwriteFlag);
                copier.copy(directoryEntry, tuple.fileEntry);
            }
        } catch (DiskException ex) {
            LOG.severe(ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
}
