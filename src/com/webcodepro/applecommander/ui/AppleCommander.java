/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-3 by Robert Greene
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
package com.webcodepro.applecommander.ui;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.webcodepro.applecommander.util.TextBundle;

/**
 * Launch AppleCommander.
 * This application attempts to identify which type of user-interface to
 * launch.  Additionally, there are some command-line interface switches
 * available - see the about method.
 * <p>
 * Regarding SWT, this application launcher tries to not be SWT dependent.
 * That means that SwtAppleCommander is launched purely by reflection.
 * NOTE: This may yet prove to be a worthless trick.  If it is, remove
 * the crud.  However, as the VERSION and COPYRIGHT are in this class and
 * are referenced in various places, it may well be worth it.
 * <p>
 * Date created: Nov 16, 2002 9:13:25 PM
 * @author Rob Greene
 */
public class AppleCommander {
	public static final String VERSION = "1.3.5.13"; //$NON-NLS-1$
	private static TextBundle textBundle = UiBundle.getInstance();
	/**
	 * Launch AppleCommander.
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			if (isSwtAvailable()) {
				launchSwtAppleCommander(args);
			} else if (isSwingAvailable()) {
				launchSwingAppleCommander(args);
			} else {
				showHelp();
			}
		} else {
			String[] extraArgs = new String[args.length - 1];
			System.arraycopy(args, 1, extraArgs, 0, extraArgs.length);
			if ("-swt".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				launchSwtAppleCommander(args);
			} else if ("-swing".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
                                System.err.println(textBundle.get("SwingVersionNotAvailable")); //$NON-NLS-1$
			        launchSwingAppleCommander(args);
			} else if ("-command".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				System.err.println(textBundle.get("CommandLineNotAvailable")); //$NON-NLS-1$
			} else if ("-help".equalsIgnoreCase(args[0])  //$NON-NLS-1$
					|| "-?".equalsIgnoreCase(args[0])) { //$NON-NLS-1$
				showHelp();
			} else {
				ac.main(args);
			}
		}
	}
	/**
	 * Launch the SWT version of AppleCommander.  This method
	 * uses reflection to load SwtAppleCommander to minimize which
	 * classes get loaded.  This is particularly important for the
	 * command-line version.
	 */
	protected static void launchSwtAppleCommander(String[] args) {
			Class swtAppleCommander;
			try {
				swtAppleCommander =	Class.forName(
					"com.webcodepro.applecommander.ui.swt.SwtAppleCommander"); //$NON-NLS-1$
				Object object = swtAppleCommander.newInstance();
				Method launchMethod = swtAppleCommander.
					getMethod("launch", null); //$NON-NLS-1$
				launchMethod.invoke(object, null);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			}
	}
	/**
	 * Test to see if SWT is available.
	 */
	protected static boolean isSwtAvailable() {
		try {
			Class.forName("org.eclipse.swt.SWT"); //$NON-NLS-1$
			Class.forName("com.webcodepro.applecommander.ui.swt.SwtAppleCommander"); //$NON-NLS-1$
			return true;
		} catch (ClassNotFoundException ex) {
			return false;
		}
	}
	/**
	 * Test to see if Swing is available.
	 */
	protected static boolean isSwingAvailable() {
		try {
			Class.forName("com.webcodepro.applecommander.ui.swing.SwingAppleCommander"); //$NON-NLS-1$
			return true;
		} catch (ClassNotFoundException ex) {
			return false;
		}
	}
	/**
	 * Launch the Swing version of AppleCommander.  This method
	 * uses reflection to load SwingAppleCommander to minimize which
	 * classes get loaded.  This is particularly important for the
	 * command-line version.
	 */
	protected static void launchSwingAppleCommander(String[] args) {
			Class swtAppleCommander;
			try {
				swtAppleCommander =	Class.forName(
					"com.webcodepro.applecommander.ui.swing.SwingAppleCommander"); //$NON-NLS-1$
				Object object = swtAppleCommander.newInstance();
				Method launchMethod = swtAppleCommander.
					getMethod("launch", null); //$NON-NLS-1$
				launchMethod.invoke(object, null);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			}
	}
	/**
	 * Display help message(s) for AppleCommander.
	 */
	protected static void showHelp() {
		System.err.println(textBundle.get("AppleCommanderHelp")); //$NON-NLS-1$
		System.err.println();
		ac.help();
	}
}
