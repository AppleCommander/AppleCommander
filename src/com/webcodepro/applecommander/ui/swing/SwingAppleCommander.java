/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2008 by Robert Greene
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
package com.webcodepro.applecommander.ui.swing;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.filechooser.FileFilter;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.Disk.FilenameFilter;
import com.webcodepro.applecommander.ui.AppleCommander;
import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.UserPreferences;
import com.webcodepro.applecommander.util.TextBundle;

public class SwingAppleCommander extends JFrame implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3302293994498495537L;
	private UserPreferences userPreferences = UserPreferences.getInstance();
	private TextBundle textBundle = UiBundle.getInstance();

	/**
	 * Launch SwingAppleCommander.
	 */
	public static void main(String[] args) {
		new SwingAppleCommander().launch();
	}

	/**
	 * Launch SwingAppleCommander.
	 */
	public void launch() {
		JToolBar toolBar = new JToolBar();
		JButton aButton = new JButton(textBundle.get("OpenButton"), new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/webcodepro/applecommander/ui/images/opendisk.gif")))); //$NON-NLS-1$
		aButton.setToolTipText(textBundle.get("SwtAppleCommander.OpenDiskImageTooltip")); //$NON-NLS-1$
		aButton.setHorizontalTextPosition(JLabel.CENTER);
		aButton.setVerticalTextPosition(JLabel.BOTTOM);
	    aButton.addActionListener(this);
		toolBar.add(aButton);
		JButton aButton2 = new JButton(textBundle.get("CreateButton"), new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/webcodepro/applecommander/ui/images/newdisk.gif")))); //$NON-NLS-1$
		aButton2.setToolTipText(textBundle.get("SwtAppleCommander.CreateDiskImageTooltip")); //$NON-NLS-1$
		aButton2.setHorizontalTextPosition(JLabel.CENTER);
		aButton2.setVerticalTextPosition(JLabel.BOTTOM);
	    aButton2.addActionListener(this);
		toolBar.add(aButton2);
		JButton aButton3 = new JButton(textBundle.get("CompareButton"), new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/webcodepro/applecommander/ui/images/comparedisks.gif")))); //$NON-NLS-1$
		aButton3.setToolTipText(textBundle.get("SwtAppleCommander.CompareDiskImageTooltip")); //$NON-NLS-1$
		aButton3.setHorizontalTextPosition(JLabel.CENTER);
		aButton3.setVerticalTextPosition(JLabel.BOTTOM);
	    aButton3.addActionListener(this);
		toolBar.add(aButton3);
		JButton aButton4 = new JButton(textBundle.get("AboutButton"), new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/webcodepro/applecommander/ui/images/about.gif")))); //$NON-NLS-1$
		aButton4.setToolTipText(textBundle.get("SwtAppleCommander.AboutTooltip")); //$NON-NLS-1$
		aButton4.setHorizontalTextPosition(JLabel.CENTER);
		aButton4.setVerticalTextPosition(JLabel.BOTTOM);
	    aButton4.addActionListener(this);
		toolBar.add(aButton4);
		SwingAppleCommander application = new SwingAppleCommander();
		application.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/webcodepro/applecommander/ui/images/diskicon.gif"))); //$NON-NLS-1$
		application.setTitle(textBundle.get("SwtAppleCommander.AppleCommander"));
		JLabel label = new JLabel(new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/webcodepro/applecommander/ui/images/AppleCommanderLogo.gif"))));
		application.getContentPane().add(label, BorderLayout.CENTER);
		application.getContentPane().add(toolBar, BorderLayout.NORTH);
		application.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);

		application.pack();
		application.setVisible(true);
    }

	/**
	 * Constructor for SwingAppleCommander.
	 */
	public SwingAppleCommander() {
		super();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(textBundle.get("AboutButton"))) { //$NON-NLS-1$
			showAboutAppleCommander();
		} else if (e.getActionCommand().equals(textBundle.get("OpenButton"))) { //$NON-NLS-1$
			openFile();
		}
	}

	/**
	 * Open a file.
	 */
	protected void openFile() {
		JFileChooser jc = new JFileChooser();
		jc.setCurrentDirectory(new File(userPreferences.getDiskImageDirectory()));
		EmulatorFileFilter ff = new EmulatorFileFilter();
		jc.setFileFilter(ff);
		int rc = jc.showDialog(this, textBundle.get("Open")); //$NON-NLS-1$
		if (rc == 0) {
			userPreferences.setDiskImageDirectory(jc.getSelectedFile().getParent());
			UserPreferences.getInstance().save();
		}
	}

/*
		fileDialog.setFilterNames(names);
		fileDialog.setFilterExtensions(extensions);
		fileDialog.setFilterPath(userPreferences.getDiskImageDirectory());
		String fullpath = fileDialog.open();
	
		if (fullpath != null) {
			userPreferences.setDiskImageDirectory(fileDialog.getFilterPath());
			try {
				Disk disk = new Disk(fullpath);
				FormattedDisk[] formattedDisks = disk.getFormattedDisks();
				if (formattedDisks != null) {
					DiskWindow window = new DiskWindow(shell, formattedDisks, imageManager);
					window.open();
				} else {
					showUnrecognizedDiskFormatMessage(fullpath);
				}
			} catch (Exception ignored) {
				ignored.printStackTrace();
				showUnrecognizedDiskFormatMessage(fullpath);
			}
		}
*/

	public void showAboutAppleCommander() {
		JOptionPane.showMessageDialog(null,
			textBundle.format("SwtAppleCommander.AboutMessage", //$NON-NLS-1$
			new Object[] { AppleCommander.VERSION, textBundle.get("Copyright") }), textBundle.get("SwtAppleCommander.AboutTitle"), //$NON-NLS-1$
			JOptionPane.INFORMATION_MESSAGE);
	}
}
