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
package org.applecommander.javafx;

import atlantafx.base.theme.*;
import com.webcodepro.applecommander.ui.AppleCommander;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

public class AppleCommanderFX extends Application {
    private static final String IMAGE_DIRECTORY_KEY = "image_directory";
    private static final String THEME_SELECTION = "theme_selection";
    private static final String EXPORT_OPTION = "export_option";

    @Override
    public void start(Stage stage) throws Exception {
        // Based on user selection, use that theme.
        getThemeSelection().orElse(ThemeSelection.MODENA).apply();

        FileStoreWindow.createWindow(stage);
    }

    static void main(String[] args) {
        launch(args);
    }

    public static String buildTitle(String... args) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.printf("AppleCommanderFX %s (BETA)", AppleCommander.VERSION);
        if (args.length > 0) {
            pw.print(" -");
            for (String arg : args) {
                pw.print(" ");
                pw.print(arg);
            }
        }
        return sw.toString();
    }

    public static void showErrorDialog(String message, Throwable t) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(message);
        alert.setContentText(t.getMessage() == null ? "An unexpected error occurred." : t.getMessage());
        alert.showAndWait();
    }

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

    public static Optional<ExportOption> getExportOption() {
        Preferences prefs = Preferences.userNodeForPackage(AppleCommanderFX.class);
        String exportName = prefs.get(EXPORT_OPTION, null);
        if (exportName == null || exportName.isBlank()) {
            return Optional.of(ExportOption.RAW_BINARY);
        }
        return Optional.of(ExportOption.valueOf(exportName));
    }
    public static void setExportOption(ExportOption exportOption) {
        Preferences prefs = Preferences.userNodeForPackage(AppleCommanderFX.class);
        prefs.put(EXPORT_OPTION, exportOption.name());
    }

    public enum ThemeSelection {
        MODENA("Modena (JavaFX default)", Application.STYLESHEET_MODENA),
        CASPIAN("Caspian (JavaFX legacy)", Application.STYLESHEET_CASPIAN),
        PRIMER("Primer (AtlantaFX)", new PrimerLight().getUserAgentStylesheet(), new PrimerDark().getUserAgentStylesheet()),
        NORD("Nord (AtlantaFX)", new NordLight().getUserAgentStylesheet(), new NordDark().getUserAgentStylesheet()),
        CUPERTINO("Cupertino (AtlantaFX)", new CupertinoLight().getUserAgentStylesheet(), new CupertinoDark().getUserAgentStylesheet()),
        DRACULA("Dracula (AtlantaFX)", new Dracula().getUserAgentStylesheet());

        private final String description;
        private final List<String> urls;

        ThemeSelection(String description, String... urls) {
            this.description = description;
            this.urls = List.of(urls);
        }

        public String getDescription() {
            return description;
        }

        public void apply() {
            urls.forEach(Application::setUserAgentStylesheet);
        }

        public boolean includesDarkMode() {
            return this != MODENA;
        }
        public boolean includesLightMode() {
            return this != DRACULA;
        }
    }

    public enum ExportOption {
        RAW_BINARY("Raw Binary (filename)"),
        APPLE_SINGLE("AppleSingle (filename.as)"),
        ATTRIBUTE_PRESERVATION("ProDOS Attribute Preservation (filename#TTAAAA)");

        private final String description;

        ExportOption(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
