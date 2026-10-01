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

import com.webcodepro.applecommander.storage.DiskConstants;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.applecommander.device.DosOrderedTrackSectorDevice;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.os.gamedos.GamedosFileStoreFactory;
import org.applecommander.source.DataBufferSource;
import org.applecommander.source.Source;

import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Function;

public class CreateFileStoreWizard {
    private final BorderPane window;
    private final StackPane content;

    private Stage stage;
    private final Stack<WizardPage> pageHistory = new Stack<>();
    private final ObjectProperty<WizardPage> currentPage = new SimpleObjectProperty<>();
    private final Map<WizardPage,Node> wizardPages;
    private final ObjectProperty<FileStoreSelection> fileStoreSelectionProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<ImageSizeSelection> imageSizeSelectionProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<SectorOrderSelection> sectorOrderSelectionProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<ButtonBar.ButtonData> result = new SimpleObjectProperty<>();

    public CreateFileStoreWizard() {
        URL imageURL = getClass().getResource("/images/DiskImageWizardLogo.png");
        Objects.requireNonNull(imageURL);

        ImageView imageView = new ImageView(imageURL.toExternalForm());
        VBox imagePane = new VBox(imageView);
        imagePane.setPadding(new Insets(0, 0, 10, 0));
        imagePane.setStyle("-fx-background-color: white;");
        imagePane.setAlignment(Pos.CENTER);

        Node selectionPane = createGenericSelectionPage("Choose an image type to create:",
                FileStoreSelection.values(), FileStoreSelection::getText, fileStoreSelectionProperty);
        Node imageSizePane = createGenericSelectionPage("Choose an image size to create:",
                ImageSizeSelection.values(), ImageSizeSelection::getText, imageSizeSelectionProperty);
        Node sectorOrderPane = createGenericSelectionPage("Choose a sector ordering for this image:",
                SectorOrderSelection.values(), SectorOrderSelection::getText, sectorOrderSelectionProperty);
        Node summaryPane = createSummaryPage();

        wizardPages = Map.of(
                WizardPage.FILESTORE, selectionPane,
                WizardPage.SIZE, imageSizePane,
                WizardPage.SECTOR, sectorOrderPane,
                WizardPage.SUMMARY, summaryPane
        );

        content = new StackPane(selectionPane, imageSizePane, sectorOrderPane, summaryPane);
        content.setPadding(new Insets(10));

        Button nextButton = new Button("Next >");
        ButtonBar.setButtonData(nextButton, ButtonBar.ButtonData.NEXT_FORWARD);
        nextButton.setOnAction(_ -> {
            currentPage.set(nextPage());
            pageHistory.push(currentPage.get());
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

        currentPage.addListener((_, _, newPage) -> {
            wizardPages.forEach((key, value) -> value.setVisible(key == newPage));
            finishButton.setDisable(newPage != WizardPage.SUMMARY);
            previousButton.setDisable(pageHistory.empty());
        });
        nextButton.disableProperty().bind(currentPage.isEqualTo(WizardPage.FILESTORE).and(fileStoreSelectionProperty.isNull())
                .or(currentPage.isEqualTo(WizardPage.SIZE).and(imageSizeSelectionProperty.isNull()))
                .or(currentPage.isEqualTo(WizardPage.SECTOR).and(sectorOrderSelectionProperty.isNull()))
                .or(currentPage.isEqualTo(WizardPage.SUMMARY)));

        window = new BorderPane();
        window.setTop(imagePane);
        window.setCenter(content);
        window.setBottom(buttonBar);

        // First page!
        currentPage.set(WizardPage.FILESTORE);
    }
    public void initStage(Stage stage) {
        this.stage = stage;
    }

    public Optional<Source> showAndWait(Stage parent) {
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

        if (result.isEqualTo(ButtonBar.ButtonData.FINISH).get()) {
            // FAKING IT!
            Source source = DataBufferSource.create(DiskConstants.APPLE_140KB_DISK, "BLANK.DISK").get();
            TrackSectorDevice device = new DosOrderedTrackSectorDevice(source);
            GamedosFileStoreFactory.create(device);
            return Optional.of(source);
        }
        return Optional.empty();
    }

    private <T extends Enum<?>> Node createGenericSelectionPage(String prompt, T[] enumerations,
                                                                Function<T,String> textFn, ObjectProperty<T> property) {
        VBox selectionPage = new VBox();
        selectionPage.setSpacing(10);
        Label chooseLabel = new Label(prompt);
        selectionPage.getChildren().add(chooseLabel);
        ToggleGroup selection = new ToggleGroup();
        for (T value : enumerations) {
            RadioButton radioButton = new RadioButton(textFn.apply(value));
            radioButton.setToggleGroup(selection);
            radioButton.addEventHandler(ActionEvent.ACTION, _ -> property.set(value));
            selectionPage.getChildren().add(radioButton);
        }
        return selectionPage;
    }

    private Node createSummaryPage() {
        GridPane summaryPage = new GridPane();
        summaryPage.setHgap(10);
        summaryPage.setVgap(10);
        summaryPage.setPadding(new Insets(10));
        Label summaryLabel = new Label("Summary:");
        summaryPage.add(summaryLabel, 0, 0);
        GridPane.setColumnSpan(summaryPage, 2);
        Label fileStoreSelectionLabel = new Label();
        fileStoreSelectionProperty.addListener((_, _, newValue) -> {
            fileStoreSelectionLabel.setText(newValue.getText());
        });
        summaryPage.addRow(1, new Label("File Store:"), fileStoreSelectionLabel);
        Label imageSizeSelectionLabel = new Label();
        imageSizeSelectionProperty.addListener((_, _, newValue) -> {
            imageSizeSelectionLabel.setText(newValue.getText());
        });
        summaryPage.addRow(2, new Label("Image Size:"), imageSizeSelectionLabel);
        Label sectorOrderSelectionLabel = new Label();
        sectorOrderSelectionProperty.addListener((_, _, newValue) -> {
            sectorOrderSelectionLabel.setText(newValue.getText());
        });
        summaryPage.addRow(3, new Label("Sector Order:"), sectorOrderSelectionLabel);
        return summaryPage;
    }

    // FIXME need real logic!
    private WizardPage nextPage() {
        return switch (currentPage.get()) {
            case FILESTORE -> {
                if (fileStoreSelectionProperty.getValue() == FileStoreSelection.GAMEDOS) {
                    imageSizeSelectionProperty.set(ImageSizeSelection.DISK_140K);
                    yield WizardPage.SECTOR;
                }
                yield WizardPage.SECTOR;
            }
            case SIZE -> {
                if (imageSizeSelectionProperty.getValue() == ImageSizeSelection.DISK_140K) {
                    yield WizardPage.SECTOR;
                }
                yield WizardPage.SUMMARY;
            }
            case SECTOR -> WizardPage.SUMMARY;
            case SUMMARY -> null;
        };
    }

    enum WizardPage {
        FILESTORE, SIZE, SECTOR, SUMMARY
    }
    enum FileStoreSelection {
        GAMEDOS("GameDOS");

        private final String text;

        FileStoreSelection(String text) {
            this.text = text;
        }
        public String getText() {
            return text;
        }
    }
    enum ImageSizeSelection {
        DISK_140K("140KiB 5.25\" Floppy"),
        DISK_800K("800KiB 3.5\" Floppy"),
        HDD_5M("5MiB Hard Disk"),
        HDD_10M("10MiB Hard Disk"),
        HDD_20M("20MiB Hard Disk"),
        HDD_32M("32MiB Hard Disk");

        private final String text;

        ImageSizeSelection(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }
    enum SectorOrderSelection {
        DOS("DOS Ordered"),
        PRODOS("ProDOS/Pascal Ordered"),
        NIBBLE("Nibble Image");

        private final String text;

        SectorOrderSelection(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }
}
