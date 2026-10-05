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

import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

public class FxUtils {
    public static Button createButton(String url, String text, EventHandler<ActionEvent> eventHandler) {
        ImageView imageView = new ImageView(imageUrl(url));
        imageView.setFitHeight(24);
        imageView.setFitWidth(24);
        imageView.setPreserveRatio(true);
        Label label = new Label(text);
        VBox vbox = new VBox(imageView, label);
        vbox.setSpacing(2);
        vbox.setAlignment(Pos.CENTER);
        Button button = new Button();
        button.setGraphic(vbox);
        button.setOnAction(eventHandler);
        return button;
    }

    public static ToggleButton createToggleButton(String url, String text, ToggleGroup toggleGroup,
                                           EventHandler<ActionEvent> eventHandler) {
        ImageView imageView = new ImageView(imageUrl(url));
        imageView.setFitHeight(24);
        imageView.setFitWidth(24);
        Label label = new Label(text);
        VBox vbox = new VBox(imageView, label);
        vbox.setSpacing(2);
        vbox.setAlignment(Pos.CENTER);
        ToggleButton button = new ToggleButton();
        button.setGraphic(vbox);
        button.setOnAction(eventHandler);
        button.setToggleGroup(toggleGroup);
        return button;
    }

    public static ToggleButton createOnOffButton(String urlOff, String urlOn, String text, EventHandler<ActionEvent> eventHandler) {
        ImageView imageView = new ImageView(imageUrl(urlOff));
        imageView.setFitHeight(24);
        imageView.setFitWidth(24);
        Label label = new Label(text);
        VBox vbox = new VBox(imageView, label);
        vbox.setSpacing(2);
        vbox.setAlignment(Pos.CENTER);
        ToggleButton button = new ToggleButton();
        button.setGraphic(vbox);
        button.setOnAction(eventHandler);
        button.selectedProperty().addListener((_, _, _) -> {
            String imageUrl = button.isSelected()  ? urlOn : urlOff;
            imageView.setImage(new Image(imageUrl(imageUrl)));
        });
        return button;
    }

    public static String imageUrl(String imageName) {
        return Objects.requireNonNull(FxUtils.class.getResource("/images/" + imageName)).toExternalForm();
    }

    public static void applyShortcutToButton(Scene scene, ButtonBase button, String tooltipText, KeyCombination keyCombination, Runnable runnable) {
        scene.getAccelerators().put(keyCombination, runnable);
        button.setOnAction(e -> runnable.run());
        button.setTooltip(new Tooltip(String.format("%s (%s)", tooltipText, keyCombination.getDisplayText().toUpperCase(Locale.ROOT))));
    }

    public static <E extends Enum<?>> Node createGenericSelectionPage(String prompt, E[] enumerations,
                                                               Function<E,String> textFn, ObjectProperty<E> property) {
        VBox selectionPage = new VBox();
        selectionPage.setSpacing(10);
        selectionPage.setPadding(new Insets(10));
        Label chooseLabel = new Label(prompt);
        selectionPage.getChildren().add(chooseLabel);
        ToggleGroup selection = new ToggleGroup();
        for (E value : enumerations) {
            RadioButton radioButton = new RadioButton(textFn.apply(value));
            radioButton.setToggleGroup(selection);
            property.addListener((_, _, newValue) -> {
                radioButton.setSelected(newValue.equals(value));
            });
            radioButton.addEventHandler(ActionEvent.ACTION, _ -> property.set(value));
            selectionPage.getChildren().add(radioButton);
        }
        return selectionPage;
    }

    public static void showErrorDialog(String title, Throwable t) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(t.getMessage() == null ? "An unexpected error occurred." : t.getMessage());
        // Generate the stack trace
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        alert.setContentText(sw.toString());

        alert.showAndWait();
    }
}
