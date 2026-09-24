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
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.applecommander.filestore.FileStore;
import org.applecommander.usage.BlockUsage;
import org.applecommander.usage.DiskUsage;
import org.applecommander.usage.SectorUsage;

import java.util.Optional;

import static org.applecommander.javafx.FxUtils.applyShortcutToButton;
import static org.applecommander.javafx.FxUtils.createToggleButton;

public class DiskUsageView extends StackPane {
    private final FileStoreWindow fileStoreWindow;
    private final ToggleButton usageGridToggle;
    private final ToggleButton usagePieChartToggle;
    private final BorderPane gridView;
    private final CanvasPane diskUsageCanvas;
    private final HBox legendBox;
    private final PieChart pieChart;

    private final ObjectProperty<Mode> viewMode = new SimpleObjectProperty<>(Mode.GRID_VIEW);

    public DiskUsageView(FileStoreWindow fileStoreWindow, ToolBar toolBar) {
        this.fileStoreWindow = fileStoreWindow;

        ToggleGroup listingModeGroup = new ToggleGroup();
        usageGridToggle = createToggleButton("usage-grid-view.png", "Native", listingModeGroup, e -> selectGridView());
        usagePieChartToggle = createToggleButton("usage-chart-view.png", "Detail", listingModeGroup, e -> selectChartView());
        HBox listingModeBox = new HBox(usageGridToggle, usagePieChartToggle);
        toolBar.getItems().add(listingModeBox);

        // Grid view
        diskUsageCanvas = new CanvasPane();
        legendBox = new HBox();
        legendBox.setSpacing(12);
        legendBox.setAlignment(Pos.CENTER);
        diskUsageCanvas.setRepaint(this::renderDiskUsage);
        gridView = new BorderPane();
        gridView.setCenter(diskUsageCanvas);
        gridView.setBottom(legendBox);

        // Chart view
        pieChart = new PieChart();
        pieChart.getStylesheets().add("/css/pie-chart-custom-colors.css");
        pieChart.setTitle("Disk Usage");
        fileStoreWindow.fileStoreSelection().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Optional<DiskUsage> opt = newValue.get(DiskUsage.class);
                if (opt.isEmpty()) {
                    // nothing to render
                    return;
                }
                DiskUsage usage = opt.get();
                var free = new PieChart.Data("Free", usage.getFree());
                var used = new PieChart.Data("Used", usage.getUsed());
                pieChart.setData(FXCollections.observableArrayList(free, used));
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

    private void renderDiskUsage(Canvas canvas) {
        if (fileStoreWindow.fileStoreSelection().isEmpty()) {
            return;
        }
        FileStore fileStore = fileStoreWindow.fileStoreSelection().getSelectedItem();
        Optional<DiskUsage> opt = fileStore.get(DiskUsage.class);
        if (opt.isEmpty()) {
            // nothing to render
            return;
        }
        DiskUsage usage = opt.get();

        String topTitle = "Tracks";
        String leftTitle = "Sectors";
        if (usage instanceof BlockUsage) {
            topTitle = "Blocks";
            leftTitle = "Blocks";
        }

        int xCount = 1;
        int yCount = 1;

        if (usage instanceof SectorUsage sectorUsage) {
            xCount = sectorUsage.getTotalTracks();
            yCount = sectorUsage.getTotalSectors();
        } else  {
            // Compute a near-square grid to render individual blocks
            int cols = (int) Math.ceil(Math.sqrt(usage.getTotal()));
            int rows = (int) Math.ceil((double) usage.getTotal() / cols);
            xCount = Math.max(1, cols);
            yCount = Math.max(1, rows);
        }

        // Draw onto canvas
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0) w = 800;
        if (h <= 0) h = 480;

        double padding = 8.0;
        double gap = 2.0;

        // Determine label font sizes and measure required label areas
        Font titleFont = Font.font(14);
        Font labelFont = Font.font(12);

        // Measure top labels (column numbers) and left labels (row numbers)
        double maxTopLabelWidth = 0.0;
        double maxTopLabelHeight = 0.0;
        for (int col = 0; col < xCount; col++) {
            String label = Integer.toString(col);
            Text t = new Text(label);
            t.setFont(labelFont);
            Bounds b = t.getLayoutBounds();
            maxTopLabelWidth = Math.max(maxTopLabelWidth, b.getWidth());
            maxTopLabelHeight = Math.max(maxTopLabelHeight, b.getHeight());
        }

        double maxLeftLabelWidth = 0.0;
        double maxLeftLabelHeight = 0.0;
        for (int row = 0; row < yCount; row++) {
            String label = Integer.toString(row);
            Text t = new Text(label);
            t.setFont(labelFont);
            Bounds b = t.getLayoutBounds();
            maxLeftLabelWidth = Math.max(maxLeftLabelWidth, b.getWidth());
            maxLeftLabelHeight = Math.max(maxLeftLabelHeight, b.getHeight());
        }

        // Split top area into title area and number area to prevent overlap
        double topTitleHeight = titleFont.getSize() + 4.0;
        double topNumberHeight = maxTopLabelHeight + 6.0;
        double topLabelHeight = Math.max(24.0, topTitleHeight + topNumberHeight + 4.0);

        // Split left area into title area (for rotated title) and number area
        double leftTitleWidth = titleFont.getSize() + 6.0; // rotated title approx
        double leftNumberWidth = maxLeftLabelWidth + 8.0;
        double leftLabelWidth = Math.max(48.0, leftTitleWidth + leftNumberWidth + 6.0);

        double availableW = Math.max(10, w - padding * 2 - leftLabelWidth);
        double availableH = Math.max(10, h - padding * 2 - topLabelHeight);

        double cellW = xCount > 0 ? (availableW - (xCount - 1) * gap) / xCount : availableW;
        double cellH = yCount > 0 ? (availableH - (yCount - 1) * gap) / yCount : availableH;
        gc.clearRect(0, 0, w, h);

        // Colors
        Color freeColor = Color.web("#90EE90"); // lightgreen
        Color usedColor = Color.web("#F08080"); // lightcoral
        Color borderColor = Color.web("#000000");

        // Use measured titleFont and labelFont from earlier
        gc.setFont(titleFont);

        // Draw top title (centered in title area)
        double titleX = padding + leftLabelWidth + availableW / 2.0;
        double titleY = padding + topTitleHeight / 2.0;
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(topTitle, titleX, titleY);

        // Draw top numeric labels (in their own band below the title)
        gc.setFont(labelFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        double topNumbersY = padding + topTitleHeight + topNumberHeight / 2.0;
        for (int col = 0; col < xCount; col++) {
            // show 0, then every 5th index (0,5,10,...) and always show last index
            if ((col % 5 != 0) && col != xCount - 1) continue;
            String label;
            if (usage instanceof BlockUsage) {
                // single-dimension blocks: top label shows starting block index for column
                int labelVal = col * yCount;
                label = Integer.toString(labelVal);
            } else {
                label = Integer.toString(col);
            }
            double xCenter = padding + leftLabelWidth + col * (cellW + gap) + cellW / 2.0;
            gc.fillText(label, xCenter, topNumbersY);
        }

        // Left vertical title: place in the left title band, centered vertically against grid
        gc.save();
        double leftTitleCenterX = padding + leftTitleWidth / 2.0;
        double leftTitleCenterY = padding + topLabelHeight + availableH / 2.0;
        gc.translate(leftTitleCenterX, leftTitleCenterY);
        gc.rotate(-90);
        gc.fillText(leftTitle, 0, 0);
        gc.restore();

        // Left numeric labels: place in the left number band (to the right of the rotated title)
        double leftNumbersX = padding + leftTitleWidth + leftNumberWidth / 2.0;
        for (int row = 0; row < yCount; row++) {
            // show 0, then every 5th index (0,5,10,...) and always show last index
            if ((row % 5 != 0) && row != yCount - 1) continue;
            String label = Integer.toString(row);
            double yCenter = padding + topLabelHeight + row * (cellH + gap) + cellH / 2.0;
            gc.fillText(label, leftNumbersX, yCenter);
        }

        // Iterate DiskUsage - column-major (columns first)
        for (int col = 0; col < xCount; col++) {
            for (int row = 0; row < yCount; row++) {
                boolean isFree = false;
                boolean isUsed = false;
                if (usage instanceof BlockUsage blockUsage) {
                    int block = col * yCount + row;
                    if (block < blockUsage.getTotal()) {
                        isUsed = blockUsage.isUsed(block);
                        isFree = !isUsed;
                    }
                } else if (usage instanceof SectorUsage sectorUsage) {
                    isUsed = sectorUsage.isUsed(col, row);
                    isFree = !isUsed;
                }

                double x = padding + leftLabelWidth + col * (cellW + gap);
                double y = padding + topLabelHeight + row * (cellH + gap);

                if (isFree) {
                    gc.setFill(freeColor);
                } else if (isUsed) {
                    gc.setFill(usedColor);
                } else {
                    continue;
                }
                gc.fillRect(x, y, Math.max(1, cellW), Math.max(1, cellH));
                gc.setStroke(borderColor);
                gc.strokeRect(x, y, Math.max(1, cellW), Math.max(1, cellH));
            }
        }

        // Legend
        legendBox.getChildren().clear();

        HBox freeLegend = new HBox(6);
        Region freeSwatch = new Region();
        freeSwatch.setStyle("-fx-background-color: #90EE90; -fx-border-color: #000000; -fx-min-width: 16px; -fx-min-height: 16px;");
        Label freeLabel = new Label("Free");
        freeLegend.getChildren().addAll(freeSwatch, freeLabel);

        HBox usedLegend = new HBox(6);
        Region usedSwatch = new Region();
        usedSwatch.setStyle("-fx-background-color: #F08080; -fx-border-color: #000000; -fx-min-width: 16px; -fx-min-height: 16px;");
        Label usedLabel = new Label("Used");
        usedLegend.getChildren().addAll(usedSwatch, usedLabel);

        legendBox.getChildren().addAll(freeLegend, usedLegend);
    }

    public enum Mode {
        CHART_VIEW,
        GRID_VIEW
    }
}
