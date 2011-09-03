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
package com.webcodepro.applecommander.storage.filters;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.ui.AppleCommander;
import com.webcodepro.applecommander.util.AppleUtil;

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
 * @author Rob Greene
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
	/**
	 * Indicates if we are in a header or footer.  This is somewhat of a 
	 * hack to generate a RTF file correctly.  If a header is found (and
	 * presumably a footer), there isn't necessarily a header end code
	 * included.
	 */
	private boolean inHeaderOrFooter = false;
	/**
	 * This constant indicates how many TWIPS there are per inch.
	 * I'm fairly certain of this number, but it may need tweaking.
	 */
	private static final int TWIPS_PER_INCH = 1440;
	/*
	 * Identifies the codes embedded in the AppleWorks file.
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
//	private static final int CODE_ENTER_KEYBOARD = 0x0a;
	private static final int CODE_STICKY_SPACE = 0x0b;
//	private static final int CODE_MAILMERGE_BEGIN = 0x0c;
//	private static final int CODE_RESERVED1 = 0x0d;
	private static final int CODE_DATE = 0x0e;
	private static final int CODE_TIME = 0x0f;
//	private static final int CODE_SPECIAL_1 = 0x10;
//	private static final int CODE_SPECIAL_2 = 0x11;
//	private static final int CODE_SPECIAL_3 = 0x12;
//	private static final int CODE_SPECIAL_4 = 0x13;
//	private static final int CODE_SPECIAL_5 = 0x14;
//	private static final int CODE_SPECIAL_6 = 0x15;
//	private static final int CODE_TAB = 0x16;
//	private static final int CODE_TAB_FILL = 0x17;
//	private static final int CODE_RESERVED2 = 0x18;
	/*
	 * Identifies the commands embedded in the AppleWorks file.
	 */
//	private static final int COMMAND_RESERVED = 0xd4;
	private static final int COMMAND_PAGEHEADER_END = 0xd5;
	private static final int COMMAND_PAGEFOOTER_END = 0xd6;
	private static final int COMMAND_RIGHT = 0xd7;
	private static final int COMMAND_PLATEN_WIDTH = 0xd8;	// 10ths of an inch
	private static final int COMMAND_MARGIN_LEFT = 0xd9;		// 10ths of an inch
	private static final int COMMAND_MARGIN_RIGHT = 0xda;	// 10ths of an inch
//	private static final int COMMAND_CHARS_PER_INCH = 0xdb;
//	private static final int COMMAND_PROPORTIONAL_1 = 0xdc;
//	private static final int COMMAND_PROPORTIONAL_2 = 0xdd;
//	private static final int COMMAND_INDENT = 0xde;			// in characters
	private static final int COMMAND_JUSTIFY = 0xdf;
	private static final int COMMAND_LEFT = 0xe0;
	private static final int COMMAND_CENTER = 0xe1;
	private static final int COMMAND_PAPER_LENGTH = 0xe2;	// 10ths of an inch
	private static final int COMMAND_MARGIN_TOP = 0xe3;		// 10ths of an inch
	private static final int COMMAND_MARGIN_BOTTOM = 0xe4;	// 10ths of an inch
//	private static final int COMMAND_LINES_PER_INCH = 0xe5;
//	private static final int COMMAND_SINGLE_SPACE = 0xe6;
//	private static final int COMMAND_DOUBLE_SPACE = 0xe7;
//	private static final int COMMAND_TRIPLE_SPACE = 0xe8;
	private static final int COMMAND_NEW_PAGE = 0xe9;
//	private static final int COMMAND_GROUP_BEGIN = 0xea;
//	private static final int COMMAND_GROUP_END = 0xeb;
	private static final int COMMAND_PAGEHEADER = 0xed;		// may be mixed up
	private static final int COMMAND_PAGEFOOTER = 0xec;		// with this...
	private static final int COMMAND_SKIP_LINES = 0xee;
//	private static final int COMMAND_PAGE_NUMBER = 0xef;
//	private static final int COMMAND_PAUSE_EACH_PAGE = 0xf0;
//	private static final int COMMAND_PAUSE_HERE = 0xf1;
//	private static final int COMMAND_SET_MARKER = 0xf2;
//	private static final int COMMAND_PAGE_NUMBER_256 = 0xf3;	// add 256
	private static final int COMMAND_PAGE_BREAK = 0xf4;		// byte page#
	private static final int COMMAND_PAGE_BREAK_256 = 0xf5;	// byte page# + 256
//	private static final int COMMAND_PP_PAGE_BREAK = 0xf6;	// break in midl/par.
//	private static final int COMMAND_PP_PAGE_BREAK_256 = 0xf7;	// +256 ??
	private static final int COMMAND_EOF = 0xff;				// END OF FILE

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
			printWriter.println("<html><style>BODY { font-family: monospace; }</style><body>"); //$NON-NLS-1$
		} else if (isRtfRendering()) {
			printWriter.print("{\\rtf1"); //$NON-NLS-1$
			printWriter.print("{\\fonttbl{\\f0\\fmodern\\fprq1\\fcharset0 Courier New;}}"); //$NON-NLS-1$
			printWriter.print("{\\*\\generator AppleCommander "); //$NON-NLS-1$
			printWriter.print(AppleCommander.VERSION);
			printWriter.println(";}"); //$NON-NLS-1$
			printWriter.print("\\f0 "); //$NON-NLS-1$
		}
		boolean version3 = (fileData[183] != 0);
		int offset = 300 + (version3 ? 2 : 0);	// version 3.0's first line record is invalid
		while (offset < fileData.length) {
			int byte0 = AppleUtil.getUnsignedByte(fileData[offset++]);
			int byte1 = AppleUtil.getUnsignedByte(fileData[offset++]);
			
			if (byte0 == COMMAND_EOF && byte1 == COMMAND_EOF) {
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
			printWriter.println("</body></html>"); //$NON-NLS-1$
		} else if (isRtfRendering()) {
			printWriter.println("}"); //$NON-NLS-1$
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
		while ((length > 0) && (offset < fileData.length)) {
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
						printWriter.print("&nbsp;"); //$NON-NLS-1$
						while (fileData[offset] == ' ') {
							offset++;
							length--;
							printWriter.print("&nbsp;"); //$NON-NLS-1$
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
		if (isHtmlRendering()) printWriter.println("<br>"); //$NON-NLS-1$
		else if (isRtfRendering()) printWriter.println("\\par"); //$NON-NLS-1$
		else printWriter.println();
	}
	/**
	 * Process special coding of a text record.
	 */
	protected void handleSpecialCodesAsHtml(PrintWriter printWriter, byte ch) {
		switch (ch) {
			case CODE_BOLD_ON:
						printWriter.print("<b>"); //$NON-NLS-1$
						break;
			case CODE_BOLD_OFF:
						printWriter.print("</b>"); //$NON-NLS-1$
						break;
			case CODE_SUPERSCRIPT_ON:
						printWriter.print("<sup>"); //$NON-NLS-1$
						break;
			case CODE_SUPERSCRIPT_OFF:
						printWriter.print("</sup>"); //$NON-NLS-1$
						break;
			case CODE_SUBSCRIPT_ON:
						printWriter.print("<sub>"); //$NON-NLS-1$
						break;
			case CODE_SUBSCRIPT_OFF:
						printWriter.print("</sub>"); //$NON-NLS-1$
						break;
			case CODE_UNDERLINE_ON:
						printWriter.print("<u>"); //$NON-NLS-1$
						break;
			case CODE_UNDERLINE_OFF:
						printWriter.print("</u>"); //$NON-NLS-1$
						break;
			case CODE_STICKY_SPACE:
						printWriter.print("&nbsp;"); //$NON-NLS-1$
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
			case CODE_PAGE_NUMBER:
						printWriter.print("{\\chpgn}"); //$NON-NLS-1$
						break;
			case CODE_BOLD_ON:
						printWriter.print("\\b "); //$NON-NLS-1$
						break;
			case CODE_BOLD_OFF:
						printWriter.print("\\b0 "); //$NON-NLS-1$
						break;
			case CODE_UNDERLINE_ON:
						printWriter.print("\\ul "); //$NON-NLS-1$
						break;
			case CODE_UNDERLINE_OFF:
						printWriter.print("\\ulnone"); //$NON-NLS-1$
						break;
			case CODE_SUPERSCRIPT_ON:
						printWriter.print("\\super "); //$NON-NLS-1$
						break;
			case CODE_SUBSCRIPT_ON:
						printWriter.print("\\sub "); //$NON-NLS-1$
						break;
			case CODE_SUPERSCRIPT_OFF:
			case CODE_SUBSCRIPT_OFF:
						printWriter.print("\\nosupersub "); //$NON-NLS-1$
						break;
			case CODE_STICKY_SPACE:
						printWriter.print(" "); //$NON-NLS-1$
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
						printWriter.print("[Page#]"); //$NON-NLS-1$
						break;
			case CODE_DATE:
						SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy"); //$NON-NLS-1$
						printWriter.print(dateFormat.format(new Date()));
						break;
			case CODE_TIME:
						SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss"); //$NON-NLS-1$
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
						printWriter.println("<style>BODY: text-align: right;</style>"); //$NON-NLS-1$
						break;
			case COMMAND_JUSTIFY:
						printWriter.println("<style>BODY: text-align: justify;</style>"); //$NON-NLS-1$
						break;
			case COMMAND_LEFT:
						printWriter.println("<style>BODY: text-align: left;</style>"); //$NON-NLS-1$
						break;
			case COMMAND_CENTER:
						printWriter.println("<style>BODY: text-align: center;</style>"); //$NON-NLS-1$
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
		
		if (inHeaderOrFooter) {
			printWriter.print("}\\f0 "); //$NON-NLS-1$
			inHeaderOrFooter = false;
		}
		int twipDistance = byte0 * TWIPS_PER_INCH / 10;
		switch (byte1) {
			case COMMAND_PAGEHEADER:
						printWriter.print("{\\header "); //$NON-NLS-1$
						inHeaderOrFooter = true;
						break;
			case COMMAND_PAGEFOOTER:
						printWriter.print("{\\footer "); //$NON-NLS-1$
						inHeaderOrFooter = true;
						break;
			case COMMAND_PAGEHEADER_END:
			case COMMAND_PAGEFOOTER_END:
						printWriter.print("}"); //$NON-NLS-1$
						break;
			case COMMAND_RIGHT:
						printWriter.println("\\pard\\qr "); //$NON-NLS-1$
						break;
			case COMMAND_LEFT:
						printWriter.println("\\pard "); //$NON-NLS-1$
						break;
			case COMMAND_CENTER:
						printWriter.println("\\pard\\qc "); //$NON-NLS-1$
						break;
			case COMMAND_JUSTIFY:
						printWriter.print("\\qj "); //$NON-NLS-1$
						break;
			case COMMAND_PAGE_BREAK:
			case COMMAND_PAGE_BREAK_256:
			case COMMAND_NEW_PAGE:
						printWriter.print("\\page "); //$NON-NLS-1$
						break;
			case COMMAND_PLATEN_WIDTH:
						printWriter.print("\\paperw"); //$NON-NLS-1$
						printWriter.print(twipDistance);
						printWriter.print(" "); //$NON-NLS-1$
						break;
			case COMMAND_PAPER_LENGTH:
						printWriter.print("\\paperl"); //$NON-NLS-1$
						printWriter.print(twipDistance);
						printWriter.print(" "); //$NON-NLS-1$
						break;
			case COMMAND_MARGIN_LEFT:
						printWriter.print("\\margl"); //$NON-NLS-1$
						printWriter.print(twipDistance);
						printWriter.print(" "); //$NON-NLS-1$
						break;
			case COMMAND_MARGIN_RIGHT:
						printWriter.print("\\margr"); //$NON-NLS-1$
						printWriter.print(twipDistance);
						printWriter.print(" "); //$NON-NLS-1$
						break;
			case COMMAND_MARGIN_TOP:
						printWriter.print("\\margt"); //$NON-NLS-1$
						printWriter.print(twipDistance);
						printWriter.print(" "); //$NON-NLS-1$
						break;
			case COMMAND_MARGIN_BOTTOM:
						printWriter.print("\\margb"); //$NON-NLS-1$
						printWriter.print(twipDistance);
						printWriter.print(" "); //$NON-NLS-1$
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
			case COMMAND_SKIP_LINES:
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
		String extension = ".txt"; //$NON-NLS-1$
		if (isHtmlRendering()) extension = ".html"; //$NON-NLS-1$
		else if (isRtfRendering()) extension = ".rtf"; //$NON-NLS-1$

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