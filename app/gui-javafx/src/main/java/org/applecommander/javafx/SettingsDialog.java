package org.applecommander.javafx;

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

import static org.applecommander.javafx.FxUtils.createGenericSelectionPage;

public class SettingsDialog {
    private final ObjectProperty<AppleCommanderFX.ThemeSelection> themeSelection = new SimpleObjectProperty<>();
    private final ObjectProperty<ExportOption> exportOption = new SimpleObjectProperty<>();

    private final Dialog<ButtonType> dialog;

    public SettingsDialog() {
        Tab themeTab = new Tab("Theme");
        themeTab.setClosable(false);
        themeTab.setContent(createGenericSelectionPage("Please choose a theme:", AppleCommanderFX.ThemeSelection.values(),
                AppleCommanderFX.ThemeSelection::getDescription, themeSelection));

        Tab exportTab = new Tab("Export");
        exportTab.setClosable(false);
        exportTab.setContent(createGenericSelectionPage("Please select the export type:", ExportOption.values(),
                ExportOption::getDescription, exportOption));

        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(themeTab, exportTab);
        tabPane.setPadding(new Insets(10));

        dialog = new Dialog<>();
        dialog.setTitle("Settings");
        dialog.setHeaderText("AppleCommanderFX Settings");
        dialog.getDialogPane().setContent(tabPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE, ButtonType.APPLY);
    }

    public void showAndWait(Stage parent) {
        Objects.requireNonNull(parent);

        AppleCommanderFX.getThemeSelection().ifPresent(themeSelection::set);

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
                AppleCommanderFX.ThemeSelection theme = themeSelection.get();
                AppleCommanderFX.setThemeSelection(theme);
                theme.apply();
            }
        });
    }

    enum ExportOption {
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
