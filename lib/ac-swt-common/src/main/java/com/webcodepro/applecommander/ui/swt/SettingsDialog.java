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
package com.webcodepro.applecommander.ui.swt;

import com.webcodepro.applecommander.ui.UserPreferences;
import com.webcodepro.applecommander.ui.swt.util.SwtUtil;
import com.webcodepro.applecommander.util.Host;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The settings dialog allows some management of AppleCommander behavior.
 * It also shows some of the "hidden" things that people may be interested in.
 */
public class SettingsDialog {
    private final UserPreferences preference = UserPreferences.getInstance();
    private final Shell parent;
    private Shell dialog;
    private Combo backupStrategyCombo;
    private Button backupBrowseButton;
    private Text backupDirectoryText;

    // Temporary state of the window settings
    private boolean saveWindowSize;
    private boolean centerWindow;

    public SettingsDialog(Shell parent) {
        this.parent = parent;
    }

    public void open() {
        int styles = SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE;
        if (Host.isMacosx()) {
            styles |= SWT.SHEET;
        }
        dialog = new Shell(parent, styles);
        dialog.setText("Settings");
        GridLayout dialogLayout = new GridLayout();
        dialogLayout.marginLeft = 5;
        dialogLayout.marginRight = 5;
        dialogLayout.marginBottom = 5;
        dialogLayout.marginTop = 5;
        dialogLayout.verticalSpacing = 10;
        dialog.setLayout(dialogLayout);

        TabFolder tabFolder = new TabFolder(dialog, SWT.NONE);
        tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

        GridLayout tabItemLayout = new GridLayout(2, false);
        tabItemLayout.marginLeft = 5;
        tabItemLayout.marginRight = 5;
        tabItemLayout.verticalSpacing = 10;
        tabItemLayout.horizontalSpacing = 10;
        tabItemLayout.marginTop = 5;
        tabItemLayout.marginBottom = 5;

        GridData rowSpanGridData = new GridData(GridData.FILL_HORIZONTAL);
        rowSpanGridData.horizontalSpan = 2;

        createWindowTab(tabFolder, tabItemLayout);
        createBackupTab(tabFolder, tabItemLayout, rowSpanGridData);
        createInfoTab(tabFolder, tabItemLayout, rowSpanGridData);

        // Bottom row of buttons
        Composite buttonRow = new Composite(dialog, SWT.NONE);
        GridData buttonRowGridData = new GridData();
        buttonRowGridData.grabExcessHorizontalSpace = true;
        buttonRowGridData.horizontalAlignment = GridData.CENTER;
        buttonRow.setLayoutData(buttonRowGridData);
        buttonRow.setLayout(new FillLayout(SWT.HORIZONTAL));
        Button button = new Button(buttonRow, SWT.PUSH);
        button.setText("Cancel");
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                dialog.close();
            }
        });
        button = new Button(buttonRow, SWT.PUSH);
        button.setText("Save");
        dialog.setDefaultButton(button);
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                savePreferences();
                dialog.close();
            }
        });

        dialog.pack();
        SwtUtil.center(parent, dialog);
        dialog.open();
    }

    public void savePreferences() {
        // Window tab
        preference.setSaveWindowSize(saveWindowSize);
        preference.setCenterWindow(centerWindow);

        // Backup tab
        String strategy = switch (backupStrategyCombo.getSelectionIndex()) {
            case 1 -> "bak";
            case 2 -> backupDirectoryText.getText();
            default -> "";
        };
        preference.setBackupStrategy(strategy);

        preference.save();
    }

    public void createWindowTab(TabFolder tabFolder, GridLayout tabItemLayout) {
        TabItem item = new TabItem(tabFolder, SWT.NONE);
        item.setText("Window");
        Composite control = new Composite(tabFolder, SWT.NONE);
        control.setLayout(tabItemLayout);
        item.setControl(control);

        Label label = new Label(control, SWT.NONE);
        label.setText("Save Window Size?");
        saveWindowSize = preference.getSaveWindowSize();
        addYesNoRadio(control, saveWindowSize, flag -> saveWindowSize = flag);

        label = new Label(control, SWT.NONE);
        label.setText("Center Window on Screen?");
        centerWindow = preference.getCenterWindow();
        addYesNoRadio(control, centerWindow, flag -> centerWindow = flag);
    }

    private void addYesNoRadio(Composite control, boolean value, Consumer<Boolean> callback) {
        Composite ynPanel = new Composite(control, SWT.NONE);
        FillLayout ynLayout = new FillLayout(SWT.HORIZONTAL);
        ynLayout.spacing = 10;
        ynPanel.setLayout(ynLayout);

        Button button = new Button(ynPanel, SWT.RADIO);
        button.setText("Yes");
        button.setSelection(value);
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                callback.accept(true);
            }
        });

        button = new Button(ynPanel, SWT.RADIO);
        button.setText("No");
        button.setSelection(!value);
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                callback.accept(false);
            }
        });
    }

    public void createBackupTab(TabFolder tabFolder, GridLayout tabItemLayout, GridData rowSpanGridData) {
        TabItem item = new TabItem(tabFolder, SWT.NONE);
        item.setText("Backup");
        Composite control = new Composite(tabFolder, SWT.NONE);
        control.setLayout(tabItemLayout);
        item.setControl(control);

        Label label = new Label(control, SWT.WRAP);
        label.setText("""
                AppleCommander has the ability to backup an image on Save.
                This does not impact Save As, since the image is under a new name.
                """);
        label.setLayoutData(rowSpanGridData);

        label = new Label(control, SWT.NONE);
        label.setText("Strategy:");
        backupStrategyCombo = new Combo(control, SWT.READ_ONLY);
        backupStrategyCombo.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL));
        backupStrategyCombo.setItems("Disabled", "Append '.bak'", "To directory");
        backupStrategyCombo.select(switch (preference.getBackupStrategy()) {
            case "bak" -> 1;
            case "" -> 0;
            default -> 2;
        });
        backupStrategyCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                backupBrowseButton.setEnabled(backupStrategyCombo.getSelectionIndex() == 2);
                if (backupStrategyCombo.getSelectionIndex() < 2) {
                    backupDirectoryText.setText("");
                }
            }
        });

        label = new Label(control, SWT.NONE);
        label.setText("... To?");
        backupDirectoryText = new Text(control, SWT.WRAP | SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
        float fontHeight = backupDirectoryText.getFont().getFontData()[0].height;
        GridData textGridData = new GridData(GridData.FILL_BOTH);
        textGridData.heightHint = (int)fontHeight * 4;
        backupDirectoryText.setLayoutData(textGridData);
        backupDirectoryText.setText(switch (preference.getBackupStrategy()) {
            case "", "bak" -> "";
            default -> preference.getBackupStrategy();
        });
        label = new Label(control, SWT.NONE);   // spacing
        backupBrowseButton = new Button(control, SWT.PUSH);
        backupBrowseButton.setText("Browse...");
        backupBrowseButton.setEnabled(backupStrategyCombo.getSelectionIndex() == 2);
        backupBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dialog = new DirectoryDialog(parent.getShell());
                dialog.setFilterPath(backupDirectoryText.getText());
                dialog.setText("Select backup directory");
                dialog.setMessage("Please select the backup directory.");
                Optional<String> result = dialog.openDialog();
                result.ifPresent(backupDirectoryText::setText);
            }
        });
    }

    public void createInfoTab(TabFolder tabFolder, GridLayout tabItemLayout, GridData rowSpanGridData) {
        TabItem item = new TabItem(tabFolder, SWT.NONE);
        item.setText("Info");
        Composite control = new Composite(tabFolder, SWT.NONE);
        control.setLayout(tabItemLayout);
        item.setControl(control);

        BiConsumer<String,String> infoPairings = (key, value) -> {
            Label label = new Label(control, SWT.NONE);
            label.setText(key);
            label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
            label = new Label(control, SWT.WRAP | SWT.BORDER);
            label.setText(value);
            label.setLayoutData(new GridData(GridData.FILL_BOTH));
        };
        infoPairings.accept("Location:", preference.getPreferencesPath());

        Label label = new Label(control, SWT.WRAP);
        label.setText("""
                These are the current directories that AppleCommander is using.
                They change as you select different folders for a related activity.
                """);
        label.setLayoutData(rowSpanGridData);

        infoPairings.accept("Images:", preference.getDiskImageDirectory());
        infoPairings.accept("Export:", preference.getExportDirectory());
        infoPairings.accept("Import:", preference.getImportDirectory());
        infoPairings.accept("Save:", preference.getSaveDirectory());
    }
}
