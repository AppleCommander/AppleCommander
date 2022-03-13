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
package io.github.applecommander.acx.command;

import java.util.function.Consumer;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.compare.ComparisonResult;
import com.webcodepro.applecommander.storage.compare.DiskDiff;

import io.github.applecommander.acx.base.ReadOnlyDiskImageCommandOptions;
import io.github.applecommander.acx.converter.DiskConverter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "compare", description = "Compare two disk images.")
public class CompareCommand extends ReadOnlyDiskImageCommandOptions {
    @Parameters(arity = "1", converter = DiskConverter.class, description = "Second image to compare to.")
    private Disk disk2;
    
    @ArgGroup(heading = "%nComparison Strategy Selection:%n")
    private StrategySelection strategySelection = new StrategySelection();

    @Override
    public int handleCommand() throws Exception {
        DiskDiff.Builder builder = DiskDiff.create(disk, disk2);
        strategySelection.strategy.accept(builder);
        ComparisonResult result = builder.compare();
        
        if (result.getDifferenceCount() == 0) {
            System.out.println("The disks match.");
        }
        else {
            System.out.println("The disks do not match.");
            result.getErrors().forEach(System.out::println);
            result.getWarnings().forEach(System.out::println);
        }
                    
        return 0;
    }
    
    public static class StrategySelection {
        private Consumer<DiskDiff.Builder> strategy = this::nativeGeometry;
        
        @Option(names = "--native", description = "Compare by native geometry.")
        private void selectNativeGeometry(boolean flag) {
            strategy = this::nativeGeometry;
        }
        @Option(names = "--block", description = "Compare by block geometry.")
        private void selectBlockGeometry(boolean flag) {
            strategy = this::blockGeometry;
        }
        @Option(names = { "--track-sector", "--ts" }, description = "Compare by track/sector geometry.")
        private void selectTrackSectorGeometry(boolean flag) {
            strategy = this::trackSectorGeometry;
        }
        
        private void nativeGeometry(DiskDiff.Builder builder) {
            builder.selectCompareByNativeGeometry();
        }
        private void blockGeometry(DiskDiff.Builder builder) {
            builder.selectCompareByBlockGeometry();
        }
        private void trackSectorGeometry(DiskDiff.Builder builder) {
            builder.selectCompareByTrackSectorGeometry();
        }
    }
}
