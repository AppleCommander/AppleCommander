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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Launch AppleCommander.
 * This application attempts to identify which type of user-interface to
 * launch.  Additionally, there are some command-line interface switches
 * available - see the about method.
 * <p>
 * Regarding SWT, this appliation launcher tries to not be SWT dependent.
 * That means that SwtAppleCommander is launched purely by reflection.
 * NOTE: This may yet prove to be a worthless trick.  If it is, remove
 * the crud.  However, as the VERSION and COPYRIGHT are in this class and
 * are referenced in various places, it may well be worth it.
 * <p>
 * Date created: Nov 16, 2002 9:13:25 PM
 * @author: Rob Greene
 */
public class AppleCommander {
	public static final String VERSION = "1.3.2pre";
	public static final String COPYRIGHT = "Copyright (c) 2002-2003";
	/**
	 * Launch AppleCommander.
	 */
	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			if (isSwtAvailable()) {
				launchSwtAppleCommander(args);
			} else {
				showHelp();
			}
		} else {
			String[] extraArgs = new String[args.length - 1];
			System.arraycopy(args, 1, extraArgs, 0, extraArgs.length);
			if ("-swt".equalsIgnoreCase(args[0])) {
				launchSwtAppleCommander(args);
			} else if ("-swing".equalsIgnoreCase(args[0])) {
				System.err.println("Sorry, the Swing GUI is not available (yet).");
			} else if ("-command".equalsIgnoreCase(args[0])) {
				System.err.println("Sorry, the command line user interface is not available (yet).");
			} else if ("-help".equalsIgnoreCase(args[0]) || "-?".equalsIgnoreCase(args[0])) {
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
					"com.webcodepro.applecommander.ui.swt.SwtAppleCommander");
				Object object = swtAppleCommander.newInstance();
				Method launchMethod = swtAppleCommander.
					getMethod("launch", null);
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
			Class.forName("org.eclipse.swt.SWT");
			Class.forName("com.webcodepro.applecommander.ui.swt.SwtAppleCommander");
			return true;
		} catch (ClassNotFoundException ex) {
			return false;
		}
	}
	/**
	 * Display help message(s) for AppleCommander.
	 */
	protected static void showHelp() {
		System.err.println("AppleCommander general options:");
		System.err.println("-swt will launch the SWT version of AppleCommander.");
		System.err.println("     This requires the SWT jar and dll files to be present.");
		System.err.println("-swing will launch the Swing version of AppleCommander.");
		System.err.println("     (This is not implemented yet.)");
		System.err.println("-command will enter command interpreter mode.  (This is also");
		System.err.println("     not implemented yet.)");
		System.err.println("-help will show this help text.");
		System.err.println();
		ac.help();
	}
}
