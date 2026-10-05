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

import atlantafx.base.theme.*;
import javafx.application.Application;
import javafx.application.Platform;

/// ThemeSelection encapsulates all the themes available as well as the logic to apply them.
public enum ThemeSelection {
    MODENA("Modena (JavaFX default)", Application.STYLESHEET_MODENA),
    CASPIAN("Caspian (JavaFX legacy)", Application.STYLESHEET_CASPIAN),
    PRIMER("Primer (AtlantaFX)", new PrimerLight(), new PrimerDark()),
    NORD("Nord (AtlantaFX)", new NordLight(), new NordDark()),
    CUPERTINO("Cupertino (AtlantaFX)", new CupertinoLight(), new CupertinoDark()),
    DRACULA("Dracula (AtlantaFX)", new Dracula());

    private final String description;
    private final String lightModeUrl;
    private final String darkModeUrl;

    /// This constructor is for JavaFX supplied themes. They are (apparently?) always a light mode theme.
    ThemeSelection(String description, String lightModeUrl) {
        assert description != null;
        assert lightModeUrl != null;
        this.description = description;
        this.lightModeUrl = lightModeUrl;
        this.darkModeUrl = null;
    }
    /// This constructor is for AtlantaFX supplied themes. They mostly have light and dark mode themes.
    ThemeSelection(String description, Theme... themes) {
        assert description != null;
        assert themes != null;
        assert themes.length >= 1 && themes.length <= 2;
        this.description = description;
        String lightModeUrl = null;
        String darkModeUrl = null;
        for (Theme theme : themes) {
            if (theme.isDarkMode()) {
                darkModeUrl = theme.getUserAgentStylesheet();
            }
            else {
                lightModeUrl = theme.getUserAgentStylesheet();
            }
        }
        this.lightModeUrl = lightModeUrl;
        this.darkModeUrl = darkModeUrl;
    }

    public String getDescription() {
        return description;
    }

    /// Apply the appropriate theme. Note that we don't always have the theme to match the system color scheme,
    /// so we fall back to the alternate theme in that case.
    public void apply() {
        String url = switch (Platform.getPreferences().getColorScheme()) {
            case LIGHT -> {
                if (includesLightMode()) yield lightModeUrl;
                else yield darkModeUrl;
            }
            case DARK -> {
                if (includesDarkMode()) yield darkModeUrl;
                else yield lightModeUrl;
            }
        };
        Application.setUserAgentStylesheet(url);
    }

    public boolean includesDarkMode() {
        return darkModeUrl != null;
    }

    public boolean includesLightMode() {
        return lightModeUrl != null;
    }
}
