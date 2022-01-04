package io.github.applecommander.acx.fileutil;

import java.util.Map;
import java.util.Optional;

import com.webcodepro.applecommander.storage.os.rdos.RdosFileEntry;

public class RdosFileEntryReader implements FileEntryReader {
    private static final Map<String,String> FILE_TYPES;
    static {
        FILE_TYPES = Map.of(
                "T", "TXT",
                "A", "BAS",
                "B", "BIN"
            );
    }
    
    private RdosFileEntry fileEntry;
    
    public RdosFileEntryReader(RdosFileEntry fileEntry) {
        this.fileEntry = fileEntry;
    }
    
    @Override
    public Optional<String> getFilename() {
        return Optional.ofNullable(fileEntry.getFilename());
    }

    @Override
    public Optional<Integer> getBinaryAddress() {
        return Optional.ofNullable(fileEntry.getAddress());
    }

    @Override
    public Optional<String> getProdosFiletype() {
        return Optional.ofNullable(FILE_TYPES.get(fileEntry.getFiletype()));
    }
    
    @Override
    public Optional<byte[]> getFileData() {
        return Optional.ofNullable(fileEntry.getFileData());
    }
}
