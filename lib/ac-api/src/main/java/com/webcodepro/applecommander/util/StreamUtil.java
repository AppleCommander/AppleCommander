/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2022 by Robert Greene
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
package com.webcodepro.applecommander.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Common stream-handling routines.
 * 
 * @author Rob Greene
 */
public class StreamUtil {
	public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
		byte[] data = new byte[1024];
		int bytes;
		while ((bytes = inputStream.read(data)) > 0) {
			outputStream.write(data, 0, bytes);
		}
		inputStream.close();
	}
}
