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

import java.util.List;
import java.util.Optional;

/**
 * Basic interface for the disk check (check disk, filesystem check, etc) for a disk. Any finding comes back in the list
 * and the CLI or GUI decides what to act upon.
 */
public interface DiskCheck {
    /** Perform the disk scan. Note that a generic Exception can be thrown simply because we're crossing between old an new interfaces! */
    List<Finding> scan() throws Exception;

    /** Encapsulates a single "finding" or problem. It may or may not be fixable via action. */
    record Finding(String description, Optional<Runnable> action, String classification, Coordinate coordinate) {}
}
