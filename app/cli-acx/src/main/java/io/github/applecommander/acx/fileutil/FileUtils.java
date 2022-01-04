package io.github.applecommander.acx.fileutil;

import java.util.Optional;
import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.FileEntry;

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
