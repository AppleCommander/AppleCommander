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
package com.webcodepro.applecommander.storage;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Extract the contents of an AWP (AppleWorks word processor) document and
 * convert to a text format.
 * See format documentation at:
 * 	http://www.gno.org/pub/apple2/doc/apple/filetypes/ftn.1a.xxxx
 * <p>
 * Date created: Nov 15, 2002 3:55:21 PM
 * @author: Rob Greene
 */
public class AppleWorksWordProcessorFileFilter implements FileFilter {
	public static final int RENDER_AS_TEXT = 0;
	public static final int RENDER_AS_HTML = 1;
	private int rendering = RENDER_AS_TEXT;
	/**
	 * Constructor for AppleWorksWordProcessorFileFilter.
	 */
	public AppleWorksWordProcessorFileFilter() {
		super();
	}
	/**
	 * Process the given FileEntry and return a byte array with filtered data.
	 * @see com.webcodepro.applecommander.storage.FileFilter#filter(FileEntry)
	 */
	public byte[] filter(FileEntry fileEntry) {
		byte[] fileData = fileEntry.getFileData();
		if (fileData[4] != 0x4f) return null;	// not an AWP file!
		ByteArrayOutputStream byteArray = new ByteArrayOutputStream(fileData.length);
		PrintWriter printWriter = new PrintWriter(byteArray, true);
		if (isHtmlRendering()) {
			printWriter.println("<html><style>BODY { font-family: monospace; }</style><body>");
		}
		boolean version3 = (fileData[183] != 0);
		int offset = 300 + (version3 ? 2 : 0);	// version 3.0's first line record is invalid
		while (offset < fileData.length) {
			int byte0 = AppleUtil.getUnsignedByte(fileData[offset++]);
			int byte1 = AppleUtil.getUnsignedByte(fileData[offset++]);
			
			if (byte0 == 0xff && byte1 == 0xff) {	// end of file
				break;
			} else if (byte1 == 0xd0) {			// Carriage return line records
				handleReturn(printWriter);
			} else if (byte1 > 0xd0) {				// Command line records
				if (isHtmlRendering()) {
					offset = handleCommandRecordAsHtml(byte0, byte1, printWriter, offset);
				}
			} else {								// Text records (assumed)
				offset = handleTextRecord(fileData, printWriter, offset);
			}
		}
		if (isHtmlRendering()) {
			printWriter.println("</body></html>");
		}
		return byteArray.toByteArray();
	}
	/**
	 * Deal with an individual text record.
	 */
	protected int handleTextRecord(byte[] fileData, PrintWriter printWriter, int offset) {
		int byte2 = AppleUtil.getUnsignedByte(fileData[offset++]);
		int byte3 = AppleUtil.getUnsignedByte(fileData[offset++]);
		boolean addReturn = (byte3 >= 0x80);
		int length = (byte3 & 0x7f);
		while (length > 0) {
			byte ch = fileData[offset++];
			length--;
			if (ch < 0x20) {	// special formatting character
				if (isHtmlRendering()) handleSpecialCodesAsHtml(printWriter, ch);
			} else {
				if (isHtmlRendering() && ch == ' ') {
					int extraSpaces = 0;
					while (fileData[offset+extraSpaces] == ' ') {
						extraSpaces++;
					}
					if (extraSpaces > 0) {
						printWriter.print("&nbsp;");
						while (fileData[offset] == ' ') {
							offset++;
							length--;
							printWriter.print("&nbsp;");
						}
					} else {
						printWriter.print((char)ch);
					}
				} else {
					printWriter.print((char)ch);
				}
			}
		}
		if (addReturn) handleReturn(printWriter);
		return offset;
	}
	/**
	 * Deal with carriage-return.
	 */
	protected void handleReturn(PrintWriter printWriter) {
		if (isHtmlRendering()) printWriter.println("<br>");
		else printWriter.println();
	}
	/**
	 * Process special coding of a text record.
	 */
	protected void handleSpecialCodesAsHtml(PrintWriter printWriter, byte ch) {
		switch (ch) {
			case 0x01:	printWriter.print("<b>");
						break;
			case 0x02:	printWriter.print("</b>");
						break;
			case 0x03:	printWriter.print("<sup>");
						break;
			case 0x04:	printWriter.print("</sup>");
						break;
			case 0x05:	printWriter.print("<sub>");
						break;
			case 0x06:	printWriter.print("</sub>");
						break;
			case 0x07:	printWriter.print("<u>");
						break;
			case 0x08:	printWriter.print("</u>");
						break;
			case 0x09:	printWriter.print("[Page#]");
						break;
			case 0x0b:	printWriter.print("&nbsp;");
						break;
			case 0x0e:	SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
						printWriter.print(dateFormat.format(new Date()));
						break;
			case 0x0f:	SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
						printWriter.print(timeFormat.format(new Date()));
						break;
		}
	}
	/**
	 * Deal with an individual command line record.
	 */
	protected int handleCommandRecordAsHtml(int byte0, int byte1, 
		PrintWriter printWriter, int offset) {
		
		switch (byte1) {
			case 0xd7:	printWriter.println("<style>BODY: text-align: right;</style>");
						break;
			case 0xdf:	printWriter.println("<style>BODY: text-align: justify;</style>");
						break;
			case 0xe0:	printWriter.println("<style>BODY: text-align: left;</style>");
						break;
			case 0xe1:	printWriter.println("<style>BODY: text-align: center;</style>");
						break;
			case 0xee:	for (int i=0; i<byte0; i++) {
							printWriter.println("<br>");
						}
						break;
		}
		return offset;
	}
	/**
	 * Give suggested file name.
	 * @see com.webcodepro.applecommander.storage.FileFilter#getSuggestedFileName(FileEntry)
	 */
	public String getSuggestedFileName(FileEntry fileEntry) {
		String fileName = fileEntry.getFilename().trim();
		String extension = ".txt";
		if (isHtmlRendering()) extension = ".html";

		if (!fileName.toLowerCase().endsWith(extension)) {
			fileName = fileName + extension;
		}
		return fileName;
	}
	/**
	 * Set the rendering method.
	 */
	public void setRendering(int rendering) {
		this.rendering = rendering;
	}
	/**
	 * Indicates if this is a text rendering.
	 */
	public boolean isTextRendering() {
		return rendering == RENDER_AS_TEXT;
	}
	/**
	 * Indicates if this is an HTML rendering.
	 */
	public boolean isHtmlRendering() {
		return rendering == RENDER_AS_HTML;
	}
}