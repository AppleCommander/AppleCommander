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

import com.webcodepro.applecommander.ui.AppleCommander;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Extract the contents of an AWP (AppleWorks word processor) document and
 * convert to a text format.  Currently supported formats are plain text,
 * HTML, or RTF.  These are not exact duplicates, but they are close 
 * approximations.  RTF format is suitable for conversion to other word
 * processors.
 * <p>
 * To choose export format, use the appropriately named select method.
 * <p>
 * See AWP format documentation at:
 * 	http://www.gno.org/pub/apple2/doc/apple/filetypes/ftn.1a.xxxx
 * <p>
 * Date created: Nov 15, 2002 3:55:21 PM
 * @author: Rob Greene
 */
public class AppleWorksWordProcessorFileFilter implements FileFilter {
	/*
	 * This list identifies the various rendering options.
	 * As the internal format may change in the future, 
	 * the internal representation is hidden and the developer
	 * should use the appropriate select method.
	 */
	private static final int RENDER_AS_TEXT = 0;
	private static final int RENDER_AS_HTML = 1;
	private static final int RENDER_AS_RTF = 2;
	private int rendering = RENDER_AS_TEXT;
	/*
	 * Identifies the codes embedded in the AppleWorks file.
	 * FIXME: Need to ensure that all codes are defined.
	 */
	private static final int CODE_BOLD_ON = 0x01;
	private static final int CODE_BOLD_OFF = 0x02;
	private static final int CODE_SUPERSCRIPT_ON = 0x03;
	private static final int CODE_SUPERSCRIPT_OFF = 0x04;
	private static final int CODE_SUBSCRIPT_ON = 0x05;
	private static final int CODE_SUBSCRIPT_OFF = 0x06;
	private static final int CODE_UNDERLINE_ON = 0x07;
	private static final int CODE_UNDERLINE_OFF = 0x08;
	private static final int CODE_PAGE_NUMBER = 0x09;
	private static final int CODE_NONBREAKING_SPACE = 0x0b;
	private static final int CODE_DATE = 0x0e;
	private static final int CODE_TIME = 0x0f;
	/*
	 * Identifies the commands embedded in the AppleWorks file.
	 * FIXME: Need to ensure that all commands are defined.
	 */
	private static final int COMMAND_LEFT = 0xe0;
	private static final int COMMAND_RIGHT = 0xd7;
	private static final int COMMAND_CENTER = 0xe1;
	private static final int COMMAND_JUSTIFY = 0xedf;
	private static final int COMMAND_MULTIPLE_RETURNS = 0xee;

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
		} else if (isRtfRendering()) {
			printWriter.println("{\\rtf1");
			printWriter.print("{\\*\\generator AppleCommander ");
			printWriter.print(AppleCommander.VERSION);
			printWriter.println(";}");
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
				} else if (isRtfRendering()) {
					offset = handleCommandRecordAsRtf(byte0, byte1, printWriter, offset);
				} else {
					offset = handleCommandRecordAsText(byte0, byte1, printWriter, offset);
				}
			} else {								// Text records (assumed)
				offset = handleTextRecord(fileData, printWriter, offset);
			}
		}
		if (isHtmlRendering()) {
			printWriter.println("</body></html>");
		} else if (isRtfRendering()) {
			printWriter.println("}");
		}
		return byteArray.toByteArray();
	}
	/**
	 * Deal with an individual text record.
	 */
	protected int handleTextRecord(byte[] fileData, PrintWriter printWriter, int offset) {
		/* byte2 */ AppleUtil.getUnsignedByte(fileData[offset++]);
		int byte3 = AppleUtil.getUnsignedByte(fileData[offset++]);
		boolean addReturn = (byte3 >= 0x80);
		int length = (byte3 & 0x7f);
		while (length > 0) {
			byte ch = fileData[offset++];
			length--;
			if (ch < 0x20) {	// special formatting character
				if (isHtmlRendering()) handleSpecialCodesAsHtml(printWriter, ch);
				else if (isRtfRendering()) handleSpecialCodesAsRtf(printWriter, ch);
				else handleSpecialCodesAsText(printWriter, ch);
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
		else if (isRtfRendering()) printWriter.println("\\par");
		else printWriter.println();
	}
	/**
	 * Process special coding of a text record.
	 */
	protected void handleSpecialCodesAsHtml(PrintWriter printWriter, byte ch) {
		switch (ch) {
			case CODE_BOLD_ON:
						printWriter.print("<b>");
						break;
			case CODE_BOLD_OFF:
						printWriter.print("</b>");
						break;
			case CODE_SUPERSCRIPT_ON:
						printWriter.print("<sup>");
						break;
			case CODE_SUPERSCRIPT_OFF:
						printWriter.print("</sup>");
						break;
			case CODE_SUBSCRIPT_ON:
						printWriter.print("<sub>");
						break;
			case CODE_SUBSCRIPT_OFF:
						printWriter.print("</sub>");
						break;
			case CODE_UNDERLINE_ON:
						printWriter.print("<u>");
						break;
			case CODE_UNDERLINE_OFF:
						printWriter.print("</u>");
						break;
			case CODE_NONBREAKING_SPACE:
						printWriter.print("&nbsp;");
						break;
			default:	handleSpecialCodesAsText(printWriter, ch);
						break;
		}
	}
	/**
	 * Process special coding of a text record.
	 */
	protected void handleSpecialCodesAsRtf(PrintWriter printWriter, byte ch) {
		switch (ch) {
			case CODE_BOLD_ON:
						printWriter.print("\\b ");
						break;
			case CODE_BOLD_OFF:
						printWriter.print("\\b0");
						break;
			case CODE_UNDERLINE_ON:
						printWriter.print("\\ul ");
						break;
			case CODE_UNDERLINE_OFF:
						printWriter.print("\\ulnone");
						break;
			case CODE_NONBREAKING_SPACE:
						printWriter.print(" ");
						break;
			default:	handleSpecialCodesAsText(printWriter, ch);
						break;
		}
	}
	/**
	 * Process special coding of a text record.
	 */
	protected void handleSpecialCodesAsText(PrintWriter printWriter, byte ch) {
		switch (ch) {
			case CODE_PAGE_NUMBER:
						printWriter.print("[Page#]");
						break;
			case CODE_DATE:
						SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
						printWriter.print(dateFormat.format(new Date()));
						break;
			case CODE_TIME:
						SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
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
			case COMMAND_RIGHT:
						printWriter.println("<style>BODY: text-align: right;</style>");
						break;
			case COMMAND_JUSTIFY:
						printWriter.println("<style>BODY: text-align: justify;</style>");
						break;
			case COMMAND_LEFT:
						printWriter.println("<style>BODY: text-align: left;</style>");
						break;
			case COMMAND_CENTER:
						printWriter.println("<style>BODY: text-align: center;</style>");
						break;
			default:	offset = handleCommandRecordAsText(byte0, byte1, 
							printWriter, offset);
						break;
		}
		return offset;
	}
	/**
	 * Deal with an individual command line record.
	 */
	protected int handleCommandRecordAsRtf(int byte0, int byte1, 
		PrintWriter printWriter, int offset) {
		
		switch (byte1) {
			case COMMAND_RIGHT:
						printWriter.println("\\pard\\qr ");
						break;
			case COMMAND_LEFT:
						printWriter.println("\\pard ");
						break;
			case COMMAND_CENTER:
						printWriter.println("\\pard\\qc ");
						break;
			default:	offset = handleCommandRecordAsText(byte0, byte1, 
							printWriter, offset);
						break;
		}
		return offset;
	}
	/**
	 * Deal with an individual command line record.
	 */
	protected int handleCommandRecordAsText(int byte0, int byte1,
		PrintWriter printWriter, int offset) {
			
		switch (byte1) {	
			case COMMAND_MULTIPLE_RETURNS:
						for (int i=0; i<byte0; i++) {
							handleReturn(printWriter);
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
	protected void setRendering(int rendering) {
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
	/**
	 * Indicates if this is an RTF rendering.
	 */
	public boolean isRtfRendering() {
		return rendering == RENDER_AS_RTF;
	}
	/**
	 * Selects the text rendering engine.
	 */
	public void selectTextRendering() {
		rendering = RENDER_AS_TEXT;
	}
	/**
	 * Selects the HTML rendering engine.
	 */
	public void selectHtmlRendering() {
		rendering = RENDER_AS_HTML;
	}
	/**
	 * Selects the RTF rendering engine.
	 */
	public void selectRtfRendering() {
		rendering = RENDER_AS_RTF;
	}
}