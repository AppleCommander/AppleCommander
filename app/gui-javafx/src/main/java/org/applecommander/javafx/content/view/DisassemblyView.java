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
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import org.applecommander.disassembler.api.Disassembler;
import org.applecommander.disassembler.api.Instruction;
import org.applecommander.disassembler.api.InstructionSet;
import org.applecommander.disassembler.api.mos6502.InstructionSet6502;
import org.applecommander.disassembler.api.z80.InstructionSetZ80;
import org.applecommander.filestore.ContentType;
import org.applecommander.javafx.FxUtils;
import org.applecommander.javafx.content.FileContentWindow;
import org.applecommander.transfer.ProdosAttributes;
import org.applecommander.util.DataBuffer;

import java.util.*;

/// Provides a generic disassembly view that includes links to addresses contained in the file
/// as well as an optional code/data explorer mode.
public class DisassemblyView extends NavigableTextView {
    private final InstructionSet instructionSet;
    private final String buttonText;
    private final HBox toolBar;
    private final ToggleButton codeButton;
    private final ToggleButton mliButton;
    private final List<Integer> entryPoints = new ArrayList<>();

    public DisassemblyView(FileContentWindow parent) {
        if (parent.getFileEntry().getContentType() == ContentType.BINARY_Z80) {
            this.instructionSet = InstructionSetZ80.forZ80();
            this.buttonText = "Z80";
        }
        else {
            this.instructionSet = InstructionSet6502.for65C02();
            this.buttonText = "65C02";
        }
        super(parent);
        this.codeButton = FxUtils.createOnOffButton("code.png", "code.png",
                "Code", e -> refresh());
        this.mliButton = FxUtils.createOnOffButton("prodos-mli-off.png", "prodos-mli-on.png",
                "MLI", e -> refresh());
        this.toolBar = new HBox(this.codeButton, this.mliButton);
        this.toolBar.setSpacing(6);
        refresh();
    }

    /// Get the toggle button configuration. The `FileContentWindow` will set up the toolbar
    /// with this button. When the button is clicked, the view will be displayed.
    @Override
    public ButtonConfig getButtonConfig() {
        return new ButtonConfig(buttonText, "view-disassembly.png");
    }

    /// Provide any additional toolbar items. Note that a single button can be returned
    /// with as a `Button` but multiple buttons should be in an `HBox` or similar
    /// container.
    @Override
    public Optional<Node> getAdditionalToolbarButtons() {
        return Optional.of(this.toolBar);
    }

    /// Invoked when a byte is clicked on with the address of that byte. That address
    /// is used to toggle on or off the code entry point.
    @Override
    public void handleCustomLink(int value) {
        if (entryPoints.contains(value)) {
            // The integer remove method removes by index value instead of the object value.
            entryPoints.remove((Object)value);
        }
        else {
            entryPoints.add(value);
        }
        refresh();
    }

    /// Build the disassembly output. The disassembly must be re-processed each time
    /// due to likely changes in the entry point listing.
    public void build(TextBuilder builder) {
        if (codeButton == null) {
            // We get called early as everything is constructed; ignore the call.
            return;
        }
        ProdosAttributes attributes = parent.getFileEntry().getProdosAttributes();
        final int startAddress = attributes.auxType();
        final DataBuffer dataFork = parent.getFileEntry().getDataFork();
        final int endAddress = startAddress + dataFork.limit();
        Map<Integer,String> labels = new HashMap<>();
        Disassembler.Builder disassembler = Disassembler.with(dataFork.asBytes())
                .startingAddress(startAddress)
                .use(instructionSet);
        final boolean detectionMode = codeButton.isSelected();
        if (detectionMode) {
            if (entryPoints.isEmpty()) entryPoints.add(startAddress);
            entryPoints.forEach(disassembler::entryPoint);
        }
        final boolean mliMode = mliButton.isSelected();
        if (mliMode) {
            disassembler.customMapping(Disassembler.class.getResourceAsStream("/prodos-mli.toml"));
        }
        List<Instruction> instructions = disassembler.decode(labels);
        int bytesPerLine = instructionSet.defaults().bytesPerInstruction();
        for (Instruction inst : instructions) {
            builder.addNavigableTarget(inst.address(), "%04X", inst.address());
            builder.addText("- ");
            byte[] code = inst.code();
            for (int i=0; i<bytesPerLine; i++) {
                if (i >= code.length) {
                    builder.addText("   ");
                }
                else if (detectionMode) {
                    builder.addCustomLink(inst.address()+i, "%02X", Byte.toUnsignedInt(code[i]));
                    builder.addText(" ");
                }
                else {
                    builder.addText("%02X ", Byte.toUnsignedInt(code[i]));
                }
            }
            builder.addText(" %-10.10s ", labels.getOrDefault(inst.address(), ""));
            if (detectionMode) {
                if (entryPoints.contains(inst.address())) {
                    builder.addCustomLink(inst.address(), "--> ");
                }
                else {
                    builder.addText("    ");
                }
            }
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
                    builder.addNavigableLink(addr, text);
                }
                else {
                    builder.addText(text);
                }
            }
            // Add comment - note we pad 30 characters to align all comments.
            inst.description().ifPresent(description -> {
                while (builder.length() - startChar < 30) {
                    builder.addText(" ");
                }
                builder.addText("; %s", description);
            });
            builder.addText("\n");
        }
    }
}
