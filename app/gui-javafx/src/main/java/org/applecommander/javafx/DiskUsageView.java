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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.applecommander.filestore.FileStore;
import org.applecommander.usage.BlockUsage;
import org.applecommander.usage.DiskUsage;
import org.applecommander.usage.DiskUsage.UsageType;
import org.applecommander.usage.SectorUsage;

import java.util.*;

import static org.applecommander.javafx.FxUtils.applyShortcutToButton;
import static org.applecommander.javafx.FxUtils.createToggleButton;

public class DiskUsageView extends StackPane {
    private static final Map<UsageType,Color> COLORS = Map.of(
        UsageType.FREE, Color.LIGHTGREEN,
        UsageType.USED, Color.LIGHTCORAL,
        UsageType.BOOT, Color.TEAL,
        UsageType.BITMAP, Color.STEELBLUE,
        UsageType.SYSTEM, Color.PALEGOLDENROD,
        UsageType.DIRECTORY, Color.MEDIUMORCHID,
        UsageType.FILE, Color.GOLDENROD
    );


    private final ToggleButton usageGridToggle;
    private final ToggleButton usagePieChartToggle;
    private final VBox gridView;
    private final GridPane diskUsageGrid;
    private final HBox legendBox;
    private final PieChart pieChartView;

    private final ObjectProperty<Mode> viewMode = new SimpleObjectProperty<>();
    private final BooleanProperty showPieChartOnlyProperty = new SimpleBooleanProperty();

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
        ScrollPane diskUsageScroll = new ScrollPane(diskUsageGrid);
        diskUsageScroll.setFitToWidth(true);
        diskUsageScroll.setFitToHeight(true);
        gridView = new VBox(diskUsageScroll, legendBox);
        gridView.setAlignment(Pos.CENTER);
        gridView.setPadding(new Insets(5));
        gridView.setSpacing(5);

        // Chart view
        pieChartView = new PieChart();
        pieChartView.setTitle("Disk Usage");

        // Refresh on updates
        fileStoreWindow.fileStoreSelection().selectedItemProperty().addListener((_, _, newFileStore) -> {
            updateDisplay(newFileStore);
        });
        fileStoreWindow.changeCountProperty().addListener((_, _, changeCount) -> {
            if (changeCount != null && changeCount.intValue() > 0) {
                updateDisplay(fileStoreWindow.fileStoreSelection().getSelectedItem());
            }
        });

        gridView.visibleProperty().bind(viewMode.isEqualTo(Mode.GRID_VIEW));
        gridView.managedProperty().bind(gridView.visibleProperty());
        pieChartView.visibleProperty().bind(viewMode.isEqualTo(Mode.CHART_VIEW));
        pieChartView.managedProperty().bind(pieChartView.visibleProperty());

        usageGridToggle.visibleProperty().bind(fileStoreWindow.viewModeProperty().isEqualTo(ViewMode.USAGE).and(showPieChartOnlyProperty.not()));
        usageGridToggle.managedProperty().bind(usageGridToggle.visibleProperty());
        usagePieChartToggle.visibleProperty().bind(fileStoreWindow.viewModeProperty().isEqualTo(ViewMode.USAGE).and(showPieChartOnlyProperty.not()));
        usagePieChartToggle.managedProperty().bind(usagePieChartToggle.visibleProperty());
        viewMode.addListener((observable, oldValue, newValue) -> {
            usageGridToggle.setSelected(newValue == Mode.GRID_VIEW);
            usagePieChartToggle.setSelected(newValue == Mode.CHART_VIEW);
        });

        getChildren().addAll(gridView, pieChartView);
        visibleProperty().bind(fileStoreWindow.viewModeProperty().isEqualTo(ViewMode.USAGE));
        managedProperty().bind(visibleProperty());
        viewMode.set(Mode.GRID_VIEW);
    }

    public void updateDisplay(FileStore fileStore) {
        if (fileStore == null) return;
        Optional<DiskUsage> opt = fileStore.get(DiskUsage.class);
        if (opt.isEmpty()) {
            return;
        }
        DiskUsage usage = opt.get();
        if (usage instanceof SectorUsage sectorUsage) {
            showPieChartOnlyProperty.set(false);
            updateSectorUsageGrid(sectorUsage);
            viewMode.set(Mode.GRID_VIEW);
        }
        else if (usage instanceof BlockUsage blockUsage) {
            boolean showPieChartOnly = switch (blockUsage.getTotal()) {
                // CP/M, ProDOS/Pascal, RDOS 13 sector, RDOS 16 sector
                case 140,280,455,560 -> false;
                // Everything else
                default -> true;
            };
            showPieChartOnlyProperty.set(showPieChartOnly);
            if (showPieChartOnly) {
                viewMode.set(Mode.CHART_VIEW);
            }
            else {
                updateBlockUsageGrid(blockUsage);
                viewMode.set(Mode.GRID_VIEW);
            }
        }
        updateChartView(usage);
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
        Map<UsageType,Integer> counts = usage.getUsageCounts();
        // Need to pull these out in order to make the colors line up!
        List<PieChart.Data> data = new ArrayList<>();
        for (UsageType usageType : UsageType.values()) {
            int value = counts.getOrDefault(usageType, 0);
            if (usageType == UsageType.FREE || value > 0) {
                data.add(new PieChart.Data(usageType.name(), value));
            }
        }
        pieChartView.setData(FXCollections.observableArrayList(data));
    }

    private void updateSectorUsageGrid(SectorUsage usage) {
        Objects.requireNonNull(usage);

        // Summing this independently since we have to loop through all sectors/blocks anyway.
        final Map<UsageType,Integer> counts = new HashMap<>();

        // Iterate DiskUsage based on type
        diskUsageGrid.setHgap(5);
        diskUsageGrid.setVgap(5);
        diskUsageGrid.setPadding(new Insets(10));
        diskUsageGrid.alignmentProperty().setValue(Pos.CENTER);
        diskUsageGrid.getChildren().clear();
        final int size = 26;
        diskUsageGrid.add(new Label(), 0, 0);
        for (int t = 0; t<usage.getTracksOnDisk(); t++) {
            if ( t % 5 == 0 || t == usage.getTracksOnDisk()-1) {
                Label label = new Label(String.format("T%02d", t));
                label.setMinSize(size, size);
                diskUsageGrid.add(label, t+1, 0);
                GridPane.setHalignment(label, HPos.CENTER);
            }
            else {
                diskUsageGrid.add(new Label(), t+1, 0);
            }
        }
        for (int s = 0; s<usage.getSectorsPerTrack(); s++) {
            if ( s % 5 == 0 || s == usage.getSectorsPerTrack()-1 ) {
                Label label = new Label(String.format("S%02d", s));
                label.setMinSize(size, size);
                diskUsageGrid.add(label, 0, s+1);
                GridPane.setHalignment(label, HPos.RIGHT);
            }
            else {
                diskUsageGrid.add(new Label(), 0, s+1);
            }
        }
        for (int t = 0; t<usage.getTracksOnDisk(); t++) {
            for (int s = 0; s< usage.getSectorsPerTrack(); s++) {
                UsageType usageType = usage.getUsage(t,s);
                counts.compute(usageType, (k, v) -> v == null ? 1 : v + 1);
                diskUsageGrid.add(makeSwatch(COLORS.get(usageType), size, size), t+1, s+1);
            }
        }

        // Legend - note that we work the colors in the same order as the pie chart for consistency!
        legendBox.getChildren().clear();
        for (UsageType usageType : UsageType.values()) {
            if (usageType == UsageType.FREE || counts.getOrDefault(usageType, 0) > 0) {
                legendBox.getChildren().addAll(makeSwatch(COLORS.get(usageType), size, size), new Label(usageType.name()));
                if (usageType == UsageType.FREE) {
                    legendBox.getChildren().add(new Separator(Orientation.VERTICAL));
                }
            }
        }
        legendBox.setAlignment(Pos.CENTER);
    }
    private void updateBlockUsageGrid(BlockUsage usage) {
        Objects.requireNonNull(usage);

        // Summing this independently since we have to loop through all sectors/blocks anyway.
        final Map<UsageType,Integer> counts = new HashMap<>();

        // Iterate DiskUsage based on type
        diskUsageGrid.setHgap(5);
        diskUsageGrid.setVgap(5);
        diskUsageGrid.setPadding(new Insets(10));
        diskUsageGrid.alignmentProperty().setValue(Pos.CENTER);
        diskUsageGrid.getChildren().clear();
        final int size = 24;
        final int sectorsPerSwatch = switch (usage.getTotal()) {
            // RDOS, 13 sector
            case 455 -> 1;
            // RDOS; 16 sector
            case 560 -> 1;
            // ProDOS or Pascal; 16 sector
            case 280 -> 2;
            // CP/M; 16 sector
            case 140 -> 4;
            // Unknown
            default -> 1;
        };
        final int sectorsPerTrack = usage.getTotal() == 455 ? 13 : 16;
        final int tracks = 35;  // total assumption based on selection

        Label label = new Label();
        diskUsageGrid.add(label, 0, 0);

        label = new Label("--- BLOCKS ---");
        diskUsageGrid.add(label, 1, 0);
        GridPane.setColumnSpan(label, tracks + 1);
        GridPane.setHalignment(label, HPos.CENTER);

        label = new Label("--- BLOCKS ---");
        label.setRotate(270.0);
        Group group = new Group(label);
        diskUsageGrid.add(group, 0, 1);
        GridPane.setRowSpan(group, sectorsPerTrack + 1);
        GridPane.setValignment(group, VPos.CENTER);

        int row = 0;
        int col = 0;
        for (int b=0; b<usage.getTotal(); b++) {
            UsageType usageType = usage.getUsage(b);
            counts.compute(usageType, (k, v) -> v == null ? 1 : v + 1);
            Rectangle swatch = makeSwatch(COLORS.get(usageType), size, size*sectorsPerSwatch);
            diskUsageGrid.add(swatch, col+1, row+1);
            GridPane.setRowSpan(swatch, sectorsPerSwatch);
            row+= sectorsPerSwatch;
            if (row >= sectorsPerTrack) {
                row = 0;
                col++;
            }
        }

        // Legend - note that we work the colors in the same order as the pie chart for consistency!
        legendBox.getChildren().clear();
        for (UsageType usageType : UsageType.values()) {
            if (usageType == UsageType.FREE || counts.getOrDefault(usageType, 0) > 0) {
                legendBox.getChildren().addAll(makeSwatch(COLORS.get(usageType), size,
                    size), new Label(usageType.name()));
                if (usageType == UsageType.FREE) {
                    legendBox.getChildren().add(new Separator(Orientation.VERTICAL));
                }
            }
        }
        legendBox.setAlignment(Pos.CENTER);
    }
    private Rectangle makeSwatch(Color background, int width, int height) {
        Rectangle swatch = new Rectangle(width, height);
        swatch.fillProperty().set(background);
        swatch.strokeProperty().set(Color.BLACK);
        return swatch;
    }

    public enum Mode {
        CHART_VIEW,
        GRID_VIEW
    }
}
