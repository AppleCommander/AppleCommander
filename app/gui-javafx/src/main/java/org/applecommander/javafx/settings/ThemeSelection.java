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

import java.util.List;

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
