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
package org.applecommander.os;

/// A simplistic set of Coordinates. Note that block only matches to block while track and sector have
/// two forms that match against each other.
public interface Coordinate {
    static Coordinate block(int block) {
        return new BlockCoordinate(block);
    }
    static Coordinate trackOnly(int track) {
        return new TrackCoordinate(track);
    }
    static Coordinate trackAndSector(int track, int sector) {
        return new TrackAndSectorCoordinate(track, sector);
    }

    /// Represents a (potentially) ProDOS, RDOS, CP/M, Pascal, or other block coordinate.
    record BlockCoordinate(int block) implements Coordinate {
        @Override
        public String toString() {
            return String.format("B%d", block);
        }
    }
    /// Represents a DOS track and sector coordinate. Potentially as a physical coordinate
    /// for a 140K floppy.
    record TrackAndSectorCoordinate(int track, int sector) implements Coordinate {
        @Override
        public boolean equals(Object obj) {
            return switch (obj) {
                case TrackAndSectorCoordinate tsCoordinate -> tsCoordinate.track == track && tsCoordinate.sector == sector;
                case TrackCoordinate trackCoordinate -> trackCoordinate.track == track;
                default -> false;
            };
        }
        @Override
        public String toString() {
            return String.format("T%d,S%d", track, sector);
        }
    }
    /// Represents a full track coordinate. Useful for full track matches or (possibly) a "nibblized"
    /// track.
    record TrackCoordinate(int track) implements Coordinate {
        @Override
        public boolean equals(Object obj) {
            return switch (obj) {
                case TrackAndSectorCoordinate tsCoordinate -> tsCoordinate.track == track;
                case TrackCoordinate trackCoordinate -> trackCoordinate.track == track;
                default -> false;
            };
        }
        @Override
        public String toString() {
            return String.format("T%d", track);
        }
    }
}
