/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002 by Robert Greene
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

import com.webcodepro.applecommander.ui.swt.SwtAppleCommander;

/**
 * Launch AppleCommander.
 * This application attempts to identify which type of user-interface to
 * launch.  Additionally, there are some command-line interface switches
 * available - see the about method.
 * <p>
 * Date created: Nov 16, 2002 9:13:25 PM
 * @author: Rob Greene
 */
public class AppleCommander {
	public static final String VERSION = "1.2.0";
	public static final String COPYRIGHT = "Copyright (c) 2002-2003";
	/**
	 * Launch AppleCommander.
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			try {
				Class.forName("org.eclipse.swt.SWT");
				SwtAppleCommander.main(args);
			} catch (ClassNotFoundException ex) {
				System.err.println("Sorry, the SWT libraries do not appear to be available (yet).");
				//SwingAppleCommander.main(args);
			}
		} else {
			String[] extraArgs = new String[args.length - 1];
			System.arraycopy(args, 1, extraArgs, 0, extraArgs.length);
			if ("-swt".equalsIgnoreCase(args[0])) {
				SwtAppleCommander.main(extraArgs);
			} else if ("-swing".equalsIgnoreCase(args[0])) {
				System.err.println("Sorry, the Swing GUI is not available (yet).");
				//SwingAppleCommander.main(extraArgs);
			} else if ("-command".equalsIgnoreCase(args[0])) {
				System.err.println("Sorry, the command line user interface is not available (yet).");
				//CommandLineAppleCommander.main(extraArgs);
			} else {
				System.err.println("Unknown user interface specified!");
				System.err.println("Use -swt, -swing, or -command.");
			}
		}
	}
}
