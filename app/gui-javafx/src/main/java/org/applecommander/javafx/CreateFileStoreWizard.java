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
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import org.applecommander.device.DosOrderedTrackSectorDevice;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.os.gamedos.GamedosFileStoreFactory;
import org.applecommander.source.DataBufferSource;
import org.applecommander.source.Source;

import java.util.Map;
import static org.applecommander.javafx.FxUtils.createGenericSelectionPage;

public class CreateFileStoreWizard extends WizardDialog<CreateFileStoreWizard.WizardPage> {
    private final ObjectProperty<FileStoreSelection> fileStoreSelectionProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<ImageSizeSelection> imageSizeSelectionProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<SectorOrderSelection> sectorOrderSelectionProperty = new SimpleObjectProperty<>();

    public CreateFileStoreWizard() {
        super();
        initializeWizard("/images/DiskImageWizardLogo.png", WizardPage.FILESTORE, WizardPage.SUMMARY);
    }

    public Source getSource() {
        Source source = DataBufferSource.create(DiskConstants.APPLE_140KB_DISK, "BLANK.DISK").get();
        TrackSectorDevice device = new DosOrderedTrackSectorDevice(source);
        GamedosFileStoreFactory.create(device);
        return source;
    }

    public BooleanBinding createNextPageBinding() {
        return currentPageProperty().isEqualTo(WizardPage.FILESTORE).and(fileStoreSelectionProperty.isNull())
                .or(currentPageProperty().isEqualTo(WizardPage.SIZE).and(imageSizeSelectionProperty.isNull()))
                .or(currentPageProperty().isEqualTo(WizardPage.SECTOR).and(sectorOrderSelectionProperty.isNull()))
                .or(currentPageProperty().isEqualTo(WizardPage.SUMMARY));
    }

    public BooleanBinding completedWizardBinding() {
        return fileStoreSelectionProperty.isNotNull()
                .and(imageSizeSelectionProperty.isNotNull())
                .and(sectorOrderSelectionProperty.isNotNull());
    }

    @Override
    public Map<WizardPage, Node> createWizardPages() {
        return Map.of(
                WizardPage.FILESTORE, createGenericSelectionPage("Choose an image type to create:",
                        FileStoreSelection.values(), FileStoreSelection::getText, fileStoreSelectionProperty),
                WizardPage.SIZE, createGenericSelectionPage("Choose an image size to create:",
                        ImageSizeSelection.values(), ImageSizeSelection::getText, imageSizeSelectionProperty),
                WizardPage.SECTOR, createGenericSelectionPage("Choose a sector ordering for this image:",
                        SectorOrderSelection.values(), SectorOrderSelection::getText, sectorOrderSelectionProperty),
                WizardPage.SUMMARY, createSummaryPage()
        );
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

    public WizardPage nextPage() {
        return switch (currentPageProperty().get()) {
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

    public enum WizardPage {
        FILESTORE, SIZE, SECTOR, SUMMARY
    }
    public enum FileStoreSelection {
        GAMEDOS("GameDOS");

        private final String text;

        FileStoreSelection(String text) {
            this.text = text;
        }
        public String getText() {
            return text;
        }
    }
    public enum ImageSizeSelection {
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
    public enum SectorOrderSelection {
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
