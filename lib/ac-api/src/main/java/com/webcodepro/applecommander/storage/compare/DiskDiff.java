/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2021-2022 by Robert Greene and others
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
package com.webcodepro.applecommander.storage.compare;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.DiskGeometry;
import com.webcodepro.applecommander.storage.DiskUnrecognizedException;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import com.webcodepro.applecommander.util.Range;
import com.webcodepro.applecommander.util.filestreamer.FileStreamer;
import com.webcodepro.applecommander.util.filestreamer.FileTuple;
import com.webcodepro.applecommander.util.filestreamer.TypeOfFile;
import com.webcodepro.applecommander.util.readerwriter.FileEntryReader;

/**
 * Perform a disk comparison based on selected strategy.
 */
public class DiskDiff {
    public static ComparisonResult compare(Disk diskA, Disk diskB) {
        return new DiskDiff(diskA, diskB).compare();
    }
    public static Builder create(Disk diskA, Disk diskB) {
        return new Builder(diskA, diskB);
    }
    
    private Disk diskA;
    private Disk diskB;
    private ComparisonResult results = new ComparisonResult();
    
    private BiConsumer<FormattedDisk,FormattedDisk> diskComparisonStrategy = this::compareByNativeGeometry;
    
    private DiskDiff(Disk diskA, Disk diskB) {
        Objects.requireNonNull(diskA);
        Objects.requireNonNull(diskB);
        this.diskA = diskA;
        this.diskB = diskB;
    }
    
    public ComparisonResult compare() {
        FormattedDisk[] formattedDisksA = null;
        try {
            formattedDisksA = diskA.getFormattedDisks();
        } catch (DiskUnrecognizedException e) {
            results.addError(e);
        }
        FormattedDisk[] formattedDisksB = null;
        try {
            formattedDisksB = diskB.getFormattedDisks();
        } catch (DiskUnrecognizedException e) {
            results.addError(e);
        }
        
        if (!results.hasErrors()) {
            compareAll(formattedDisksA, formattedDisksB);
        }
        return results;
    }
    
    public void compareAll(FormattedDisk[] formattedDisksA, FormattedDisk[] formattedDisksB) {
        Objects.requireNonNull(formattedDisksA);
        Objects.requireNonNull(formattedDisksB);
        
        if (formattedDisksA.length != formattedDisksB.length) {
            results.addWarning("Cannot compare all disks; %s has %d while %s has %d.",
                    diskA.getFilename(), formattedDisksA.length,
                    diskB.getFilename(), formattedDisksB.length);
        }

        int min = Math.min(formattedDisksA.length, formattedDisksB.length);
        for (int i=0; i<min; i++) {
            this.diskComparisonStrategy.accept(formattedDisksA[i], formattedDisksB[i]);
        }
    }
    
    /** Compare disks by whatever native geometry the disks have. Fails if geometries do not match. */
    public void compareByNativeGeometry(FormattedDisk formattedDiskA, FormattedDisk formattedDiskB) {
        DiskGeometry geometryA = formattedDiskA.getDiskGeometry();
        DiskGeometry geometryB = formattedDiskB.getDiskGeometry();

        if (geometryA != geometryB) {
            results.addError("Disks are different geometry (block versus track/sector)");
            return;
        }
        
        switch (geometryA) {
        case BLOCK:
            compareByBlockGeometry(formattedDiskA, formattedDiskB);
            break;
        case TRACK_SECTOR:
            compareByTrackSectorGeometry(formattedDiskA, formattedDiskB);
            break;
        default:
            results.addError("Unknown geometry: %s", geometryA);
        }
    }

    /** Compare disks by 512-byte ProDOS/Pascal blocks. */
    public void compareByBlockGeometry(FormattedDisk formattedDiskA, FormattedDisk formattedDiskB) {
        ImageOrder orderA = formattedDiskA.getImageOrder();
        ImageOrder orderB = formattedDiskB.getImageOrder();
        
        if (orderA.getBlocksOnDevice() != orderB.getBlocksOnDevice()) {
            results.addError("Different sized disks do not equal. (Blocks: %d <> %d)", 
                    orderA.getBlocksOnDevice(), orderB.getBlocksOnDevice());
            return;
        }

        List<Integer> unequalBlocks = new ArrayList<>();
        for (int block=0; block<orderA.getBlocksOnDevice(); block++) {
            byte[] blockA = orderA.readBlock(block);
            byte[] blockB = orderB.readBlock(block);
            if (!Arrays.equals(blockA, blockB)) {
                unequalBlocks.add(block);
            }
        }
        for (Range r : Range.from(unequalBlocks)) {
            if (r.size() == 1) {
                results.addError("Block #%s does not match.", r);
            }
            else {
                results.addError("Blocks #%s do not match.", r);
            }
        }
    }
    
    /** Compare disks by 256-byte DOS sectors. */
    public void compareByTrackSectorGeometry(FormattedDisk formattedDiskA, FormattedDisk formattedDiskB) {
        ImageOrder orderA = formattedDiskA.getImageOrder();
        ImageOrder orderB = formattedDiskB.getImageOrder();

        if (orderA.getSectorsPerDisk() != orderB.getSectorsPerDisk()) {
            results.addError("Different sized disks do not equal. (Sectors: %d <> %d)",
                    orderA.getSectorsPerDisk(), orderB.getSectorsPerDisk());
            return;
        }
        
        for (int track=0; track<orderA.getTracksPerDisk(); track++) {
            List<Integer> unequalSectors = new ArrayList<>();
            for (int sector=0; sector<orderA.getSectorsPerTrack(); sector++) {
                byte[] sectorA = orderA.readSector(track, sector);
                byte[] sectorB = orderB.readSector(track, sector);
                if (!Arrays.equals(sectorA, sectorB)) {
                    unequalSectors.add(sector);
                }
            }
            if (!unequalSectors.isEmpty()) {
                results.addError("Track %d does not match on sectors %s", track,
                        Range.from(unequalSectors)
                             .stream()
                             .map(Range::toString)
                             .collect(Collectors.joining(",")));
            }
        }
    }
    
    /** Compare by filename. This accounts for names only in disk A, only in disk B, or different but same-named. */
    public void compareByFileName(FormattedDisk formattedDiskA, FormattedDisk formattedDiskB) {
        try {
            Map<String,List<FileTuple>> filesA = FileStreamer.forDisk(formattedDiskA)
                    .includeTypeOfFile(TypeOfFile.FILE)
                    .recursive(true)
                    .stream()
                    .collect(Collectors.groupingBy(FileTuple::fullPath));
            Map<String,List<FileTuple>> filesB = FileStreamer.forDisk(formattedDiskB)
                    .includeTypeOfFile(TypeOfFile.FILE)
                    .recursive(true)
                    .stream()
                    .collect(Collectors.groupingBy(FileTuple::fullPath));
            
            Set<String> pathsOnlyA = new HashSet<>(filesA.keySet());
            pathsOnlyA.removeAll(filesB.keySet());
            if (!pathsOnlyA.isEmpty()) {
                results.addError("Files only in %s: %s", formattedDiskA.getFilename(), String.join(", ", pathsOnlyA));
            }

            Set<String> pathsOnlyB = new HashSet<>(filesB.keySet());
            pathsOnlyB.removeAll(filesA.keySet());
            if (!pathsOnlyB.isEmpty()) {
                results.addError("Files only in %s: %s", formattedDiskB.getFilename(), String.join(", ", pathsOnlyB));
            }
            
            Set<String> pathsInAB = new HashSet<>(filesA.keySet());
            pathsInAB.retainAll(filesB.keySet());
            for (String path : pathsInAB) {
                List<FileTuple> tuplesA = filesA.get(path);
                List<FileTuple> tuplesB = filesB.get(path);

                // Since this is by name, we expect a single file; report oddities
                FileTuple tupleA = tuplesA.get(0);
                if (tuplesA.size() > 1) {
                    results.addWarning("Path %s on disk %s has %d entries.", path, formattedDiskA.getFilename(), tuplesA.size());
                }
                FileTuple tupleB = tuplesB.get(0);
                if (tuplesB.size() > 1) {
                    results.addWarning("Path %s on disk %s has %d entries.", path, formattedDiskB.getFilename(), tuplesB.size());
                }
                
                // Do our own custom compare so we can capture a description of differences:
                FileEntryReader readerA = FileEntryReader.get(tupleA.fileEntry);
                FileEntryReader readerB = FileEntryReader.get(tupleB.fileEntry);
                List<String> differences = compare(readerA, readerB);
                if (!differences.isEmpty()) {
                    results.addWarning("Path %s differ: %s", path, String.join(", ", differences));
                }
            }
        } catch (DiskException ex) {
            results.addError(ex);
        }
    }

    /** Compare by file content. Accounts for content differences that are "only" in disk A or "only" in disk B. */
    public void compareByFileContent(FormattedDisk formattedDiskA, FormattedDisk formattedDiskB) {
        try {
            Map<String,List<FileTuple>> contentA = FileStreamer.forDisk(formattedDiskA)
                    .includeTypeOfFile(TypeOfFile.FILE)
                    .recursive(true)
                    .stream()
                    .collect(Collectors.groupingBy(this::contentHash));
            Map<String,List<FileTuple>> contentB = FileStreamer.forDisk(formattedDiskB)
                    .includeTypeOfFile(TypeOfFile.FILE)
                    .recursive(true)
                    .stream()
                    .collect(Collectors.groupingBy(this::contentHash));
            
            Set<String> contentOnlyA = new HashSet<>(contentA.keySet());
            contentOnlyA.removeAll(contentB.keySet());
            if (!contentOnlyA.isEmpty()) {
                Set<String> pathNamesA = contentOnlyA.stream()
                    .map(contentA::get)
                    .flatMap(List::stream)
                    .map(FileTuple::fullPath)
                    .collect(Collectors.toSet());
                results.addError("Content that only exists in %s: %s", 
                        formattedDiskA.getFilename(), String.join(", ", pathNamesA));
            }

            Set<String> contentOnlyB = new HashSet<>(contentB.keySet());
            contentOnlyB.removeAll(contentA.keySet());
            if (!contentOnlyB.isEmpty()) {
                Set<String> pathNamesB = contentOnlyB.stream()
                        .map(contentB::get)
                        .flatMap(List::stream)
                        .map(FileTuple::fullPath)
                        .collect(Collectors.toSet());
                results.addError("Content that only exists in %s: %s", 
                        formattedDiskB.getFilename(), String.join(", ", pathNamesB));
            }

            Set<String> contentInAB = new HashSet<>(contentA.keySet());
            contentInAB.retainAll(contentB.keySet());
            for (String content : contentInAB) {
                List<FileTuple> tuplesA = contentA.get(content);
                List<FileTuple> tuplesB = contentB.get(content);

                // This is by content, but uncertain how to report multiple per disk, so pick first one
                FileTuple tupleA = tuplesA.get(0);
                if (tuplesA.size() > 1) {
                    results.addWarning("Hash %s on disk %s has %d entries.", content, 
                            formattedDiskA.getFilename(), tuplesA.size());
                }
                FileTuple tupleB = tuplesB.get(0);
                if (tuplesB.size() > 1) {
                    results.addWarning("Hash %s on disk %s has %d entries.", content, 
                            formattedDiskB.getFilename(), tuplesB.size());
                }
                
                // Do our own custom compare so we can capture a description of differences:
                FileEntryReader readerA = FileEntryReader.get(tupleA.fileEntry);
                FileEntryReader readerB = FileEntryReader.get(tupleB.fileEntry);
                List<String> differences = compare(readerA, readerB);
                if (!differences.isEmpty()) {
                    results.addWarning("Files %s and %s share same content but file attributes differ: %s", 
                            tupleA.fullPath(), tupleB.fullPath(), String.join(", ", differences));
                }
            }
        } catch (DiskException ex) {
            results.addError(ex);
        }
    }
    private String contentHash(FileTuple tuple) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] digest = messageDigest.digest(tuple.fileEntry.getFileData());
            return String.format("%032X", new BigInteger(1, digest));
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    private List<String> compare(FileEntryReader readerA, FileEntryReader readerB) {
        List<String> differences = new ArrayList<>();
        if (!readerA.getFilename().equals(readerB.getFilename())) {
            differences.add("filename");
        }
        if (!readerA.getProdosFiletype().equals(readerB.getProdosFiletype())) {
            differences.add("filetype");
        }
        if (!readerA.isLocked().equals(readerB.isLocked())) {
            differences.add("locked");
        }
        if (!Arrays.equals(readerA.getFileData().orElse(null), readerB.getFileData().orElse(null))) {
            differences.add("file data");
        }
        if (!Arrays.equals(readerA.getResourceData().orElse(null), readerB.getResourceData().orElse(null))) {
            differences.add("resource fork");
        }
        if (!readerA.getBinaryAddress().equals(readerB.getBinaryAddress())) {
            differences.add("address");
        }
        if (!readerA.getBinaryLength().equals(readerB.getBinaryLength())) {
            differences.add("length");
        }
        if (!readerA.getAuxiliaryType().equals(readerB.getAuxiliaryType())) {
            differences.add("aux. type");
        }
        if (!readerA.getCreationDate().equals(readerB.getCreationDate())) {
            differences.add("create date");
        }
        if (!readerA.getLastModificationDate().equals(readerB.getLastModificationDate())) {
            differences.add("mod. date");
        }
        return differences;
    }

    public static class Builder {
        private DiskDiff diff;
        
        public Builder(Disk diskA, Disk diskB) {
            diff = new DiskDiff(diskA, diskB);
        }
        /** Compare disks by whatever native geometry the disks have. Fails if geometries do not match. */
        public Builder selectCompareByNativeGeometry() {
            diff.diskComparisonStrategy = diff::compareByNativeGeometry;
            return this;
        }
        /** Compare disks by 256-byte DOS sectors. */
        public Builder selectCompareByTrackSectorGeometry() {
            diff.diskComparisonStrategy = diff::compareByTrackSectorGeometry;
            return this;
        }
        /** Compare disks by 512-byte ProDOS/Pascal blocks. */
        public Builder selectCompareByBlockGeometry() {
            diff.diskComparisonStrategy = diff::compareByBlockGeometry;
            return this;
        }
        /** Compare disks by files ensuring that all filenames match. */
        public Builder selectCompareByFileName() {
            diff.diskComparisonStrategy = diff::compareByFileName;
            return this;
        }
        /** Compare disks by files based on content; allowing files to have moved or been renamed. */
        public Builder selectCompareByFileContent() {
            diff.diskComparisonStrategy = diff::compareByFileContent;
            return this;
        }
        
        public ComparisonResult compare() {
            return diff.compare();
        }
    }
}
