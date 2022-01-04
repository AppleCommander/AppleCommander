package io.github.applecommander.acx.fileutil;

import java.util.Optional;

import com.webcodepro.applecommander.storage.os.nakedos.NakedosFileEntry;

public class NakedosFileEntryReader implements FileEntryReader {
    private NakedosFileEntry fileEntry;
    
    public NakedosFileEntryReader(NakedosFileEntry fileEntry) {
        this.fileEntry = fileEntry;
    }
    
    @Override
    public Optional<String> getFilename() {
        return Optional.of(fileEntry.getFilename());
    }
    @Override
    public Optional<byte[]> getFileData() {
        return Optional.of(fileEntry.getFileData());
    }
}
