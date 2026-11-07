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

import com.webcodepro.applecommander.util.AppleUtil;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.applecommander.converter.ConverterRepository;
import org.applecommander.converter.TextConverter;
import org.applecommander.filestore.ContentType;
import org.applecommander.filestore.FileEntry;

public class FileContentWindow {
    private final BorderPane window;
    private final StackPane content;
    private final TextFlow textView;
    private final FileEntry fileEntry;

    private Stage stage;

    public FileContentWindow(FileEntry fileEntry) {
        this.window = new BorderPane();
        this.textView = new TextFlow();
        this.content = new StackPane(textView);
        this.fileEntry = fileEntry;

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        this.window.setCenter(scrollPane);
    }

    public void show() {
        Scene scene = new Scene(window, 800, 500);
        stage = new Stage();
        stage.setScene(scene);
        stage.toFront();
        stage.requestFocus();
        this.initView();
        stage.show();
    }

    public void close() {
        stage.close();
    }

    private void initView() {
        ContentType contentType = fileEntry.getContentType();
        if (contentType.subtype == ContentType.Subtype.TEXT) {
            TextConverter textConverter = ConverterRepository.getTextConverter(contentType).orElseThrow();
            showPlainText(textConverter.read(fileEntry));
        }
        else {
            // HEX VIEW FIXME
            showPlainText(AppleUtil.getHexDump(fileEntry.getDataFork().asBytes()));
        }
    }

    private void showPlainText(String value) {
        Text text = new Text(value);
        text.setFont(Font.font("monospaced"));
        textView.getChildren().clear();
        textView.getChildren().add(text);
        showThisButHideOthers(textView);
    }

    private void showThisButHideOthers(Node nodeToShow) {
        content.getChildren().forEach(node -> {
            boolean showing = node == nodeToShow;
            node.setVisible(showing);
            node.setManaged(showing);
        });
    }
}
