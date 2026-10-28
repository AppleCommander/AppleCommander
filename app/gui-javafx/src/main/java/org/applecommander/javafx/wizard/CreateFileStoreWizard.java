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
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import org.applecommander.filestore.FileStore;
import org.applecommander.filestore.FileStoreFactory;
import org.applecommander.filestore.FileStoreOrganization;
import org.applecommander.filestore.FileStores;
import org.applecommander.javafx.FXControls;

import java.util.List;
import java.util.Map;

public class CreateFileStoreWizard extends WizardDialog<CreateFileStoreWizard.WizardPage> {
    private final ObjectProperty<FileStoreFactory> fileStoreFactoryProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<FileStoreOrganization> fileStoreOrganizationProperty = new SimpleObjectProperty<>();

    public CreateFileStoreWizard() {
        super();
        initializeWizard("DiskImageWizardLogo.png", WizardPage.FILESTORE, WizardPage.SUMMARY);
    }

    public List<FileStore> getFileStores() {
        FileStoreFactory factory = fileStoreFactoryProperty.get();
        FileStoreOrganization organization = fileStoreOrganizationProperty.get();
        return List.of(factory.create(organization));
    }

    public BooleanBinding createNextPageBinding() {
        return currentPageProperty().isEqualTo(WizardPage.FILESTORE).and(fileStoreFactoryProperty.isNull())
                .or(currentPageProperty().isEqualTo(WizardPage.ORGANIZATION).and(fileStoreOrganizationProperty.isNull()))
                .or(currentPageProperty().isEqualTo(WizardPage.SUMMARY));
    }

    public BooleanBinding completedWizardBinding() {
        return fileStoreFactoryProperty.isNotNull()
                .and(fileStoreOrganizationProperty.isNotNull());
    }

    @Override
    public Map<WizardPage, Node> createWizardPages() {
        // Auto select if there is only one option; also enables finish page early.
        fileStoreFactoryProperty.addListener((_, _, newValue) -> {
            FileStoreFactory factory = fileStoreFactoryProperty.get();
            if (factory == null) {
                fileStoreOrganizationProperty.set(null);
            }
            else if (factory.getFileStoreOrganizations().size() == 1) {
                fileStoreOrganizationProperty.set(factory.getFileStoreOrganizations().getFirst());
            }
            else if (fileStoreOrganizationProperty.get() != null
                     && !factory.getFileStoreOrganizations().contains(fileStoreOrganizationProperty.get())) {
                fileStoreOrganizationProperty.set(null);
            }
        });

        final FileStoreFactory[] factories = FileStores.allFactories().stream()
                .filter(f -> !f.getFileStoreOrganizations().isEmpty())
                .toArray(FileStoreFactory[]::new);
        return Map.of(
                WizardPage.FILESTORE, FXControls.vertical()
                        .label("Choose an image type to create:")
                        .radioButton(factories, FileStoreFactory::getName, fileStoreFactoryProperty)
                        .get(),
                WizardPage.ORGANIZATION, FXControls.vertical()
                        .label("Choose the image organization:")
                        .radioButton(FileStoreOrganization.values(), FileStoreOrganization::getDescription,
                                fileStoreOrganizationProperty, FileStoreOrganization.ARCHIVE)
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
        fileStoreFactoryProperty.addListener((_, _, newValue) -> {
            if (newValue != null) {
                fileStoreSelectionLabel.setText(newValue.getName());
            }
        });
        summaryPage.addRow(1, new Label("File Store:"), fileStoreSelectionLabel);
        Label imageSizeSelectionLabel = new Label();
        fileStoreOrganizationProperty.addListener((_, _, newValue) -> {
            if (newValue != null) {
                imageSizeSelectionLabel.setText(newValue.getDescription());
            }
        });
        summaryPage.addRow(2, new Label("Organization:"), imageSizeSelectionLabel);
        return summaryPage;
    }

    public WizardPage nextPage() {
        return switch (currentPageProperty().get()) {
            case FILESTORE -> {
                FileStoreFactory factory = fileStoreFactoryProperty.get();
                if (factory.getFileStoreOrganizations().size() == 1) {
                    yield WizardPage.SUMMARY;
                }
                yield WizardPage.ORGANIZATION;
            }
            case ORGANIZATION -> WizardPage.SUMMARY;
            case SUMMARY -> null;
        };
    }

    /// All possible pages in the new file store wizard.
    public enum WizardPage {
        FILESTORE, ORGANIZATION, SUMMARY
    }
}
