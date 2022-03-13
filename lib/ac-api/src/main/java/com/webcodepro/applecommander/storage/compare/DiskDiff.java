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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskGeometry;
import com.webcodepro.applecommander.storage.DiskUnrecognizedException;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.physical.ImageOrder;

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
        
        for (int block=0; block<orderA.getBlocksOnDevice(); block++) {
            byte[] blockA = orderA.readBlock(block);
            byte[] blockB = orderB.readBlock(block);
            if (!Arrays.equals(blockA, blockB)) {
                results.addError("Block #%d does not match.", block);
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
            List<String> unequalSectors = new ArrayList<>();
            for (int sector=0; sector<orderA.getSectorsPerTrack(); sector++) {
                byte[] sectorA = orderA.readSector(track, sector);
                byte[] sectorB = orderB.readSector(track, sector);
                if (!Arrays.equals(sectorA, sectorB)) {
                    unequalSectors.add(Integer.toString(sector));
                }
            }
            if (!unequalSectors.isEmpty()) {
                results.addError("Track %d does not match on sectors %s", track,
                        String.join(",", unequalSectors));
            }
        }
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
        
        public ComparisonResult compare() {
            return diff.compare();
        }
    }
}
