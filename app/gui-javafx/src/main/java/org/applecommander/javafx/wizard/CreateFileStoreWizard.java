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
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.applecommander.filestore.*;
import org.applecommander.javafx.FXControls;
import org.applecommander.javafx.filter.CharacterRestrictionFilter;
import org.applecommander.javafx.filter.CompositeFilter;
import org.applecommander.javafx.filter.FixedLengthFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public class CreateFileStoreWizard extends WizardDialog<CreateFileStoreWizard.WizardPage> {
    private final ObjectProperty<FileStoreFactory> fileStoreFactoryProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<FileStoreOrganization> fileStoreOrganizationProperty = new SimpleObjectProperty<>();
    private Map<String,ConfigurationOption> configurationOptions = Collections.emptyMap();

    public CreateFileStoreWizard() {
        super();
        initializeWizard("DiskImageWizardLogo.png", WizardPage.FILESTORE, WizardPage.SUMMARY);
    }

    public List<FileStore> getFileStores() {
        FileStoreFactory factory = fileStoreFactoryProperty.get();
        FileStoreOrganization organization = fileStoreOrganizationProperty.get();
        return List.of(factory.create(organization, configurationOptions));
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
    public Map<WizardPage,Pane> createWizardPages() {
        fileStoreFactoryProperty.addListener(this::fileStoreFactoryListener);

        final FileStoreFactory[] factories = FileStores.allFactories().stream()
                .filter(f -> !f.getFileStoreOrganizations().isEmpty())
                .toArray(FileStoreFactory[]::new);
        return Map.of(
                WizardPage.FILESTORE, FXControls.vertical()
                        .label("Choose an image type to create:")
                        .radioButton(factories, FileStoreFactory::getName, fileStoreFactoryProperty)
                        .get(),
                WizardPage.OPTIONS, new VBox(),
                WizardPage.ORGANIZATION, FXControls.vertical()
                        .label("Choose the image organization:")
                        .radioButton(FileStoreOrganization.values(), FileStoreOrganization::getDescription,
                                fileStoreOrganizationProperty, FileStoreOrganization.ARCHIVE)
                        .get(),
                WizardPage.SUMMARY, new GridPane()
        );
    }

    public void setupSummaryPage() {
        GridPane summaryPage = (GridPane) getWizardPage(WizardPage.SUMMARY);
        summaryPage.getChildren().clear();
        summaryPage.setHgap(10);
        summaryPage.setVgap(10);
        summaryPage.setPadding(new Insets(10));
        Label summaryLabel = new Label("Summary:");
        summaryPage.add(summaryLabel, 0, 0);
        GridPane.setColumnSpan(summaryLabel, 2);

        Label fileStoreSelectionLabel = new Label(fileStoreFactoryProperty.get().getName());
        summaryPage.addRow(1, new Label("File Store:"), fileStoreSelectionLabel);

        Label imageSizeSelectionLabel = new Label(fileStoreOrganizationProperty.get().getDescription());
        summaryPage.addRow(2, new Label("Organization:"), imageSizeSelectionLabel);

        int row = 3;
        for (ConfigurationOption option : configurationOptions.values()) {
            Label prompt = new Label(option.getPrompt() + ":");
            GridPane.setValignment(prompt, VPos.TOP);
            String text = switch (option) {
                case ConfigurationOption.StringConfigurationOption stringOption -> stringOption.getValue();
                case ConfigurationOption.FlagConfigurationOption flagOption -> flagOption.getValue() ? "Yes" : "No";
                default -> "Unknown";
            };
            Label value = new Label(text);
            summaryPage.addRow(row++, prompt, value);
        }
    }

    /// Auto select if there is only one option; also enables finish page early.
    private void fileStoreFactoryListener(ObservableValue<? extends FileStoreFactory> observable, FileStoreFactory oldValue, FileStoreFactory newValue) {
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
    }

    public WizardPage nextPage() {
        // Simple path
        WizardPage nextPage = switch (currentPageProperty().get()) {
            case FILESTORE -> WizardPage.OPTIONS;
            case OPTIONS -> WizardPage.ORGANIZATION;
            case ORGANIZATION -> WizardPage.SUMMARY;
            case SUMMARY -> null;
        };
        // Now check reality!
        FileStoreFactory factory = fileStoreFactoryProperty.get();
        if (nextPage == WizardPage.OPTIONS) {
            if (factory.getConfigurationOptions().isEmpty()) {
                nextPage = WizardPage.ORGANIZATION;
            }
            else {
                setupCustomOptionsPage();
            }
        }
        if (nextPage == WizardPage.ORGANIZATION) {
            if (factory.getFileStoreOrganizations().size() == 1) {
                nextPage = WizardPage.SUMMARY;
            }
            else {
                setupOrganizationPage();
            }
        }
        if (nextPage == WizardPage.SUMMARY) {
            setupSummaryPage();
        }
        return nextPage;
    }

    /// Dynamically set up the (custom) options page. Note that we "specially" put a VBox place-holder
    /// into the Wizard itself. We just reuse that VBox, but repopulate it each traversal.
    public void setupCustomOptionsPage() {
        FileStoreFactory factory = fileStoreFactoryProperty.get();
        VBox vbox = (VBox) getWizardPage(WizardPage.OPTIONS);
        vbox.getChildren().clear();
        vbox.setSpacing(10);
        vbox.setPadding(new Insets(10));
        // Note that we must be careful and use the same copy since the configuration options contain the values as well.
        configurationOptions = factory.getConfigurationOptions();
        for (ConfigurationOption option : configurationOptions.values()) {
            switch (option) {
                case ConfigurationOption.StringConfigurationOption stringOption -> {
                    Label label = new Label();
                    label.setText(option.getPrompt() + ":");
                    TextInputControl field = null;
                    if (stringOption.getMaxLength() > 50) {
                        field = new TextArea(stringOption.getValue());
                    } else {
                        field = new TextField(stringOption.getValue());
                    }
                    List<UnaryOperator<TextFormatter.Change>> filters = new ArrayList<>();
                    if (stringOption.getMaxLength() > 0) {
                        filters.add(new FixedLengthFilter(stringOption.getMaxLength()));
                    }
                    if (stringOption.getCharacterRestrictions() != null) {
                        filters.add(new CharacterRestrictionFilter(stringOption.getCharacterRestrictions()));
                    }
                    field.setTextFormatter(new TextFormatter<>(new CompositeFilter(filters)));
                    final TextInputControl textInputControl = field;
                    field.textProperty().addListener((_, _, newText) -> {
                        if (!stringOption.getValidatorFn().test(newText)) {
                            textInputControl.setStyle("-fx-border-color: red;");
                        }
                        else {
                            textInputControl.setStyle("");
                            stringOption.setValue(newText);
                        }
                    });
                    if (option.getNotes() != null && !option.getNotes().isEmpty()) {
                        field.setTooltip(new Tooltip(option.getNotes()));
                    }
                    vbox.getChildren().addAll(label, field);
                }
                case ConfigurationOption.FlagConfigurationOption flagOption -> {
                    CheckBox field = new CheckBox(flagOption.getPrompt());
                    field.setSelected(flagOption.getValue());
                    field.selectedProperty().addListener((_, _, newValue) -> {
                        flagOption.setValue(newValue);
                    });
                    if (option.getNotes() != null && !option.getNotes().isEmpty()) {
                        field.setTooltip(new Tooltip(option.getNotes()));
                    }
                    vbox.getChildren().add(field);
                }
                default -> { /* Ignore for now */ }
            }
        }
    }

    /// Configure the organization page to only show those that apply.
    public void setupOrganizationPage() {
        FileStoreFactory factory = fileStoreFactoryProperty.get();
        Pane pane = getWizardPage(WizardPage.ORGANIZATION);
        for (Node node : pane.getChildren()) {
            if (node instanceof RadioButton radio) {
                if (radio.getUserData() instanceof FileStoreOrganization organization) {
                    boolean found = factory.getFileStoreOrganizations().contains(organization);
                    radio.setVisible(found);
                    radio.setManaged(found);
                }
            }
        }
    }

    /// All possible pages in the new file store wizard.
    public enum WizardPage {
        FILESTORE, OPTIONS, ORGANIZATION, SUMMARY
    }
}
