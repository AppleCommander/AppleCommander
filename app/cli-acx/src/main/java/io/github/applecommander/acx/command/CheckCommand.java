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
import org.applecommander.os.DiskCheck;
import org.applecommander.os.DiskCheck.Finding;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Optional;

@Command(name = "check", description = "Check image for issues.")
public class CheckCommand extends ReadWriteDiskCommandOptions {
    @Option(names = { "--fix" }, description = "Fix defects (modifies image in place).")
    private boolean fix;

    @Override
    public int handleCommand() throws Exception {
        int checkCount = 0;
        int findingCount = 0;
        for (FormattedDisk disk : selectedDisks()) {
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
        List<Finding> findings = diskCheck.scan();
        for (Finding finding : findings) {
            System.out.printf("[%-10.10s] %s @ %s\n", finding.classification(), finding.description(), finding.coordinate());
            if (fix) {
                finding.action().ifPresent(Runnable::run);
            }
        }
        return findings.size();
    }
}
