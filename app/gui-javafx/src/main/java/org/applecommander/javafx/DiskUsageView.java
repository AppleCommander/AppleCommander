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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.applecommander.usage.BlockUsage;
import org.applecommander.usage.DiskUsage;
import org.applecommander.usage.SectorUsage;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import static org.applecommander.javafx.FxUtils.applyShortcutToButton;
import static org.applecommander.javafx.FxUtils.createToggleButton;

public class DiskUsageView extends StackPane {
    private final ToggleButton usageGridToggle;
    private final ToggleButton usagePieChartToggle;
    private final GridPane diskUsageGrid;
    private final HBox legendBox;
    private final PieChart pieChart;

    private final ObjectProperty<Mode> viewMode = new SimpleObjectProperty<>(Mode.GRID_VIEW);

    public DiskUsageView(FileStoreWindow fileStoreWindow, ToolBar toolBar) {
        ToggleGroup listingModeGroup = new ToggleGroup();
        usageGridToggle = createToggleButton("usage-grid-view.png", "Native", listingModeGroup, e -> selectGridView());
        usagePieChartToggle = createToggleButton("usage-chart-view.png", "Detail", listingModeGroup, e -> selectChartView());
        HBox listingModeBox = new HBox(usageGridToggle, usagePieChartToggle);
        toolBar.getItems().add(listingModeBox);

        // Grid view
        diskUsageGrid = new GridPane();
        legendBox = new HBox();
        legendBox.setSpacing(12);
        legendBox.setPadding(new Insets(5));
        legendBox.setAlignment(Pos.CENTER);
        ScrollPane scrollPane = new ScrollPane(diskUsageGrid);
        scrollPane.setFitToWidth(true);
        BorderPane gridView = new BorderPane();
        gridView.setCenter(scrollPane);
        gridView.setBottom(legendBox);

        // Chart view
        pieChart = new PieChart();
        pieChart.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/pie-chart-custom-colors.css")).toExternalForm());
        pieChart.setTitle("Disk Usage");
        fileStoreWindow.fileStoreSelection().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Optional<DiskUsage> opt = newValue.get(DiskUsage.class);
                if (opt.isEmpty()) {
                    // nothing to view
                    return;
                }
                DiskUsage usage = opt.get();
                updateGridView(usage);
                updateChartView(usage);
            }
        });

        gridView.visibleProperty().bind(viewMode.isEqualTo(Mode.GRID_VIEW));
        gridView.managedProperty().bind(gridView.visibleProperty());
        pieChart.visibleProperty().bind(viewMode.isEqualTo(Mode.CHART_VIEW));
        pieChart.managedProperty().bind(pieChart.visibleProperty());

        usageGridToggle.visibleProperty().bind(fileStoreWindow.viewModeProperty().isEqualTo(ViewMode.USAGE));
        usageGridToggle.managedProperty().bind(usageGridToggle.visibleProperty());
        usagePieChartToggle.visibleProperty().bind(fileStoreWindow.viewModeProperty().isEqualTo(ViewMode.USAGE));
        usagePieChartToggle.managedProperty().bind(usagePieChartToggle.visibleProperty());
        viewMode.addListener((observable, oldValue, newValue) -> {
            usageGridToggle.setSelected(newValue == Mode.GRID_VIEW);
            usagePieChartToggle.setSelected(newValue == Mode.CHART_VIEW);
        });

        getChildren().addAll(gridView, pieChart);
        visibleProperty().bind(fileStoreWindow.viewModeProperty().isEqualTo(ViewMode.USAGE));
        managedProperty().bind(visibleProperty());
    }

    public void bindScene(Scene scene) {
        // Function keys for view modes
        applyShortcutToButton(scene, usageGridToggle, "Grid View",
                new KeyCodeCombination(KeyCode.F2), this::selectGridView);
        applyShortcutToButton(scene, usagePieChartToggle, "Detail View",
                new KeyCodeCombination(KeyCode.F3), this::selectChartView);
    }

    public void selectGridView() {
        viewMode.set(Mode.GRID_VIEW);
    }
    public void selectChartView() {
        viewMode.set(Mode.CHART_VIEW);
    }

    private void updateChartView(DiskUsage usage) {
        Objects.requireNonNull(usage);
        var free = new PieChart.Data("Free", usage.getFree());
        var used = new PieChart.Data("Used", usage.getUsed());
        pieChart.setData(FXCollections.observableArrayList(free, used));
    }

    private void updateGridView(DiskUsage usage) {
        Objects.requireNonNull(usage);

        final Color freeColor = Color.LIGHTGREEN;
        final Color usedColor = Color.LIGHTCORAL;
        final BiFunction<Color, Integer, Rectangle> makeSwatch = (background, size) -> {
            Rectangle swatch = new Rectangle(size, size);
            swatch.fillProperty().set(background);
            swatch.strokeProperty().set(Color.BLACK);
            return swatch;
        };

        // Iterate DiskUsage based on type
        diskUsageGrid.setHgap(5);
        diskUsageGrid.setVgap(5);
        diskUsageGrid.setPadding(new Insets(10));
        diskUsageGrid.alignmentProperty().setValue(Pos.CENTER);
        diskUsageGrid.getChildren().clear();
        if (usage instanceof BlockUsage blockUsage) {
            int size = 24;
            if (blockUsage.getTotal() > 8000) {
                size = 10;
            }
            else if (blockUsage.getTotal() > 1600) {
                size = 15;
            }
            else if (blockUsage.getTotal() > 300) {
                size = 20;
            }
            int row = 0;
            int col = 0;
            for (int b=0; b<blockUsage.getTotal(); b++) {
                diskUsageGrid.add(blockUsage.isUsed(b) ? makeSwatch.apply(usedColor, size)
                                                         : makeSwatch.apply(freeColor, size), col, row);
                col++;
                if (col > 32) { // Arbitrary
                    col = 0;
                    row++;
                }
            }
        }
        else if (usage instanceof SectorUsage sectorUsage) {
            diskUsageGrid.add(new Label(), 0, 0);
            for (int t=0; t<sectorUsage.getTotalTracks(); t++) {
                if ( t % 5 == 0 || t == sectorUsage.getTotalTracks()-1) {
                    Label label = new Label(String.format("T%02d", t));
                    diskUsageGrid.add(label, t+1, 0);
                    GridPane.setHalignment(label, HPos.CENTER);
                }
                else {
                    diskUsageGrid.add(new Label(), t+1, 0);
                }
            }
            for (int s=0; s<sectorUsage.getTotalSectors(); s++) {
                if ( s % 5 == 0 || s == sectorUsage.getTotalSectors()-1 ) {
                    Label label = new Label(String.format("S%02d", s));
                    diskUsageGrid.add(label, 0, s+1);
                    GridPane.setHalignment(label, HPos.RIGHT);
                }
                else {
                    diskUsageGrid.add(new Label(), 0, s+1);
                }
            }
            for (int t=0; t<sectorUsage.getTotalTracks(); t++) {
                for (int s=0; s< sectorUsage.getTotalSectors(); s++) {
                    diskUsageGrid.add(sectorUsage.isUsed(t,s) ? makeSwatch.apply(usedColor, 24)
                                                                : makeSwatch.apply(freeColor, 24), t+1, s+1);
                }
            }
        }

        // Legend
        legendBox.getChildren().clear();

        HBox freeLegend = new HBox(6);
        Label freeLabel = new Label("Free");
        freeLegend.getChildren().addAll(makeSwatch.apply(freeColor, 24), freeLabel);

        HBox usedLegend = new HBox(6);
        Label usedLabel = new Label("Used");
        usedLegend.getChildren().addAll(makeSwatch.apply(usedColor, 24), usedLabel);

        legendBox.getChildren().addAll(freeLegend, usedLegend);
    }

    public enum Mode {
        CHART_VIEW,
        GRID_VIEW
    }
}
