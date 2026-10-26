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
package org.applecommander.os.rdos;

public enum RDOSVersionAdapter {
    RDOS21("RDOS 2.1", 13, 23, 25, 24, 25),
    RDOS33("RDOS 3.3", 16, 31, 1, 2, 31);

    public final String label;
    public final int firstDirectoryBlock;
    public final int lastDirectoryBlock;
    public final int catalogCodeBlock;
    public final int chainCodeBlock;
    public final int lastSystemBlock;

    RDOSVersionAdapter(String label, int firstDirectoryBlock, int lastDirectoryBlock, int catalogCodeBlock,
                       int chainCodeBlock, int lastSystemBlock) {
        this.label = label;
        this.firstDirectoryBlock = firstDirectoryBlock;
        this.lastDirectoryBlock = lastDirectoryBlock;
        this.catalogCodeBlock = catalogCodeBlock;
        this.chainCodeBlock = chainCodeBlock;
        this.lastSystemBlock = lastSystemBlock;
    }
}
