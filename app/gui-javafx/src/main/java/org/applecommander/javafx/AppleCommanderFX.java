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

    @Override
    public void start(Stage stage) throws Exception {
        // Based on user selection, use that theme.
        getThemeSelection().orElse(ThemeSelection.MODENA).urls.forEach(Application::setUserAgentStylesheet);

        FileStoreViewer.createWindow(stage);
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

    public enum ThemeSelection {
        MODENA(Application.STYLESHEET_MODENA),
        PRIMER(new PrimerLight().getUserAgentStylesheet(), new PrimerDark().getUserAgentStylesheet()),
        NORD(new NordLight().getUserAgentStylesheet(), new NordDark().getUserAgentStylesheet()),
        CUPERTINO(new CupertinoLight().getUserAgentStylesheet(), new CupertinoDark().getUserAgentStylesheet()),
        DRACULA(new Dracula().getUserAgentStylesheet());

        public final List<String> urls;

        ThemeSelection(String... urls) {
            this.urls = List.of(urls);
        }

        public boolean includesDarkMode() {
            return this != MODENA;
        }
        public boolean includesLightMode() {
            return this != DRACULA;
        }
    }
}
