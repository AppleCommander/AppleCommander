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
package io.github.applecommander.acx.fileutil;

import java.util.Optional;
import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.util.readerwriter.FileEntryReader;
import com.webcodepro.applecommander.util.readerwriter.FileEntryWriter;

import io.github.applecommander.acx.command.CopyFileCommand;

public class FileUtils {
    private static Logger LOG = Logger.getLogger(CopyFileCommand.class.getName());
    
    private boolean overwrite;
    
    public FileUtils(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public void copy(DirectoryEntry directory, FileEntry file) throws DiskException {
        LOG.fine(() -> String.format("Copying '%s'", file.getFilename()));
		if (file.isDeleted()) {
			// Skip deleted files
		}
		else if (file.isDirectory()) {
			copyDirectory(directory, (DirectoryEntry)file, file.getFilename());
		} 
		else {
			copyFile(directory, file);
		}
	}
	
	void copyDirectory(DirectoryEntry targetParent, DirectoryEntry sourceDir, String name) throws DiskException {
	    Optional<FileEntry> targetFile = targetParent.getFiles()
	            .stream()
	            .filter(fileEntry -> name.equals(fileEntry.getFilename()))
                    .filter(fileEntry -> !fileEntry.isDeleted())
	            .findFirst();
	    Optional<DirectoryEntry> targetDir = targetFile
	            .filter(FileEntry::isDirectory)
	            .map(DirectoryEntry.class::cast);

	    if (targetDir.isPresent()) {
	        // Fall through to general logic
	    }
	    else if (targetFile.isPresent()) {
	        // This is an abstract class, so faking it for now.
	        throw new DiskException("Unable to create directory", name) {
                private static final long serialVersionUID = 4726414295404986677L;
	        };
	    }
	    else {
	        targetDir = Optional.of(targetParent.createDirectory(name));
	    }
	    
        for (FileEntry fileEntry : sourceDir.getFiles()) {
            copy(targetDir.get(), fileEntry);
        }
	}
	
    void copyFile(DirectoryEntry directory, FileEntry sourceFile) throws DiskException {
        FileEntryReader source = FileEntryReader.get(sourceFile);
        copyFile(directory, source);
	}
	
	public void copyFile(DirectoryEntry directory, FileEntryReader source) throws DiskException {
	    String sourceName = source.getFilename().get();
	    String sanitizedName = directory.getFormattedDisk().getSuggestedFilename(sourceName);
	    final Optional<FileEntry> fileEntry = directory.getFiles().stream()
	        .filter(entry -> entry.getFilename().equals(sanitizedName))
                .filter(entry -> !entry.isDeleted())
	        .findFirst();

        final FileEntry targetFile;
        if (fileEntry.isPresent()) {
            targetFile = fileEntry
                    .filter(entry -> overwrite)
                    .orElseThrow(() -> new RuntimeException(String.format("File '%s' exists.", 
                            source.getFilename().get())));
	    }
	    else {
	        targetFile = directory.createFile();
	    }
	    
	    FileEntryWriter target = FileEntryWriter.get(targetFile);
	    
	    source.getFilename().ifPresent(target::setFilename);
	    source.getProdosFiletype().ifPresent(target::setProdosFiletype);
	    source.isLocked().ifPresent(target::setLocked);
	    source.getBinaryAddress().ifPresent(target::setBinaryAddress);
	    source.getBinaryLength().ifPresent(target::setBinaryLength);
	    source.getAuxiliaryType().ifPresent(target::setAuxiliaryType);
	    source.getCreationDate().ifPresent(target::setCreationDate);
	    source.getLastModificationDate().ifPresent(target::setLastModificationDate);

        if (source.getFileData().isPresent() && source.getResourceData().isPresent()) {
            target.setFileData(source.getFileData().get(), source.getResourceData().get());
        } else {
            source.getFileData().ifPresent(target::setFileData);
        }
	}
}
