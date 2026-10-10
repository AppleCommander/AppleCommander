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

import com.webcodepro.applecommander.ui.AppleCommander;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.applecommander.javafx.settings.Settings;
import org.applecommander.javafx.settings.ThemeSelection;

import java.io.PrintWriter;
import java.io.StringWriter;

public class AppleCommanderFX extends Application {
    private static AppleCommanderFX application;

    @Override
    public void start(Stage stage) throws Exception {
        // This is really stupid the hoops just to launch the browser.
        application = this;
        // Based on user selection, use that theme.
        Settings.getThemeSelection().orElse(ThemeSelection.MODENA).apply();
        Platform.getPreferences().colorSchemeProperty().addListener((_, _, _) -> {
            Settings.getThemeSelection().orElse(ThemeSelection.MODENA).apply();
        });

        FileStoreWindow.createWindow(stage);
    }

    static void main(String[] args) {
        launch(args);
    }

    public static AppleCommanderFX getApplication() {
        return application;
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
}
