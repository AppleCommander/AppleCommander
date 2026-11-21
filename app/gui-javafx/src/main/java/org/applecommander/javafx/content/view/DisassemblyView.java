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

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import org.applecommander.disassembler.api.Disassembler;
import org.applecommander.disassembler.api.Instruction;
import org.applecommander.disassembler.api.InstructionSet;
import org.applecommander.disassembler.api.mos6502.InstructionSet6502;
import org.applecommander.disassembler.api.pcode.InstructionSetPCode;
import org.applecommander.disassembler.api.z80.InstructionSetZ80;
import org.applecommander.javafx.content.FileContentView;
import org.applecommander.javafx.content.FileContentWindow;
import org.applecommander.transfer.ProdosAttributes;

import java.util.*;
import java.util.stream.Collectors;

public class DisassemblyView extends GridPane implements FileContentView {
    private final FileContentWindow parent;
    private final InstructionSet instructionSet;
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
        // TODO: Pascal
        refresh();
    }

    private void setFont(double size) {
        this.font = Font.font("monospaced", size);
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
        // TODO
    }

    public void refresh() {
        ProdosAttributes attributes = parent.getFileEntry().getProdosAttributes();
        Map<Integer,String> labels = new HashMap<>();
        List<Instruction> instructions = Disassembler.with(parent.getFileEntry().getDataFork().asBytes())
                .startingAddress(attributes.auxType())
                .use(instructionSet)
                .decode(labels);
        getChildren().clear();
        setHgap(5);
        // address, bytes, label, code, comments
        for (int i=0; i<instructions.size(); i++) {
            Instruction inst = instructions.get(i);
            Label address = new Label(String.format("%04X-", inst.address()));
            address.setFont(font);
            Label bytes = new Label(HexFormat.ofDelimiter(" ").formatHex(inst.code()));
            bytes.setFont(font);
            Label label = new Label(labels.getOrDefault(inst.address(), ""));
            label.setFont(font);
            Label code = new Label(String.format("%s %s", inst.mnemonic(), inst.operands().stream().map(operand -> {
                    if (operand.target().isPresent()) {
                        return operand.format(labels.getOrDefault(operand.target().get().address(), operand.format()));
                    } else {
                        return operand.format();
                    }
                })
                .collect(Collectors.joining(","))));
            code.setFont(font);
            String description = "";
            if (inst.description().isPresent()) {
                description = "; " + inst.description().get();
            }
            Label comment = new Label(description);
            comment.setFont(font);
            addRow(i, address, bytes, label, code, comment);
        }
    }
}
