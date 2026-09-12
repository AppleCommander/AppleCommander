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

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.File;
import java.util.Optional;
import java.util.prefs.Preferences;

public class AppleCommanderFX extends Application {
    private static final String IMAGE_DIRECTORY_KEY = "image_directory";

    @Override
    public void start(Stage stage) throws Exception {
        createWindow(stage, null);
    }

    public static void openNewWindow(File diskFile) {
        Stage stage = new Stage();
        try {
            createWindow(stage, diskFile);
            stage.toFront();
            stage.requestFocus();
        } catch (Exception ex) {
            throw new RuntimeException("Could not open new disk window", ex);
        }
    }

    private static void createWindow(Stage stage, File diskFile) throws Exception {
        FXMLLoader loader = new FXMLLoader(AppleCommanderFX.class.getResource("/fxml/FileStoreViewer.fxml"));
        Parent root = loader.load();

        FileStoreViewer controller = loader.getController();
        controller.setPrimaryStage(stage);

        Scene scene = new Scene(root, 1200, 700);

        stage.setTitle("AppleCommanderFX");
        stage.setScene(scene);

        // Bind keyboard shortcuts in controller
        try {
            controller.bindScene(scene);
        } catch (Exception ignored) {
        }

        stage.show();

        if (diskFile != null) {
            controller.openDiskFile(diskFile, false);
        }
    }

    static void main(String[] args) {
        launch(args);
    }

    public static void showErrorDialog(String message, Throwable t) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(message);
        alert.setContentText(t.getMessage() == null ? "An unexpected error occurred." : t.getMessage());
        alert.showAndWait();
    }

    public static Optional<File> getLastOpenedDirectory() {
        Preferences prefs = Preferences.userNodeForPackage(AppleCommanderFX.class);
        String directoryPath = prefs.get(IMAGE_DIRECTORY_KEY, null);
        if (directoryPath == null || directoryPath.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new File(directoryPath));
    }

    public static void setLastOpenedDirectory(File directory) {
        if (directory != null && directory.isDirectory()) {
            Preferences prefs = Preferences.userNodeForPackage(AppleCommanderFX.class);
            prefs.put(IMAGE_DIRECTORY_KEY, directory.getAbsolutePath());
        }
    }
}
