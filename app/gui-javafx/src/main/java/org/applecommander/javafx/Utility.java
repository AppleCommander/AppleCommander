package org.applecommander.javafx;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;

import java.util.Locale;
import java.util.Objects;

public class Utility {
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
        return Objects.requireNonNull(Utility.class.getResource("/images/" + imageName)).toExternalForm();
    }

    public static void applyShortcutToButton(Scene scene, ButtonBase button, String tooltipText, KeyCombination keyCombination, Runnable runnable) {
        scene.getAccelerators().put(keyCombination, runnable);
        button.setOnAction(e -> runnable.run());
        button.setTooltip(new Tooltip(String.format("%s (%s)", tooltipText, keyCombination.getDisplayText().toUpperCase(Locale.ROOT))));
    }
}
