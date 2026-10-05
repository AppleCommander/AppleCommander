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
package org.applecommander.source;

import org.applecommander.capability.CapabilityProvider;
import org.applecommander.hint.HintProvider;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.InformationProvider;

import java.io.IOException;
import java.util.Optional;

/// Source and Factory for an archive or disk in AppleCommander.
///
/// Typical usage:
///
/// ```java
/// import com.webcodepro.applecommander.storage.Sources;
/// import java.util.Optional;
/// Optional<source> sourceOpt = Sources.create(Path.of(filename));
/// if (sourceOpt.isPresent()) {
///     // do something with the Source
///     Source source = sourceOpt.get();
///     System.out.println(source.getSize());
/// }
/// else {
///     // Only happens if the initiating object is not understood
///     throw new RuntimeException("Unable to create image source");
/// }
/// ```
///
/// Optionally, you can use the Optional's more _fluent_ API and write
/// code like this:
///
/// ```java
/// import com.webcodepro.applecommander.storage.Sources;
/// Sources.create(Path.of(filename)).ifPresent(source -> {
///     // do something with the Source
///     System.out.println(source.getSize());
/// });
/// ```
public interface Source extends CapabilityProvider, HintProvider, Container, InformationProvider {
    /// Indicates the physical size of this image on disk.
    int getSize();

    /// The name of the source. It likely will be used in user interfaces. It could represent a file
    /// from the computer or even a file from a file store.
    String getName();

    /// Read all bytes from this source.
    DataBuffer readAllBytes();

    /// Read a range of bytes from this source.
    DataBuffer readBytes(int offset, int length);

    /// Write bytes to this source. Updates the changed status.
    void writeBytes(int offset, DataBuffer data);

    /// Indicates if this source has changed.
    boolean hasChanged();

    /// Clears the changed indicator.
    void clearChanges();

    /// Save the image back to the origin. Note that this could conceivably save back to the
    /// OS file system, or back to a file store. Use the `CapabilityProvider#can` to see if
    /// a save is possible. The save operation should clear the change indicator.
    ///
    /// For example:
    ///
    /// ```java
    /// if (source.can(Capability.SAVE_SOURCE)) {
    ///     source.save();
    /// }
    /// ```
    ///
    /// Note that the capability can be used to enable appropriate options. If source _cannot_
    /// be saved, a "Save As" type operation can be applied and the source switched for the
    /// `FileStore` in question.
    ///
    /// @throws IOException if an error occurs while saving the source.
    /// @throws UnsupportedOperationException if the source does not support the save operation.
    default void save() throws IOException {
        throw new UnsupportedOperationException("save operation not supported");
    }

    /// Indicates if the source image is approximately equal to this size
    /// (less any image over-read).
    /// Currently, hardcoded to allow up to 255 extra bytes at the end of a
    /// disk image.  Must be at least the requested size!
    default boolean isApproxEQ(int value) {
        return getSize() >= value && getSize() <= value + 255;
    }

    /// Indicates if the source image is "approximately" between
    /// these two values (less any image over-read).
    /// Currently, hardcoded to allow up to 255 extra bytes at the end of a
    /// disk image.  Must be at least the requested size!
    default boolean isApproxBetween(int value1, int value2) {
        return getSize() >= value1 && getSize() <= value2 + 255;
    }

    /// Test if this name has a given file extension.
    default boolean extensionLike(String extension) {
        String name = getName().toLowerCase();
        String ext1 = String.format(".%s", extension.toLowerCase());
        String ext2 = String.format(".%s.gz", extension.toLowerCase());
        return name.endsWith(ext1) || name.endsWith(ext2);
    }

    /// This is the Source creation factory interface.
    interface Factory {
        Optional<Source> fromObject(Object object);
        Optional<Source> fromSource(Source source);
    }
}
