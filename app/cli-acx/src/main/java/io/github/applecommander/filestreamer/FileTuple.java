package io.github.applecommander.filestreamer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;

public class FileTuple {
    private static final Logger LOG = Logger.getLogger(FileTuple.class.getName());
    public final FormattedDisk formattedDisk;
    public final List<String> paths;
    public final DirectoryEntry directoryEntry;
    public final FileEntry fileEntry;
    
    private FileTuple(FormattedDisk formattedDisk, 
                      List<String> paths, 
                      DirectoryEntry directoryEntry, 
                      FileEntry fileEntry) {
        this.formattedDisk = formattedDisk;
        this.paths = Collections.unmodifiableList(paths);
        this.directoryEntry = directoryEntry;
        this.fileEntry = fileEntry;
    }
    
    public FileTuple pushd(FileEntry directoryEntry) {
        LOG.fine("Adding directory " + directoryEntry.getFilename());
        List<String> newPaths = new ArrayList<>(paths);
        newPaths.add(directoryEntry.getFilename());
        return new FileTuple(formattedDisk, newPaths, (DirectoryEntry)directoryEntry, null);
    }
    public FileTuple of(FileEntry fileEntry) {
        return new FileTuple(formattedDisk, paths, directoryEntry, fileEntry);
    }
    
    public static FileTuple of(FormattedDisk disk) {
        return new FileTuple(disk, new ArrayList<String>(), (DirectoryEntry)disk, null);
    }
}
