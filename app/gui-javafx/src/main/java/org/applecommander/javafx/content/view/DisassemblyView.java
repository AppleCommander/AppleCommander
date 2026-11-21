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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.applecommander.disassembler.api.Disassembler;
import org.applecommander.disassembler.api.Instruction;
import org.applecommander.disassembler.api.InstructionSet;
import org.applecommander.disassembler.api.mos6502.InstructionSet6502;
import org.applecommander.disassembler.api.pcode.InstructionSetPCode;
import org.applecommander.disassembler.api.z80.InstructionSetZ80;
import org.applecommander.javafx.content.FileContentView;
import org.applecommander.javafx.content.FileContentWindow;
import org.applecommander.transfer.ProdosAttributes;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.Range;

import java.util.*;

public class DisassemblyView extends StackPane implements FileContentView {
    private final FileContentWindow parent;
    private final InstructionSet instructionSet;
    private final TextFlow textFlow;
    private final Path highlight;
    private final List<Range> ranges = new ArrayList<>();
    private Font font;

    public DisassemblyView(FileContentWindow parent) {
        this.parent = parent;
        this.instructionSet = switch (parent.getFileEntry().getContentType()) {
            case BINARY_APPLE_PASCAL -> InstructionSetPCode.forApplePascal();
            case BINARY_Z80 -> InstructionSetZ80.forZ80();
            default -> InstructionSet6502.for65C02();
        };
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

            Optional<Range> found = ranges.stream().filter(r -> r.in(index)).findFirst();
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
        getChildren().setAll(textFlow, highlight);
        // TODO: Pascal
        refresh();
    }

    private void setFont(double size) {
        this.font = Font.font("monospaced", size);
        if (this.textFlow != null) {
            for (Node node : this.textFlow.getChildren()) {
                if (node instanceof Text text) {
                    text.setFont(font);
                }
            }
        }
    }

    @Override
    public Node getViewNode() {
        return this;
    }

    @Override
    public ButtonConfig getButtonConfig() {
        String buttonText = switch (parent.getFileEntry().getContentType()) {
            case BINARY_APPLE_PASCAL -> "p-code";
            case BINARY_Z80 -> "Z80";
            default -> "65C02";
        };
        return new ButtonConfig(buttonText, "view-disassembly.png");
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

    public void refresh() {
        ProdosAttributes attributes = parent.getFileEntry().getProdosAttributes();
        final int startAddress = attributes.auxType();
        final DataBuffer dataFork = parent.getFileEntry().getDataFork();
        final int endAddress = startAddress + dataFork.limit();
        Map<Integer,String> labels = new HashMap<>();
        List<Instruction> instructions = Disassembler.with(dataFork.asBytes())
                .startingAddress(startAddress)
                .use(instructionSet)
                .decode(labels);
        TextBuilder builder = new TextBuilder();
        List<Range> links = new ArrayList<>();
        int bytesPerLine = instructionSet.defaults().bytesPerInstruction();
        for (Instruction inst : instructions) {
            builder.addText("%04X- ", inst.address());
            byte[] code = inst.code();
            for (int i=0; i<bytesPerLine; i++) {
                if (i >= code.length) {
                    builder.addText("   ");
                }
                else {
                    builder.addLink("%02X", Byte.toUnsignedInt(code[i]));
                    builder.addText(" ");
                }
            }
            builder.addText(" %-10.10s ", labels.getOrDefault(inst.address(), ""));
            builder.addText("%-5s ", inst.mnemonic());
            // We may need to pad out 30 spaces, so track where we started.
            int startChar = builder.length();
            boolean firstOperand = true;
            for (Instruction.Operand operand : inst.operands()) {
                if (!firstOperand) builder.addText(",");
                firstOperand = false;
                int addr = operand.target().map(Instruction.Target::address).orElse(-1);
                String text = operand.format();
                if (operand.target().isPresent() && labels.containsKey(addr)) {
                    text = operand.format(labels.get(addr));
                }
                if (addr >= startAddress && addr < endAddress) {
                    links.add(builder.addLink(text));
                }
                else {
                    builder.addText(text);
                }
            }
            // Add comment - note we pad out 30 characters to align all comments.
            inst.description().ifPresent(description -> {
                while (builder.length() - startChar < 30) {
                    builder.addText(" ");
                }
                builder.addText("; %s", description);
            });
            builder.addText("\n");
        }
        Text text = new Text(builder.getText());
        text.setFont(font);
        textFlow.getChildren().setAll(text);
        ranges.clear();
        ranges.addAll(builder.getByteRanges());
        // Update the link highlights
        List<Node> nodes = new ArrayList<>();
        nodes.add(this.textFlow);
        Color linkColor = Color.web("#B8860B", 0.35);
        links.forEach(link -> {
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
        private final List<Range> byteRanges = new ArrayList<>();

        public String getText() {
            return sb.toString();
        }
        public List<Range> getByteRanges() {
            return byteRanges;
        }

        public int length() {
            return sb.length();
        }
        public void addText(String fmt, Object... args) {
            sb.append(String.format(fmt, args));
        }
        public Range addLink(String fmt, Object... args) {
            int firstChar = sb.length();
            addText(fmt, args);
            int lastChar = sb.length();
            // Range assumes it is _inclusive_.
            Range range = new Range(firstChar, lastChar-1);
            byteRanges.add(range);
            return range;
        }
    }
}
