/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2003 by Robert Greene
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
package com.webcodepro.applecommander.compiler;

import com.webcodepro.applecommander.compiler.ApplesoftCompiler;
import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DosFormatDisk;
import com.webcodepro.applecommander.storage.FileEntry;

import java.io.FileOutputStream;

import junit.framework.TestCase;

/**
 * Test the ApplesoftCompiler. 
 * @author Rob
 */
public class ApplesoftCompilerTest extends TestCase {
	/**
	 * Constructor for ApplesoftCompilerTest.
	 * @param arg0
	 */
	public ApplesoftCompilerTest(String arg0) {
		super(arg0);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(ApplesoftCompilerTest.class);
	}

	public void testCompileColors() throws Exception {
		DosFormatDisk disk = (DosFormatDisk) 
			new Disk("C:/My Apple2/Disks/MASTER.DSK").getFormattedDisks()[0];
		FileEntry fileEntry = disk.getFile("COLORS");
		ApplesoftCompiler compiler = new ApplesoftCompiler(fileEntry);
		compiler.setIntegerOnlyMath(true);
		byte[] assembly = compiler.compile();
		System.out.println(new String(assembly));
		FileOutputStream output = new FileOutputStream("C:/Temp/COLORS.s");
		output.write(assembly);
		output.close();
	}
}
