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
import org.applecommander.os.DiskOptimizer;
import picocli.CommandLine.Command;

import java.util.Optional;

@Command(name = "optimize", aliases = { "krunch" }, description = "Optimize disk image.")
public class OptimizeCommand extends ReadWriteDiskCommandOptions {
    @Override
    public int handleCommand() throws Exception {
        int completed = 0;
        int skipped = 0;
        for (FormattedDisk disk : selectedDisks()) {
            Optional<DiskOptimizer> opt = disk.get(DiskOptimizer.class);
            if (opt.isEmpty()) {
                System.out.printf("Optimization not supported for disk %s of type %s.\n", disk.getFilename(), disk.getFormat());
                skipped++;
                continue;
            }
            DiskOptimizer optimizer = opt.get();
            if (!optimizer.canOptimizeDisk()) {
                System.out.printf("Disk %s does not need optimization.\n", disk.getFilename());
                skipped++;
                continue;
            }
            optimizer.optimizeDisk();
            completed++;
            System.out.printf("Disk %s has been optimized.\n", disk.getFilename());
        }
        System.out.printf("%d disks were optimized, %d disks were skipped.\n", completed, skipped);
        return 0;
    }
}
