/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2026 by Robert Greene and others
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

import com.webcodepro.applecommander.storage.FormattedDisk;
import io.github.applecommander.acx.base.ReadWriteDiskCommandOptions;
import org.applecommander.device.TrackSectorNibbleDevice;
import org.applecommander.device.TrackSectorNibbleDiskCheck;
import org.applecommander.os.DiskCheck;
import org.applecommander.os.DiskCheck.Finding;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

@Command(name = "check", description = "Check image for issues.")
public class CheckCommand extends ReadWriteDiskCommandOptions {
    @Option(names = { "--fix" }, description = "Fix defects (modifies image in place). Use classification, coordinate, or 'prompt'. Can combine.",
        split = ",")
    private final Set<String> fixes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    @Override
    public int handleCommand() throws Exception {
        int checkCount = 0;
        int findingCount = 0;
        for (FormattedDisk disk : selectedDisks()) {
            // Special case for nibble devices
            Optional<TrackSectorNibbleDevice> nibbleOpt = disk.get(TrackSectorNibbleDevice.class);
            if (nibbleOpt.isPresent()) {
                TrackSectorNibbleDiskCheck check = new TrackSectorNibbleDiskCheck(nibbleOpt.get());
                findingCount += handleDiskCheck(check);
            }

            Optional<DiskCheck> opt = disk.get(DiskCheck.class);
            if (opt.isEmpty()) {
                System.err.printf("Disk check not supported for disks of type '%s'.\n", disk.getFormat());
                continue;
            }
            checkCount++;
            findingCount += handleDiskCheck(opt.get());
        }
        if (checkCount > 0) {
            System.out.printf("Found %d items.\n", findingCount);
        }
        // If we processed _nothing_ treat it as an error.
        return checkCount == 0 ? 1 : 0;
    }

    public int handleDiskCheck(DiskCheck diskCheck) throws Exception {
        boolean prompt = false;
        final Set<String> classifications = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        final Set<String> coordinates = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String fix : fixes) {
            if ("prompt".equalsIgnoreCase(fix)) {
                prompt = true;
            }
            else if (fix.matches("(?i)b\\d+")) {
                coordinates.add(fix);
            }
            else if (fix.matches("(?i)t\\d+s\\d+")) {
                coordinates.add(fix);
            }
            else {
                classifications.add(fix);
            }
        }

        if (prompt && System.console() == null) {
            throw new RuntimeException("Console is not available. 'prompt' is invalid.");
        }

        // If we have fixes, assume "true" (which get ANDed for a valid condition).
        // If no fixing to be done, we skip applying it.
        Predicate<Finding> conditions = _ -> !fixes.isEmpty();
        if (!classifications.isEmpty()) {
            conditions = conditions.and(finding -> classifications.contains(finding.classification()));
        }
        if (!coordinates.isEmpty()) {
            conditions = conditions.and(finding -> coordinates.contains(finding.coordinate().toString()));
        }

        List<Finding> findings = diskCheck.scan();
        for (Finding finding : findings) {
            System.out.printf("[%-10s] %s @ %s\n", finding.classification(), finding.description(), finding.coordinate());
            if (finding.action().isPresent()) {
                if (conditions.test(finding)) {
                    if (prompt) {
                        String answer = System.console().readLine("Apply fix? [y/N] ");
                        if (answer == null) answer= "n";
                        answer = answer.trim().toLowerCase();
                        if (!answer.startsWith("y")) {
                            System.out.println(" -> Skipped.");
                            continue;
                        }
                    }
                    System.out.println(" -> Applying fix.");
                    finding.action().ifPresent(Runnable::run);
                }
            }
        }
        return findings.size();
    }
}
