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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.applecommander.javafx.FxUtils.createGenericSelectionPage;

public class SettingsDialog {
    private final ObjectProperty<ThemeSelection> themeSelection = new SimpleObjectProperty<>();
    private final ObjectProperty<ExportOption> exportOption = new SimpleObjectProperty<>();

    private final Dialog<ButtonType> dialog;

    public SettingsDialog() {
        Tab themeTab = new Tab("Theme");
        themeTab.setClosable(false);
        themeTab.setContent(createGenericSelectionPage("Please choose a theme:", ThemeSelection.values(),
                ThemeSelection::getDescription, themeSelection));

        Tab exportTab = new Tab("Export");
        exportTab.setClosable(false);
        exportTab.setContent(createGenericSelectionPage("Please select the export type:", ExportOption.values(),
                ExportOption::getDescription, exportOption));

        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(themeTab, exportTab);
        tabPane.setPadding(new Insets(10));

        dialog = new Dialog<>();
        dialog.setTitle("Settings");
        dialog.setHeaderText("Settings");
        dialog.getDialogPane().setContent(tabPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE, ButtonType.APPLY);

        themeSelection.addListener((_, _, newValue) -> {
            // This gives users a preview when clicking the radio buttons
            if (newValue != null) {
                newValue.apply();
                dialog.getDialogPane().layout();
                dialog.getDialogPane().getScene().getWindow().sizeToScene();
            }
        });
    }

    public void showAndWait(Stage parent) {
        Objects.requireNonNull(parent);

        Settings.getThemeSelection().ifPresent(themeSelection::set);
        exportOption.set(Settings.getExportOption());

        Window window = dialog.getDialogPane().getScene().getWindow();

        // See: https://stackoverflow.com/questions/40104688/javafx-center-child-stage-to-parent-stage
        // Calculate the center position of the parent Stage
        double centerXPosition = parent.getX() + parent.getWidth()/2d;
        double centerYPosition = parent.getY() + parent.getHeight()/2d;
        // Hide the pop-up stage before it is shown and becomes relocated
        window.setOnShown(event -> window.hide());
        // Relocate the pop-up Stage
        window.setOnShown(ev -> {
            window.setX(centerXPosition - window.getWidth()/2d);
            window.setY(centerYPosition - window.getHeight()/2d);
            dialog.show();
        });

        dialog.initOwner(parent);
        dialog.initOwner(parent);
        dialog.initModality(Modality.WINDOW_MODAL);
        Optional<ButtonType> button = dialog.showAndWait();
        button.ifPresent(result -> {
            if (result == ButtonType.APPLY) {
                if (themeSelection.isNotNull().get()) {
                    Settings.setThemeSelection(themeSelection.get());
                }
                if (exportOption.isNotNull().get()) {
                    Settings.setExportOption(exportOption.get());
                }
            }
        });
        // Always go back to what we've set
        Settings.getThemeSelection().ifPresent(ThemeSelection::apply);
    }
}
