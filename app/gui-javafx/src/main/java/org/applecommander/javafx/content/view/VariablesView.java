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

import org.applecommander.converter.ApplesoftVariables;
import org.applecommander.converter.ConverterRepository;
import org.applecommander.filestore.FileEntry;
import org.applecommander.javafx.content.FileContentWindow;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Collectors;

public class VariablesView extends BaseTextView {
    public VariablesView(FileContentWindow parent) {
        super(parent);

        FileEntry fileEntry = parent.getFileEntry();
        ConverterRepository.getVariables(fileEntry).ifPresent(this::showVariables);
    }

    @Override
    public ButtonConfig getButtonConfig() {
        return new ButtonConfig("Variables", "view-basic.png");
    }

    private void showVariables(ApplesoftVariables variables) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.printf("Simple variables:\n");
        var names = variables.getSimpleNames();
        names.forEach(varName -> {
            if (varName.endsWith("$")) {
                pw.printf("%s = \"%s\"\n", varName, variables.getSimpleValue(varName));
            }
            else {
                pw.printf("%s = %s\n", varName, variables.getSimpleValue(varName));
            }
        });
        if (names.isEmpty()) {
            pw.printf("-- None --\n");
        }

        pw.printf("\nArray variables:\n");
        names = variables.getArrayNames();
        names.forEach(varName -> {
            pw.printf("DIM %s(%s)\n", varName, variables.getArrayDimensions(varName).stream()
                    .map(Object::toString).collect(Collectors.joining(",")));
        });
        if (names.isEmpty()) {
            pw.printf("-- None --\n");
        }

        setText(sw.toString());
    }
}
