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
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;

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
	private JTabbedPane tabPane;
	private JLabel titleLabel;
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
		JMenuBar menuBar = createMenuBar();
		JToolBar toolBar = new JToolBar();
		JPanel topPanel = new JPanel(new BorderLayout());
		tabPane = new JTabbedPane(JTabbedPane.TOP);
		topPanel.add(menuBar,BorderLayout.NORTH);
		topPanel.add(toolBar,BorderLayout.SOUTH);
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
		titleLabel = new JLabel(new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/webcodepro/applecommander/ui/images/AppleCommanderLogo.gif"))));
		addTitleTabPane();
		application.getContentPane().add(topPanel, BorderLayout.NORTH);
		application.getContentPane().add(tabPane, BorderLayout.CENTER);
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
		} else if ((e.getActionCommand().equals(textBundle.get("OpenButton"))) || //$NON-NLS-1$
			(e.getActionCommand().equals(textBundle.get("SwingAppleCommander.MenuFileOpen")))) {
			openFile();
		} else if (e.getActionCommand().equals(textBundle.get("SwingAppleCommander.MenuFileClose"))) {
			closeFile();
		} else if (e.getActionCommand().equals(textBundle.get("SwingAppleCommander.MenuFileQuit"))) { //$NON-NLS-1$
			UserPreferences.getInstance().save();
			setVisible(false);
			dispose();
			System.exit(0);
		} else {
			System.out.println("Unhandled action: "+e.getActionCommand());
		}
	}

	/**
	 * Set up the menu bar
	 */
	JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		// File
		JMenu menuFile = new JMenu(textBundle.get("SwingAppleCommander.MenuFile")); //$NON-NLS-1$
		// File->Open
	    JMenuItem openItem = new JMenuItem(textBundle.get("SwingAppleCommander.MenuFileOpen")); //$NON-NLS-1$
	    openItem.addActionListener(this);
		menuFile.add(openItem);
		// File->Close
	    JMenuItem closeItem = new JMenuItem(textBundle.get("SwingAppleCommander.MenuFileClose")); //$NON-NLS-1$
	    closeItem.addActionListener(this);
		menuFile.add(closeItem);
		// File->New
	    JMenuItem newItem = new JMenuItem(textBundle.get("SwingAppleCommander.MenuFileNew")); //$NON-NLS-1$
	    newItem.addActionListener(this);
		menuFile.add(newItem);
		// File->Exit
	    JMenuItem quitItem = new JMenuItem(textBundle.get("SwingAppleCommander.MenuFileQuit")); //$NON-NLS-1$
	    quitItem.addActionListener(this);
		menuFile.add(quitItem);
		menuBar.add(menuFile);
		return menuBar;
	}

	/**
	 * Add the title tab.
	 */
	void addTitleTabPane() {
		tabPane.add(textBundle.get("SwtAppleCommander.AppleCommander"),titleLabel);
	}

	/**
	 * Open a file.
	 */
	protected void openFile() {
		JFileChooser jc = new JFileChooser();
		String pathName = userPreferences.getDiskImageDirectory();
		if (null == pathName) {
			pathName = ""; //$NON-NLS-1$
			}
		jc.setCurrentDirectory(new File(pathName));
		EmulatorFileFilter ff = new EmulatorFileFilter();
		jc.setFileFilter(ff);
		int rc = jc.showDialog(this, textBundle.get("Open")); //$NON-NLS-1$
		if (rc == 0) {
			userPreferences.setDiskImageDirectory(jc.getSelectedFile().getParent());
			UserPreferences.getInstance().save();
			addDiskExplorerTab(jc.getSelectedFile());
		}
	}

	protected void addDiskExplorerTab(File file) {
		if (tabPane.getTitleAt(0).equals(textBundle.get("SwtAppleCommander.AppleCommander"))) {
			tabPane.remove(0);
		}
		tabPane.add(file.getName(),new DiskExplorer());
		tabPane.setSelectedIndex(tabPane.getTabCount()-1);
	}
	/**
	 * Close a file.
	 */
	protected void closeFile() {
		if (!tabPane.getTitleAt(0).equals(textBundle.get("SwtAppleCommander.AppleCommander"))) {
			tabPane.remove(tabPane.getSelectedIndex());
		}
		if (tabPane.getTabCount() == 0) {
			addTitleTabPane();
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
