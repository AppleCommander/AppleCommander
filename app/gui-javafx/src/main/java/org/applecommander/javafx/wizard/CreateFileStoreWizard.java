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

import com.webcodepro.applecommander.storage.DiskConstants;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import org.applecommander.device.*;
import org.applecommander.image.NibbleImage;
import org.applecommander.javafx.FXControls;
import org.applecommander.os.gamedos.GamedosFileStoreFactory;
import org.applecommander.source.DataBufferSource;
import org.applecommander.source.Source;

import java.util.Map;
import java.util.Optional;

public class CreateFileStoreWizard extends WizardDialog<CreateFileStoreWizard.WizardPage> {
    private final ObjectProperty<FileStoreSelection> fileStoreSelectionProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<ImageSizeSelection> imageSizeSelectionProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<SectorOrderSelection> sectorOrderSelectionProperty = new SimpleObjectProperty<>();

    public CreateFileStoreWizard() {
        super();
        initializeWizard("/images/DiskImageWizardLogo.png", WizardPage.FILESTORE, WizardPage.SUMMARY);
    }

    public Source getSource() {
        // Generate a starting filename:
        String fileName = String.format("New Disk.%s",
                imageSizeSelectionProperty.get().getAlternateFileExtension().orElse(
                        sectorOrderSelectionProperty.get().getFileExtension()));

        // Adjust image size if we picked nibble order, otherwise use the size selected:
        int imageSize = sectorOrderSelectionProperty.get().getAlternateSize()
                .orElse(imageSizeSelectionProperty.get().getSize());
        Source source = DataBufferSource.create(imageSize, fileName).get();

        // For now, the file store is _transitory_ and it just formats the source.
        switch (fileStoreSelectionProperty.get().getDeviceType()) {
            case DeviceType.BLOCK -> {
                // TODO
            }
            case SECTOR -> {
                TrackSectorDevice device = createTrackSectorDevice(source);
                GamedosFileStoreFactory.create(device);
            }
        }
        return source;
    }

    public TrackSectorDevice createTrackSectorDevice(Source source) {
        return switch (sectorOrderSelectionProperty.get()) {
            case DOS -> new DosOrderedTrackSectorDevice(source);
            case PRODOS -> new BlockToTrackSectorAdapter(new ProdosOrderedBlockDevice(source,
                    BlockDevice.STANDARD_BLOCK_SIZE), new ProdosBlockToTrackSectorAdapterStrategy());
            case NIBBLE -> SkewedTrackSectorDevice.physicalToDosSkew(
                    TrackSectorNibbleDevice.create(new NibbleImage(source), 16));
        };
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
                WizardPage.FILESTORE, FXControls.vertical()
                        .label("Choose an image type to create:")
                        .radioButton(FileStoreSelection.values(), FileStoreSelection::getText, fileStoreSelectionProperty)
                        .get(),
                WizardPage.SIZE, FXControls.vertical()
                        .label("Choose an image size to create:")
                        .radioButton(ImageSizeSelection.values(), ImageSizeSelection::getText, imageSizeSelectionProperty)
                        .get(),
                WizardPage.SECTOR, FXControls.vertical()
                        .label("Choose a sector ordering for this image:")
                        .radioButton(SectorOrderSelection.values(), SectorOrderSelection::getText, sectorOrderSelectionProperty)
                        .get(),
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

    /// All possible pages in the new file store wizard.
    public enum WizardPage {
        FILESTORE, SIZE, SECTOR, SUMMARY
    }

    /// Possible device types. Using an enum since there are a number of subclasses.
    /// This limits the options to just two: sector or block.
    public enum DeviceType {
        SECTOR, BLOCK
    }

    /// Filestore selection options.
    public enum FileStoreSelection {
        GAMEDOS("GameDOS", DeviceType.SECTOR);

        private final String text;
        private final DeviceType deviceType;

        FileStoreSelection(String text, DeviceType deviceType) {
            this.text = text;
            this.deviceType = deviceType;
        }

        public String getText() {
            return text;
        }
        public DeviceType getDeviceType() {
            return deviceType;
        }
    }

    /// Image size selection options. Includes the expected image size.
    /// Note that 140KB can be altered based on sector order choice.
    public enum ImageSizeSelection {
        DISK_140K("140KiB 5.25\" Floppy", DiskConstants.APPLE_140KB_DISK, null),
        DISK_800K("800KiB 3.5\" Floppy", DiskConstants.APPLE_800KB_DISK, null),
        HDD_5M("5MiB Hard Disk", DiskConstants.APPLE_5MB_HARDDISK, "hdv"),
        HDD_10M("10MiB Hard Disk", DiskConstants.APPLE_10MB_HARDDISK, "hdv"),
        HDD_20M("20MiB Hard Disk", DiskConstants.APPLE_20MB_HARDDISK, "hdv"),
        HDD_32M("32MiB Hard Disk", DiskConstants.APPLE_32MB_HARDDISK, "hdv");

        private final String text;
        private final int size;
        private final String alternateFileExtension;

        ImageSizeSelection(String text, int size, String alternateFileExtension) {
            this.text = text;
            this.size = size;
            this.alternateFileExtension = alternateFileExtension;
        }

        public String getText() {
            return text;
        }
        public int getSize() {
            return size;
        }
        public Optional<String> getAlternateFileExtension() {
            return Optional.ofNullable(alternateFileExtension);
        }
    }

    /// Sector ordering selection. Note that this enum allows an override on the size of the disk;
    /// specifically for nibble based images.
    public enum SectorOrderSelection {
        DOS("DOS Ordered", -1, "do"),
        PRODOS("ProDOS/Pascal Ordered", -1, "po"),
        NIBBLE("Nibble Image", DiskConstants.APPLE_140KB_NIBBLE_DISK, "nib");

        private final String text;
        private final int alternateSize;
        private final String fileExtension;

        SectorOrderSelection(String text, int alternateSize, String fileExtension) {
            this.text = text;
            this.alternateSize = alternateSize;
            this.fileExtension = fileExtension;
        }

        public String getText() {
            return text;
        }
        public Optional<Integer> getAlternateSize() {
            if (alternateSize != -1) {
                return Optional.of(alternateSize);
            }
            return Optional.empty();
        }
        public String getFileExtension() {
            return fileExtension;
        }
    }
}
