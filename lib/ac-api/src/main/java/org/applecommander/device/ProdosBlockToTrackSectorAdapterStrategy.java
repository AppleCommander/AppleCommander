/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2025 by Robert Greene and others
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
package org.applecommander.device;

public class ProdosBlockToTrackSectorAdapterStrategy implements TrackSectorToBlockStrategy {
    /**
     * This table contains the block offset for a particular DOS sector.
     */
    public static final int[] blockInterleave = {
        0, 7, 6, 6, 5, 5, 4, 4, 3, 3, 2, 2, 1, 1, 0, 7
    };
    /**
     * Defines the location within a block in which the DOS sector resides.
     * (0 = 0-255 and 1 = 256-511.)
     */
    public static final int[] blockOffsets = {
        0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1
    };

    @Override
    public int computeBlock(int track, int sector) {
        return track * 8 + blockInterleave[sector];
    }

    @Override
    public int computeOffset(int track, int sector) {
        return blockOffsets[sector] * TrackSectorDevice.SECTOR_SIZE;
    }

    @Override
    public int getTotalTracks() {
        return 35;
    }

    @Override
    public int getSectorsPerTrack() {
        return 16;
    }

    @Override
    public String getName() {
        return "Prodos block adapter";
    }
}
