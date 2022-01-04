package io.github.applecommander.filters;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;

/**
 * A custom FileFilter to dump "raw" data from the disk.
 * This filter uses the filename as given on the Disk with
 * no additional extensions.
 * 
 * @author rob
 */
public class RawFileFilter implements FileFilter {

    @Override
    public byte[] filter(FileEntry fileEntry) {
        return fileEntry.getFileData();
    }

    @Override
    public String getSuggestedFileName(FileEntry fileEntry) {
        return fileEntry.getFilename();
    }

}
