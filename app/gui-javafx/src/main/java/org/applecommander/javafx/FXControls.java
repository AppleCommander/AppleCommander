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
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import java.util.function.Function;

/// This is a simple control builder for JavaFX components. It is intended to include bindings
/// that reflect the given property.
public class FXControls {
    public static FXControls builder() {
        return new FXControls();
    }

    private final VBox vbox;

    private FXControls() {
        vbox = new VBox();
        vbox.setSpacing(10);
        vbox.setPadding(new Insets(10));
    }

    public Node get() {
        return vbox;
    }

    public FXControls label(String fmt, Object... args) {
        Label label = new Label(String.format(fmt, args));
        label.setWrapText(true);
        vbox.getChildren().add(label);
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
            vbox.getChildren().add(radioButton);
        }
        return this;
    }

    public FXControls checkBox(String text, BooleanProperty property) {
        CheckBox checkBox = new CheckBox(text);
        checkBox.setSelected(property.get());
        // We cannot bind this property, need to add listeners in BOTH directions.
        checkBox.addEventHandler(ActionEvent.ACTION, _ -> property.set(checkBox.isSelected()));
        property.addListener((_, _, newValue) -> checkBox.setSelected(newValue));
        vbox.getChildren().add(checkBox);
        return this;
    }
}
