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
package org.applecommander.javafx.settings;

import org.applecommander.javafx.AppleCommanderFX;

import java.io.File;
import java.util.Optional;
import java.util.prefs.Preferences;

public abstract class Settings {
    private static final String IMAGE_DIRECTORY_KEY = "image_directory";
    private static final String THEME_SELECTION = "theme_selection";
    private static final String EXPORT_OPTION = "export_option";

    public static Optional<File> getLastOpenedDirectory() {
        Preferences prefs = Preferences.userNodeForPackage(AppleCommanderFX.class);
        String directoryPath = prefs.get(IMAGE_DIRECTORY_KEY, null);
        if (directoryPath == null || directoryPath.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new File(directoryPath));
    }

    public static void setLastOpenedDirectory(File directory) {
        if (directory != null && directory.isDirectory()) {
            Preferences prefs = Preferences.userNodeForPackage(AppleCommanderFX.class);
            prefs.put(IMAGE_DIRECTORY_KEY, directory.getAbsolutePath());
        }
    }

    public static Optional<ThemeSelection> getThemeSelection() {
        Preferences prefs = Preferences.userNodeForPackage(AppleCommanderFX.class);
        String themeName = prefs.get(THEME_SELECTION, null);
        if (themeName == null || themeName.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(ThemeSelection.valueOf(themeName));
    }

    public static void setThemeSelection(ThemeSelection themeSelection) {
        Preferences prefs = Preferences.userNodeForPackage(AppleCommanderFX.class);
        prefs.put(THEME_SELECTION, themeSelection.name());
    }

    public static ExportOption getExportOption() {
        Preferences prefs = Preferences.userNodeForPackage(AppleCommanderFX.class);
        String exportName = prefs.get(EXPORT_OPTION, null);
        if (exportName == null || exportName.isBlank()) {
            return ExportOption.RAW_BINARY;
        }
        return ExportOption.valueOf(exportName);
    }

    public static void setExportOption(ExportOption exportOption) {
        Preferences prefs = Preferences.userNodeForPackage(AppleCommanderFX.class);
        prefs.put(EXPORT_OPTION, exportOption.name());
    }
}
