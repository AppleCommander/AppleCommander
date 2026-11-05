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
package org.applecommander.device.nibble;

import org.applecommander.capability.CapabilityProvider;
import org.applecommander.util.DataBuffer;

/// Interface for the disk nibbilization routines.
public interface NibbleDiskCodec extends CapabilityProvider {

    /// Number of bytes in the raw (encoded) buffer.
    int encodedSize();

    /// Number of bytes in the decoded buffer. For Disk II devices, this is 256 bytes.
    int decodedSize();

    /// Decode the buffer from the raw disk bytes to actual data.
    DataBuffer decode(DataBuffer rawData);

    /// Encode the data buffer into disk bytes.
    DataBuffer encode(DataBuffer data);

    /// The read translation table.
    int[] readTranslateTable();

    /// The write translation table.
    int[] writeTranslateTable();
}
