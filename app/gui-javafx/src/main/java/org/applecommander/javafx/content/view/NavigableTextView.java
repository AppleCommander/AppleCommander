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
package org.applecommander.javafx.content.view;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.applecommander.javafx.content.FileContentView;
import org.applecommander.javafx.content.FileContentWindow;
import org.applecommander.util.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class NavigableTextView extends StackPane implements FileContentView {
    protected final FileContentWindow parent;
    private final TextFlow textFlow;
    private final Path highlight;
    private final Color linkColor;
    private final List<Range> clickableRanges = new ArrayList<>();
    private final List<Range> highlightedRanges = new ArrayList<>();
    private Font font;

    public NavigableTextView(FileContentWindow parent) {
        this.parent = parent;
        setFont(parent.fontSizeProperty().getValue());
        this.parent.fontSizeProperty().addListener((_, _, newValue) -> {
            setFont(newValue.doubleValue());
        });
        this.textFlow = new TextFlow();
        this.textFlow.relocate(0, 0);
        this.highlight = new Path();
        this.highlight.relocate(0, 0);
        this.highlight.setManaged(false);
        this.highlight.setFill(Color.rgb(80, 150, 255, 0.35));
        this.highlight.setStroke(null);
        this.highlight.setMouseTransparent(true);
        this.textFlow.setOnMouseMoved(event -> {
            Point2D point = textFlow.sceneToLocal(event.getSceneX(), event.getSceneY());
            int index = textFlow.getHitInfo(point).getCharIndex();

            Optional<Range> found = clickableRanges.stream().filter(r -> r.in(index)).findFirst();
            found.ifPresentOrElse(range -> {
                // end is exclusive so we need to add one
                PathElement[] geometry = textFlow.getRangeShape(range.first(), range.last()+1, true);
                highlight.getElements().setAll(geometry);
                textFlow.setCursor(Cursor.HAND);
            }, () -> {
                textFlow.setCursor(Cursor.DEFAULT);
                highlight.getElements().clear();
            });
        });
        this.linkColor = Color.CYAN.deriveColor(1.0, 1.0, 1.0, 0.35);
        refresh();
    }

    public void setFont(double size) {
        this.font = Font.font("monospaced", size);
        if (this.textFlow != null) {
            for (Node node : this.textFlow.getChildren()) {
                if (node instanceof Text text) {
                    text.setFont(font);
                }
            }
            Parent root = this.textFlow.getScene().getRoot();
            root.applyCss();
            root.layout();
            recalculateHighlightedRanges();
        }
    }

    @Override
    public Node getViewNode() {
        return this;
    }

    @Override
    public Optional<Node> getAdditionalToolbarButtons() {
        return FileContentView.super.getAdditionalToolbarButtons();
    }

    @Override
    public void copy() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();

        StringBuilder sb = new StringBuilder();
        for (var node : getChildren()) {
            if (node instanceof Text text) {
                sb.append(text.getText());
            }
        }
        content.putString(sb.toString());

        clipboard.setContent(content);
    }

    public abstract void build(TextBuilder builder);

    public final void refresh() {
        TextBuilder builder = new TextBuilder();
        build(builder);
        Text text = new Text(builder.getText());
        text.setFont(font);
        textFlow.getChildren().setAll(text);
        clickableRanges.clear();
        clickableRanges.addAll(builder.getClickableRanges());
        highlightedRanges.clear();
        highlightedRanges.addAll(builder.getHighlightedRanges());
        recalculateHighlightedRanges();
    }

    /// Update the highlighted link ranges.
    public void recalculateHighlightedRanges() {
        if (this.highlightedRanges.isEmpty()) {
            return;
        }
        List<Node> nodes = new ArrayList<>();
        nodes.add(this.textFlow);
        this.highlightedRanges.forEach(link -> {
            PathElement[] geometry = textFlow.getRangeShape(link.first(), link.last()+1, true);
            Path path = new Path();
            path.relocate(0, 0);
            path.setManaged(false);
            path.setFill(linkColor);
            path.setStroke(null);
            path.setMouseTransparent(true);
            path.getElements().setAll(geometry);
            nodes.add(path);
        });
        nodes.add(this.highlight);
        getChildren().setAll(nodes);
    }

    public static class TextBuilder {
        private final StringBuilder sb = new StringBuilder();
        private final List<Range> clickableRanges = new ArrayList<>();
        private final List<Range> highlightedRanges = new ArrayList<>();

        public String getText() {
            return sb.toString();
        }
        public List<Range> getClickableRanges() {
            return clickableRanges;
        }
        public List<Range> getHighlightedRanges() {
            return highlightedRanges;
        }

        public int length() {
            return sb.length();
        }
        public void addText(String fmt, Object... args) {
            sb.append(String.format(fmt, args));
        }
        public void addLink(String fmt, boolean highlighted, Object... args) {
            int firstChar = sb.length();
            addText(fmt, args);
            int lastChar = sb.length();
            // Range assumes it is _inclusive_.
            Range range = new Range(firstChar, lastChar-1);
            clickableRanges.add(range);
            if (highlighted) {
                highlightedRanges.add(range);
            }
        }
    }
}
