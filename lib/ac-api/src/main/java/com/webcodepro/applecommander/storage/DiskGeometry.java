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
package com.webcodepro.applecommander.storage;

/**
 * Indicates the broad disk geometry - track/sector or block.
 * Note that BLOCK is meant to include only ProDOS/Pascal 512-byte
 * blocks and not the RDOS 256 "blocks" (RDOS should remain under
 * the track/sector geometry.)
 */
public enum DiskGeometry {
    TRACK_SECTOR(256, "Track/Sector"),
    BLOCK(512, "Block");
    
    public final int size;
    public final String text;
    
    private DiskGeometry(int size, String text) {
        this.size = size;
        this.text = text;
    }
}
