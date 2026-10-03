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
package org.applecommander.javafx.wizard;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

public abstract class WizardDialog<T extends Enum<?>> {
    private final BorderPane window;
    private final StackPane content;

    private Stage stage;
    private final Stack<T> pageHistory = new Stack<>();
    private final ObjectProperty<T> currentPage = new SimpleObjectProperty<>();
    private final Map<T,Node> wizardPages = new HashMap<>();
    private final ObjectProperty<ButtonBar.ButtonData> result = new SimpleObjectProperty<>();

    protected WizardDialog() {
        content = new StackPane();
        window = new BorderPane();
    }

    protected void initializeWizard(String imageName, T initialPage, T finalPage) {
        URL imageURL = getClass().getResource(imageName);
        Objects.requireNonNull(imageURL);

        ImageView imageView = new ImageView(imageURL.toExternalForm());
        VBox imagePane = new VBox(imageView);
        imagePane.setPadding(new Insets(0, 0, 10, 0));
        imagePane.setStyle("-fx-background-color: white;");
        imagePane.setAlignment(Pos.CENTER);

        wizardPages.putAll(createWizardPages());
        content.getChildren().addAll(wizardPages.values());

        Button nextButton = new Button("Next >");
        ButtonBar.setButtonData(nextButton, ButtonBar.ButtonData.NEXT_FORWARD);
        nextButton.setOnAction(_ -> {
            pageHistory.push(currentPage.get());
            currentPage.set(nextPage());
        });
        Button previousButton = new Button("< Previous");
        ButtonBar.setButtonData(previousButton, ButtonBar.ButtonData.BACK_PREVIOUS);
        previousButton.setOnAction(_ -> {
            currentPage.set(pageHistory.pop());
        });
        Button finishButton = new Button("Finish");
        ButtonBar.setButtonData(finishButton, ButtonBar.ButtonData.FINISH);
        finishButton.setOnAction(_ -> {
            result.set(ButtonBar.ButtonData.FINISH);
            this.stage.close();
        });
        Button cancelButton = new Button("Cancel");
        ButtonBar.setButtonData(cancelButton, ButtonBar.ButtonData.CANCEL_CLOSE);
        cancelButton.setOnAction(_ -> {
            result.set(ButtonBar.ButtonData.CANCEL_CLOSE);
            this.stage.close();
        });
        ButtonBar buttonBar = new ButtonBar();
        buttonBar.setPadding(new Insets(10));
        buttonBar.getButtons().addAll(nextButton, previousButton, finishButton, cancelButton);

        wizardPages.forEach((key, value) -> {
            value.visibleProperty().bind(currentPageProperty().isEqualTo(key));
        });
        nextButton.disableProperty().bind(createNextPageBinding());
        previousButton.disableProperty().bind(currentPage.isEqualTo(initialPage));
        finishButton.disableProperty().bind(currentPage.isNotEqualTo(finalPage).and(completedWizardBinding().not()));

        window.setTop(imagePane);
        window.setCenter(content);
        window.setBottom(buttonBar);

        // First page!
        currentPage.set(initialPage);
    }
    public void initStage(Stage stage) {
        this.stage = stage;
    }

    public ObjectProperty<T> currentPageProperty() {
        return currentPage;
    }

    public boolean showAndWait(Stage parent) {
        Objects.requireNonNull(parent);

        Scene scene = new Scene(window);
        Stage stage = new Stage();
        this.initStage(stage);
        stage.setScene(scene);

        // See: https://stackoverflow.com/questions/40104688/javafx-center-child-stage-to-parent-stage
        // Calculate the center position of the parent Stage
        double centerXPosition = parent.getX() + parent.getWidth()/2d;
        double centerYPosition = parent.getY() + parent.getHeight()/2d;
        // Hide the pop-up stage before it is shown and becomes relocated
        stage.setOnShown(event -> stage.hide());
        // Relocate the pop-up Stage
        stage.setOnShown(ev -> {
            stage.setX(centerXPosition - stage.getWidth()/2d);
            stage.setY(centerYPosition - stage.getHeight()/2d);
            stage.show();
        });

        stage.initOwner(parent);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.toFront();
        stage.requestFocus();
        stage.showAndWait();

        return result.isEqualTo(ButtonBar.ButtonData.FINISH).get();
    }

    public abstract T nextPage();
    public abstract BooleanBinding createNextPageBinding();
    public abstract BooleanBinding completedWizardBinding();
    public abstract Map<T,Node> createWizardPages();
}
