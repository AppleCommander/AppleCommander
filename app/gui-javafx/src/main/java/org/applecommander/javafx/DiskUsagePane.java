package org.applecommander.javafx;

import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.applecommander.filestore.FileStore;
import org.applecommander.usage.BlockUsage;
import org.applecommander.usage.DiskUsage;
import org.applecommander.usage.SectorUsage;

import java.util.Optional;

public class DiskUsagePane extends BorderPane {
    private final FileStoreViewer fileStoreViewer;
    private final CanvasPane diskUsageCanvas;
    private final HBox legendBox;

    public DiskUsagePane(FileStoreViewer fileStoreViewer) {
        this.fileStoreViewer = fileStoreViewer;
        diskUsageCanvas = new CanvasPane();
        legendBox = new HBox();
        legendBox.setSpacing(12);
        legendBox.setAlignment(Pos.CENTER);

        diskUsageCanvas.setRepaint(this::renderDiskUsage);

        setCenter(diskUsageCanvas);
        setBottom(legendBox);

        visibleProperty().bind(fileStoreViewer.viewModeProperty().isEqualTo(ViewMode.USAGE));
        managedProperty().bind(visibleProperty());
    }

    private void renderDiskUsage(Canvas canvas) {
        if (fileStoreViewer.fileStoreSelection().isEmpty()) {
            return;
        }
        FileStore fileStore = fileStoreViewer.fileStoreSelection().getSelectedItem();
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
}
