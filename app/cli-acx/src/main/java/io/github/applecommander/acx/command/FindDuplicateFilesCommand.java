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

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.util.filestreamer.FileStreamer;
import com.webcodepro.applecommander.util.filestreamer.FileTuple;
import com.webcodepro.applecommander.util.filestreamer.TypeOfFile;

import io.github.applecommander.acx.base.ReadOnlyDiskImageCommandOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "dups", description = "Find duplicate files.")
public class FindDuplicateFilesCommand extends ReadOnlyDiskImageCommandOptions {
    @Option(names = { "-a", "--all" }, description = "Compare all files across all volumes; useful for formats like UniDOS.")
    private boolean compareAcrossVolumes;
    
    @Override
    public int handleCommand() throws Exception {
        Map<String,List<FileTuple>> content = new HashMap<>();
        Supplier<Map<String,List<FileTuple>>> supplier = () -> content;
        int dupsFound = 0;
        
        for (FormattedDisk formattedDisk : disk.getFormattedDisks()) {
            FileStreamer.forDisk(formattedDisk)
                    .includeTypeOfFile(TypeOfFile.FILE)
                    .recursive(true)
                    .stream()
                    .collect(Collectors.groupingBy(this::contentHash, supplier, Collectors.toList()));
            if (compareAcrossVolumes && !content.isEmpty()) {
                System.out.printf("Differences in: %s\n", formattedDisk.getDiskName());
                dupsFound += report(content);
                content.clear();
            }
        }
        
        if (!content.isEmpty()) {
            System.out.println("Differences:");
            dupsFound += report(content);
        }
        
        if (dupsFound == 0) {
            System.out.println("There are no duplicate files.");
        }
        
        return 0;
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
    
    private int report(Map<String,List<FileTuple>> content) {
        int dups = 0;
        for (Map.Entry<String,List<FileTuple>> entry : content.entrySet()) {
            if (entry.getValue().size() > 1) {
                dups++;
                boolean first = true;
                for (FileTuple tuple : entry.getValue()) {
                    if (first) {
                        System.out.printf("%s has the following duplicates:\n", tuple.fullPath());
                        first= false;
                    }
                    else {
                        System.out.printf("    - %s\n", tuple.fullPath());
                    }
                }
            }
        }
        return dups;
    }
}
