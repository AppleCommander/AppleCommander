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
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.webcodepro.applecommander.storage.*;
import com.webcodepro.applecommander.util.Range;
import com.webcodepro.applecommander.util.filestreamer.FileStreamer;
import com.webcodepro.applecommander.util.filestreamer.FileTuple;
import com.webcodepro.applecommander.util.filestreamer.TypeOfFile;
import com.webcodepro.applecommander.util.readerwriter.FileEntryReader;
import org.applecommander.device.BlockDevice;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.util.DataBuffer;

/**
 * Perform a disk comparison based on selected strategy.
 */
public class DiskDiff {
    public static Builder create(FormattedDisk diskA, FormattedDisk diskB) {
        return new Builder(List.of(diskA), List.of(diskB));
    }
    public static Builder create(List<FormattedDisk> diskA, List<FormattedDisk> diskB) {
        return new Builder(diskA, diskB);
    }
    
    private List<FormattedDisk> diskA;
    private List<FormattedDisk> diskB;
    private ComparisonResult results = new ComparisonResult();
    
    private BiConsumer<FormattedDisk,FormattedDisk> diskComparisonStrategy = this::compareByNativeGeometry;
    
    private DiskDiff(List<FormattedDisk> diskA, List<FormattedDisk> diskB) {
        Objects.requireNonNull(diskA);
        Objects.requireNonNull(diskB);
        this.diskA = diskA;
        this.diskB = diskB;
    }
    
    public ComparisonResult compare() {
        if (diskA.isEmpty()) {
            results.addError("No disks identified for disk #1");
        }
        if (diskB.isEmpty()) {
            results.addError("No disks identified for disk #2");
        }

        if (!results.hasErrors()) {
            compareAll(diskA, diskB);
        }
        return results;
    }
    
    public void compareAll(List<FormattedDisk> formattedDisksA, List<FormattedDisk> formattedDisksB) {
        Objects.requireNonNull(formattedDisksA);
        Objects.requireNonNull(formattedDisksB);
        
        if (formattedDisksA.size() != formattedDisksB.size()) {
            results.addWarning("Cannot compare all disks; disk #1 has %d while disk #2 has %d.",
                    formattedDisksA.size(), formattedDisksB.size());
        }

        int min = Math.min(formattedDisksA.size(), formattedDisksB.size());
        for (int i=0; i<min; i++) {
            this.diskComparisonStrategy.accept(formattedDisksA.get(i), formattedDisksB.get(i));
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
        BlockDevice deviceA = BlockDeviceAdapter.from(formattedDiskA);
        BlockDevice deviceB = BlockDeviceAdapter.from(formattedDiskB);

        int blocksOnDeviceA = deviceA.getGeometry().blocksOnDevice();
        int blocksOnDeviceB = deviceB.getGeometry().blocksOnDevice();
        if (blocksOnDeviceA != blocksOnDeviceB) {
            results.addError("Different sized disks do not equal. (Blocks: %d <> %d)",
                    blocksOnDeviceA, blocksOnDeviceB);
            return;
        }

        List<Integer> unequalBlocks = new ArrayList<>();
        for (int block=0; block<blocksOnDeviceA; block++) {
            DataBuffer blockA = deviceA.readBlock(block);
            DataBuffer blockB = deviceB.readBlock(block);
            if (!blockA.equals(blockB)) {
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
        TrackSectorDevice deviceA = TrackSectorDeviceAdapter.from(formattedDiskA);
        TrackSectorDevice deviceB = TrackSectorDeviceAdapter.from(formattedDiskB);

        int sectorsPerDiskA = deviceA.getGeometry().sectorsPerDisk();
        int sectorsPerDiskB = deviceB.getGeometry().sectorsPerDisk();
        if (sectorsPerDiskA != sectorsPerDiskB) {
            results.addError("Different sized disks do not equal. (Sectors: %d <> %d)",
                    sectorsPerDiskA, sectorsPerDiskB);
            return;
        }
        
        for (int track=0; track<deviceA.getGeometry().tracksOnDisk(); track++) {
            List<Integer> unequalSectors = new ArrayList<>();
            for (int sector=0; sector<deviceA.getGeometry().sectorsPerTrack(); sector++) {
                DataBuffer sectorA = deviceA.readSector(track, sector);
                DataBuffer sectorB = deviceB.readSector(track, sector);
                if (!sectorA.equals(sectorB)) {
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
        Map<String,List<FileTuple>> filesA = FileStreamer.forDisks(formattedDiskA)
                .includeTypeOfFile(TypeOfFile.FILE)
                .recursive(true)
                .stream()
                .collect(Collectors.groupingBy(FileTuple::fullPath));
        Map<String,List<FileTuple>> filesB = FileStreamer.forDisks(formattedDiskB)
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
            FileTuple tupleA = tuplesA.getFirst();
            if (tuplesA.size() > 1) {
                results.addWarning("Path %s on disk %s has %d entries.", path, formattedDiskA.getFilename(), tuplesA.size());
            }
            FileTuple tupleB = tuplesB.getFirst();
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
    }

    /** Compare by file content. Accounts for content differences that are "only" in disk A or "only" in disk B. */
    public void compareByFileContent(FormattedDisk formattedDiskA, FormattedDisk formattedDiskB) {
        Map<String,List<FileTuple>> contentA = FileStreamer.forDisks(formattedDiskA)
                .includeTypeOfFile(TypeOfFile.FILE)
                .recursive(true)
                .stream()
                .collect(Collectors.groupingBy(this::contentHash));
        Map<String,List<FileTuple>> contentB = FileStreamer.forDisks(formattedDiskB)
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
            FileTuple tupleA = tuplesA.getFirst();
            if (tuplesA.size() > 1) {
                results.addWarning("Hash %s on disk %s has %d entries.", content,
                        formattedDiskA.getFilename(), tuplesA.size());
            }
            FileTuple tupleB = tuplesB.getFirst();
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
        private final DiskDiff diff;
        
        public Builder(List<FormattedDisk> diskA, List<FormattedDisk> diskB) {
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
