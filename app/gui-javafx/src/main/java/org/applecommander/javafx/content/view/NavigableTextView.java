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
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.applecommander.javafx.FxUtils;
import org.applecommander.javafx.content.FileContentView;
import org.applecommander.javafx.content.FileContentWindow;
import org.applecommander.util.Range;

import java.util.*;

/// Provides a foundation for textual contents that have navigation components. Things such
/// as `GOTO 1000` can navigate to line 1000 or `JSR $1000` can jump to that memory location.
/// Note that in the current implementation, a `refresh()` is invoked during construction,
/// meaning all implementing classes must guard against early calls (mostly `null` items
/// that haven't been set up yet).
public abstract class NavigableTextView implements FileContentView {
    protected final FileContentWindow parent;
    private final ScrollPane scrollPane;
    private final StackPane stackPane;
    private final TextFlow textFlow;
    private final Path highlight;
    private final Color linkColor;
    private final List<Range> clickableRanges = new ArrayList<>();
    private final List<Range> highlightedRanges = new ArrayList<>();
    private final Map<Range,Integer> navigableLinks = new HashMap<>();
    private final Map<Integer,Range> navigableTargets = new HashMap<>();
    private final Map<Range,Integer> customLinks = new HashMap<>();
    private Font font;

    public NavigableTextView(FileContentWindow parent) {
        this.parent = parent;
        this.stackPane = new StackPane();
        this.scrollPane = new ScrollPane(this.stackPane);
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
        this.textFlow.setOnMouseExited(_ -> {
            textFlow.setCursor(Cursor.DEFAULT);
            highlight.getElements().clear();
        });
        this.textFlow.setOnMouseClicked(event -> {
            Point2D point = textFlow.sceneToLocal(event.getSceneX(), event.getSceneY());
            int index = textFlow.getHitInfo(point).getCharIndex();
            Optional<Range> found = navigableLinks.keySet().stream().filter(r -> r.in(index)).findFirst();
            found.ifPresent(key -> {
                final int number = navigableLinks.get(key);
                Optional<Integer> actualTarget = navigableTargets.keySet().stream().reduce((a,b) -> {
                    int diffA = Math.abs(number - a);
                    int diffB = Math.abs(number - b);
                    return diffA <= diffB ? a : b;
                });
                int target = number;
                if (actualTarget.isPresent()) {
                    target = actualTarget.get();
                }
                if (navigableTargets.containsKey(target)) {
                    Range range = navigableTargets.get(target);
                    Path path = new Path(textFlow.getRangeShape(range.first(), range.last()+1, true));
                    FxUtils.centerNodeInScrollPane(this.scrollPane, path);
                }
            });
            Optional<Range> custom = customLinks.keySet().stream().filter(r -> r.in(index)).findFirst();
            custom.ifPresent(range -> handleCustomLink(customLinks.get(range)));
        });
        this.linkColor = Color.CYAN.deriveColor(1.0, 1.0, 1.0, 0.35);
        refresh();
    }

    /// Provide a hook for custom links. These are links that are _not_ navigational. For instance,
    /// the disassembly listing marks each data byte to be a custom link when in the code/data mode.
    public void handleCustomLink(int value) {
        // do nothing in default implementation
    }

    /// Set the font size and adjust all the highlighted regions.
    public void setFont(double size) {
        this.font = Font.font("monospaced", size);
        if (this.textFlow != null) {
            for (Node node : this.textFlow.getChildren()) {
                if (node instanceof Text text) {
                    text.setFont(font);
                }
            }
            recalculateHighlightedRanges();
        }
    }

    /// Return the primary graphical `Node` representing the contents.
    @Override
    public Node getViewNode() {
        return scrollPane;
    }

    /// Perform a copy of the contents to the clipboard.
    @Override
    public void copy() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();

        StringBuilder sb = new StringBuilder();
        for (var node : textFlow.getChildren()) {
            if (node instanceof Text text) {
                sb.append(text.getText());
            }
        }
        content.putString(sb.toString());

        clipboard.setContent(content);
    }

    /// The `build(TextBuilder)` is called during the refresh cycle. This allows the implementor
    /// to process the document and tie in links, navigation targets, and text.
    public abstract void build(TextBuilder builder);

    /// Refresh the contents.
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
        navigableLinks.clear();
        navigableLinks.putAll(builder.getNavigableLinks());
        navigableTargets.clear();
        navigableTargets.putAll(builder.getNavigableTargets());
        customLinks.clear();
        customLinks.putAll(builder.getCustomLinks());
        recalculateHighlightedRanges();
    }

    /// Update the highlighted link ranges.
    public void recalculateHighlightedRanges() {
        if (this.highlightedRanges.isEmpty()) {
            return;
        }
        // Force any updates to be applied before calculating the highlights.
        if (this.textFlow.getScene() != null) {
            Parent root = this.textFlow.getScene().getRoot();
            root.applyCss();
            root.layout();
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
        this.stackPane.getChildren().setAll(nodes);
    }

    /// The `TextBuilder` is a helper class that builds all the details required
    /// for the navigable components.
    public static class TextBuilder {
        private final StringBuilder sb = new StringBuilder();
        private final List<Range> clickableRanges = new ArrayList<>();
        private final List<Range> highlightedRanges = new ArrayList<>();
        private final Map<Range,Integer> navigableLinks = new HashMap<>();
        private final Map<Integer,Range> navigableTargets = new HashMap<>();
        private final Map<Range,Integer> customLinks = new HashMap<>();

        public String getText() {
            return sb.toString();
        }
        public List<Range> getClickableRanges() {
            return clickableRanges;
        }
        public List<Range> getHighlightedRanges() {
            return highlightedRanges;
        }
        public Map<Range,Integer> getNavigableLinks() {
            return navigableLinks;
        }
        public Map<Integer,Range> getNavigableTargets() {
            return navigableTargets;
        }
        public Map<Range, Integer> getCustomLinks() {
            return customLinks;
        }

        public int length() {
            return sb.length();
        }
        /// Add text into the resulting document.
        public void addText(String fmt, Object... args) {
            if (args.length == 0) {
                sb.append(fmt);
            }
            else {
                sb.append(String.format(fmt, args));
            }
        }
        private Range addTextAsRange(String fmt, Object... args) {
            int firstChar = sb.length();
            addText(fmt, args);
            int lastChar = sb.length();
            // Range assumes it is _inclusive_.
            return new Range(firstChar, lastChar-1);
        }
        /// Add a target for navigation. This maps the target (memory address, line number, etc.)
        /// to a text range (likely the address or line number) that is used to locate placement
        /// within the scrollable document.
        public void addNavigableTarget(int target, String fmt, Object... args) {
            Range range = addTextAsRange(fmt, args);
            navigableTargets.put(target, range);
        }
        /// Add a navigable link to a navigable target. Generally, it is _assumed_ that both ends
        /// of the link will be present, but there are no guarantees. For instance, a BASIC program
        /// could have a GOTO to a non-existent number.
        public void addNavigableLink(int target, String fmt, Object... args) {
            Range range = addTextAsRange(fmt, args);
            navigableLinks.put(range, target);
            clickableRanges.add(range);
            highlightedRanges.add(range);
        }
        /// Add a custom link.
        public void addCustomLink(int target, String fmt, Object... args) {
            Range range = addTextAsRange(fmt, args);
            clickableRanges.add(range);
            customLinks.put(range, target);
        }
    }
}
