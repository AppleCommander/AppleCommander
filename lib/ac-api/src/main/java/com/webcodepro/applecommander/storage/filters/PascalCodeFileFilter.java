/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2025 by Robert Greene and others
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
package com.webcodepro.applecommander.storage.filters;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import org.applecommander.os.pascal.CodeFile;
import org.applecommander.os.pascal.PascalSupport;

import java.io.PrintWriter;
import java.io.StringWriter;

public class PascalCodeFileFilter implements FileFilter {
	public PascalCodeFileFilter() {
		super();
	}

	public byte[] filter(FileEntry fileEntry) {
        CodeFile codeFile = CodeFile.load(fileEntry.getFileData());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        PascalSupport.disassemble(pw, codeFile);
        return sw.toString().getBytes();
	}


	/**
	 * Give suggested file name.
	 */
	public String getSuggestedFileName(FileEntry fileEntry) {
		String fileName = fileEntry.getFilename().trim();
		if (!fileName.toLowerCase().endsWith(".lst")) {
			fileName = fileName + ".lst";
		}
		return fileName;
	}
}
