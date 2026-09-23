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

import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Border;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.TilePane;
import javafx.scene.paint.Color;
import org.applecommander.filestore.FileStore;

public class InformationView extends ScrollPane {
    private final TilePane tilePane;

    public InformationView(FileStoreWindow fileStoreWindow) {
        fileStoreWindow.fileStoreSelection().selectedItemProperty().addListener(this::selectedFileStoreChanged);

        tilePane = new TilePane();
        tilePane.setHgap(10);
        tilePane.setVgap(10);
        tilePane.setPadding(new Insets(10, 10, 10, 10));
        setContent(tilePane);

        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

        visibleProperty().bind(fileStoreWindow.viewModeProperty().isEqualTo(ViewMode.INFORMATION));
        managedProperty().bind(visibleProperty());
    }

    public void selectedFileStoreChanged(ObservableValue<? extends FileStore> observable, FileStore oldValue, FileStore newValue) {
        tilePane.getChildren().clear();
        if (newValue != null) {
            newValue.information().forEach(group -> {
                GridPane gridPane = new GridPane(5,5);
                gridPane.setPadding(new Insets(10,10,10,10));
                Label title = new Label("--- " + group.title() + " ---");
                gridPane.add(title, 0, 0, 2, 1);
                GridPane.setHalignment(title, HPos.CENTER);
                for (int i=0; i<group.items().size(); i++) {
                    var item = group.items().get(i);
                    Label label = new Label(item.label());
                    Label value = new Label(item.value());
                    value.setWrapText(true);
                    gridPane.addRow(i+1, label, value);
                }
                gridPane.setBorder(Border.stroke(Color.BLACK));
                tilePane.getChildren().add(gridPane);
            });
        }
    }
}
