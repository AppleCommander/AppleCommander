package com.webcodepro.applecommander.storage.os.prodos;

import com.webcodepro.applecommander.storage.DiskException;
import org.applecommander.os.DiskCheck;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProdosDiskCheck implements DiskCheck {
    // Classifications
    public static final String DIRECTORY = "directory";

    private final ProdosFormatDisk disk;

    public ProdosDiskCheck(ProdosFormatDisk disk) {
        this.disk = disk;
    }

    @Override
    public List<Finding> scan() throws Exception {
        List<Finding> findings = new ArrayList<>();
        for (var dir : disk.getFiles()) {
            if ( ! (dir instanceof ProdosDirectoryEntry pdosDir)) {
                // Skip anything but directories
                continue;
            }
            if (pdosDir.getHeaderPointer() != 2) {
                String description = String.format("Subdirectory %s is not pointing to the key block of disk %s.",
                        pdosDir.getDirname(), disk.getDirname());
                Finding finding = new Finding(description,
                        Optional.of(() -> pdosDir.setHeaderPointer(2)),
                        DIRECTORY, new Coordinate(pdosDir.getKeyPointer()));
                findings.add(finding);
            }
            handleDirectory(findings, pdosDir);
        }
        return findings;
    }

    private void handleDirectory(List<Finding> findings, ProdosDirectoryEntry mainDir) throws DiskException {
        for (var dir : mainDir.getFiles()) {
            if ( ! (dir instanceof ProdosDirectoryEntry pdosDir)) {
                // Skip anything but directories
                continue;
            }
            if (pdosDir.getHeaderPointer() != mainDir.getKeyPointer()) {
                String description = String.format("Subdirectory %s is not pointing to the key block of directory %s.",
                        pdosDir.getDirname(), mainDir.getDirname());
                Finding finding = new Finding(description,
                        Optional.of(() -> pdosDir.setHeaderPointer(mainDir.getKeyPointer())),
                        DIRECTORY, new Coordinate(pdosDir.getKeyPointer()));
                findings.add(finding);
            }
            handleDirectory(findings, pdosDir);
        }
    }
}
