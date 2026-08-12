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
package org.applecommander.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/// Provides a simple but automated backup strategy to the `acx` tool.
///
/// General usage pattern is as follows:
/// ```java
/// // Only save if there are changes.
/// if (disk.getSource().hasChanged()) {
///     backupStrategy.backup(disk.getFilename());
///     disk.save();
/// }
/// ```
///
/// To create a `BackupStrategy` in a consistent manner, use the #create
/// method:
/// ```'java
/// BackupStrategy backupStrategy = BackupStrategy.create("bak");
/// ```
///
/// If using in a context where a backup is no expected (as in a new file, or
/// a "save as" situation), simply use `BackupStrategy.none()`.
///
/// The coding is as follows:
/// * `null` or blank (`""`): Returns an ineffective strategy. (No branches in code.)
/// * `bak`: Returns a backup strategy that renames the existing file from `name.dsk`
///   to `name.dsk.bak`, overwriting any existing files.
/// * The default strategy is to assume the value is a directory, _which will be created_,
///   and make a copy of the file into this directory.
///
/// @see #create(String)
/// @see #none()
public interface BackupStrategy {
        void backup(String filename) throws IOException;

        static BackupStrategy create(String backupCode) {
            if (backupCode == null) backupCode = "";
            return switch (backupCode) {
                case "" -> BackupStrategy.none();
                case "bak" -> BackupStrategy.fileExtension("bak");
                default -> BackupStrategy.directory(backupCode);
            };
        }

        static BackupStrategy none() {
            return filename -> {
                // Do nothing
            };
        }

        static BackupStrategy fileExtension(final String extension) {
            return filename -> {
                Path src = Path.of(filename);
                Path dst = Path.of(filename + "." + extension);
                Files.copy(src, dst, REPLACE_EXISTING);
            };
        }

        static BackupStrategy directory(final String directory) {
            return filename -> {
                Path src = Path.of(filename);
                Path backupDir = Path.of(directory);
                Files.createDirectories(backupDir);
                Path dst = backupDir.resolve(src.getFileName());
                Files.copy(src, dst, REPLACE_EXISTING);
            };
        }
    }