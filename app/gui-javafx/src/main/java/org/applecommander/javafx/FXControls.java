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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.function.Function;

/// This is a simple control builder for JavaFX components. It is intended to include bindings
/// that reflect the given property.
public class FXControls {
    public static FXControls vertical() {
        return new FXControls(new VBox());
    }
    public static FXControls horizontal() {
        return new FXControls(new HBox());
    }

    private final Pane pane;

    private FXControls(Pane pane) {
        this.pane = pane;
        spacing(10);
        pane.setPadding(new Insets(10));
    }

    public Node get() {
        return pane;
    }

    public FXControls alignment(Pos value) {
        if (pane instanceof VBox vbox) {
            vbox.setAlignment(value);
        }
        else if (pane instanceof HBox hbox) {
            hbox.setAlignment(value);
        }
        return this;
    }

    public FXControls spacing(double value) {
        if (pane instanceof VBox vbox) {
            vbox.setSpacing(value);
        }
        else if (pane instanceof HBox hbox) {
            hbox.setSpacing(value);
        }
        return this;
    }

    public FXControls node(Node node) {
        pane.getChildren().add(node);
        return this;
    }

    public FXControls label(String fmt, Object... args) {
        Label label = new Label(String.format(fmt, args));
        label.setWrapText(true);
        pane.getChildren().add(label);
        return this;
    }

    public FXControls link(String text, String url) {
        Hyperlink hyperlink = new Hyperlink(url);
        hyperlink.setText(text);
        hyperlink.setOnAction(event -> {
            AppleCommanderFX.getApplication().getHostServices().showDocument(url);
        });
        pane.getChildren().add(hyperlink);
        return this;
    }

    public FXControls largeBold(String fmt, Object... args) {
        Label label = new Label(String.format(fmt, args));
        label.setWrapText(true);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 150%");
        pane.getChildren().add(label);
        return this;
    }

    public FXControls separator() {
        if (pane instanceof VBox vbox) {
            vbox.getChildren().add(new Separator(Orientation.HORIZONTAL));
        }
        else if (pane instanceof HBox hbox) {
            hbox.getChildren().add(new Separator(Orientation.VERTICAL));
        }
        return this;
    }

    /// Add a set of radio buttons with options for all values of the given enumeration.
    public <E extends Enum<?>> FXControls radioButton(E[] enumerations, Function<E,String> textFn, ObjectProperty<E> property) {
        ToggleGroup selection = new ToggleGroup();
        for (E value : enumerations) {
            RadioButton radioButton = new RadioButton(textFn.apply(value));
            radioButton.setToggleGroup(selection);
            property.addListener((_, _, newValue) -> {
                radioButton.setSelected(newValue.equals(value));
            });
            radioButton.addEventHandler(ActionEvent.ACTION, _ -> property.set(value));
            property.addListener((_, _, newValue) -> radioButton.setSelected(newValue.equals(value)));
            pane.getChildren().add(radioButton);
        }
        return this;
    }

    public FXControls checkBox(String text, BooleanProperty property) {
        CheckBox checkBox = new CheckBox(text);
        checkBox.setSelected(property.get());
        // We cannot bind this property, need to add listeners in BOTH directions.
        checkBox.addEventHandler(ActionEvent.ACTION, _ -> property.set(checkBox.isSelected()));
        property.addListener((_, _, newValue) -> checkBox.setSelected(newValue));
        pane.getChildren().add(checkBox);
        return this;
    }

    public FXControls table(int columnCount, String... values) {
        assert values.length % columnCount == 0;
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(5);
        grid.setVgap(5);
        int col = 0;
        int row = 0;
        for (String value : values) {
            Label label = new Label(value);
            if (col == 0) label.setStyle("-fx-font-weight: bold;");
            else label.setWrapText(true);
            grid.add(label, col, row);
            col++;
            if (col >= columnCount) {
                col = 0;
                row++;
            }
        }
        pane.getChildren().add(grid);
        return this;
    }
}
