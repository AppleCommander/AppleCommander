/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002, 2008 by Robert Greene
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
import java.util.StringTokenizer;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.ui.AppleCommander;

/**
 * Extract the contents of an ancient word processor file (might be Word
 * Perfect) and convert to a text format. Currently supported formats are plain
 * text, HTML, or RTF. These are not exact duplicates, but they are close
 * approximations. RTF format is suitable for conversion to other word
 * processors.
 * <p>
 * To choose export format, use the appropriately named select method.
 * <p>
 * Date created: Dec 18, 2008 9:09:21 AM
 * 
 * @author David Schmidt
 */
public class GutenbergFileFilter implements FileFilter {
	/*
	 * This list identifies the various rendering options. As the internal
	 * format may change in the future, the internal representation is hidden
	 * and the developer should use the appropriate select method.
	 */
	private static final int RENDER_AS_TEXT = 0;
	private static final int RENDER_AS_HTML = 1;
	private static final int RENDER_AS_RTF = 2;
	private int rendering = RENDER_AS_RTF;
	/**
	 * Constructor for GutenbergFileFilter.
	 */
	public GutenbergFileFilter() {
		super();
	}

	/**
	 * Process the given FileEntry and return a byte array with filtered data.
	 * 
	 * @see com.webcodepro.applecommander.storage.FileFilter#filter(FileEntry)
	 */
	public byte[] filter(FileEntry fileEntry) {
		byte[] fileData = fileEntry.getFileData();
		int offset = 0;
		ByteArrayOutputStream byteArray = new ByteArrayOutputStream(
				fileData.length);
		PrintWriter printWriter = new PrintWriter(byteArray, true);
		while (offset < fileData.length) {
			fileData[offset] = (byte) (fileData[offset++] & 0x7f);
		}
		String preprocess = new String(fileData).trim();
		handleTranslation(preprocess, printWriter, rendering);
		printWriter.flush();
		return byteArray.toByteArray();
	}

	/**
	 * Transform text into desired destination format
	 */
	protected void handleTranslation(String raw, PrintWriter output, int rendering) {
		boolean ignoreBr = false;
		boolean inHeader = false;
		boolean inItalics = false;
		boolean inBold = false;
		boolean inCenter = false;
		boolean inUnderline = false;
		boolean inSuperscript = false;
		String cooked = raw.replaceAll("\\x00", "");	//$NON-NLS-1$ $NON-NLS-2$ Remove nulls
		cooked=cooked.replaceAll("<[a|A]1>", "");		//$NON-NLS-1$ $NON-NLS-2$ File start
		cooked=cooked.replaceAll("<e9>", "");			//$NON-NLS-1$ $NON-NLS-2$ End of file
		cooked=cooked.replaceAll("<d1>", "");			//$NON-NLS-1$ $NON-NLS-2$ File start
		cooked=cooked.replaceAll("<s1>", "");			//$NON-NLS-1$ $NON-NLS-2$ File start
		cooked=cooked.replaceAll("<2>", "");			//$NON-NLS-1$ $NON-NLS-2$ Dunno what this is
		cooked=cooked.replaceAll("<f9>", "");			//$NON-NLS-1$ $NON-NLS-2$ Dunno what this is
		cooked=cooked.replaceAll("<i>", "");			//$NON-NLS-1$ $NON-NLS-2$ Dunno what this is
		cooked=cooked.replaceAll("<i1>", "");			//$NON-NLS-1$ $NON-NLS-2$ Dunno what this is
		cooked=cooked.replaceAll("<[h|H]1>(.*)", "<h1>$1</h1>");	//$NON-NLS-1$ $NON-NLS-2$ Bound a h1 heading
		cooked=cooked.replaceAll("<[h|H]2>(.*)", "<h2>$1</h2>");	//$NON-NLS-1$ $NON-NLS-2$ Bound a h2 heading
		cooked=cooked.replaceAll("<[h|H]3>(.*)", "<h3>$1</h3>");	//$NON-NLS-1$ $NON-NLS-2$ Bound a h3 heading
		cooked=cooked.replaceAll("<[h|H]4>(.*)", "<h4>$1</h4>");	//$NON-NLS-1$ $NON-NLS-2$ Bound a h4 heading
		cooked=cooked.replaceAll("<[n|N]1>(.*)", "<h1>$1</h1>");	//$NON-NLS-1$ $NON-NLS-2$ Another kind of heading?  Give it boundaries
		cooked=cooked.replaceAll("<[n|N]2>(.*)", "<h2>$1</h2>");	//$NON-NLS-1$ $NON-NLS-2$ Another kind of heading?  Give it boundaries
		cooked=cooked.replaceAll("<[n|N]3>(.*)", "<h3>$1</h3>");	//$NON-NLS-1$ $NON-NLS-2$ Another kind of heading?  Give it boundaries
		cooked=cooked.replaceAll("<[t|T]1>", "<p>");	//$NON-NLS-1$ $NON-NLS-2$ Tab level 1
		cooked=cooked.replaceAll("<[t|T]2>(.*)", "<p><bq1>$1</bq1>");	//$NON-NLS-1$ $NON-NLS-2$ Tab level 2
		cooked=cooked.replaceAll("<[t|T]3>(.*)", "<p><bq2>$1</bq2>");	//$NON-NLS-1$ $NON-NLS-2$ Tab level 3
		cooked=cooked.replaceAll("\\x0f", "</i>");				//$NON-NLS-1$ $NON-NLS-2$ Italics off
		cooked=cooked.replaceAll("\\x01(.*)", " <i>$1</i>");	//$NON-NLS-1$ $NON-NLS-2$ Italics on
		cooked=cooked.replaceAll("\\x02(.*)", " <i>$1</i>");	//$NON-NLS-1$ $NON-NLS-2$ Italics on
		cooked=cooked.replaceAll("~", "\"");					//$NON-NLS-1$ $NON-NLS-2$ Leading quote
		cooked=cooked.replaceAll("_", "-");						//$NON-NLS-1$ $NON-NLS-2$ 
		StringTokenizer newlines = new StringTokenizer(cooked, "\r", false);	//$NON-NLS-1$
		switch (rendering)
		{
			case RENDER_AS_HTML:
				output.println("<body><html>");
				break;
			case RENDER_AS_RTF:
				output.print("{\\rtf1"); //$NON-NLS-1$
				output.print("{\\fonttbl{\\f0\\fmodern\\fprq1;}}"); //$NON-NLS-1$
				output.print("{\\*\\generator AppleCommander "); //$NON-NLS-1$
				output.print(AppleCommander.VERSION);
				output.println(";}"); //$NON-NLS-1$
				output.print("\\f0 "); //$NON-NLS-1$
				break;
			default:
				break;
		}
		while (newlines.hasMoreTokens())
		{
			int mode = 0;
			String line = newlines.nextToken();
			StringTokenizer commands = new StringTokenizer(line,"<>",true);
			while (commands.hasMoreTokens())
			{
				String t = commands.nextToken();
				if (t.equals("<")) //$NON-NLS-1$ 
					mode = 1;
				else if (t.equals(">")) //$NON-NLS-1$ 
					mode = 0;
				else if (mode == 1) {
					if (t.startsWith("NF")) //$NON-NLS-1$ 
					{
						// do nothing... consume it
					}
					else if (t.equalsIgnoreCase("i") && (!inItalics)) //$NON-NLS-1$ 
					{
						// Italics on
						switch (rendering)
						{
							case RENDER_AS_HTML:
								output.print("<i>"); //$NON-NLS-1$ 
								break;
							case RENDER_AS_RTF:
								output.print("\\i "); //$NON-NLS-1$
								break;
							default:
								break;
						}
						inItalics = true;
					}
					else if (t.equalsIgnoreCase("/i")) //$NON-NLS-1$
					{
						// Italics off
						switch (rendering)
						{
							case RENDER_AS_HTML:
								output.print("</i>"); //$NON-NLS-1$
								break;
							case RENDER_AS_RTF:
								output.print("\\i0 "); //$NON-NLS-1$
								break;
							default:
								break;
						}
						inItalics = false;
					}
					else if (t.equalsIgnoreCase("p") || t.startsWith("j") || t.startsWith("J")) //$NON-NLS-1$ $NON-NLS-2$ $NON-NLS-3$
					{
						switch (rendering)
						{
							case RENDER_AS_HTML:
								output.print("<p>"); //$NON-NLS-1$
								break;
							case RENDER_AS_RTF:
								output.print("\\par \\par \\li0 "); //$NON-NLS-1$
								break;
							default:
								break;
						}
						ignoreBr = true;
					}
					else if (t.equalsIgnoreCase("UL") && (!inUnderline)) //$NON-NLS-1$
					{
						switch (rendering)
						{
							// Underline on
							case RENDER_AS_HTML:
								output.print("<u>"); //$NON-NLS-1$
								break;
							case RENDER_AS_RTF:
								output.print("\\ul "); //$NON-NLS-1$
								break;
							default:
								break;
						}
						inUnderline = true;
					}
					else if ((t.equalsIgnoreCase("KU") || t.equalsIgnoreCase("KL") || t.equalsIgnoreCase("UK")) && (inUnderline)) //$NON-NLS-1$ $NON-NLS-2$ $NON-NLS-3$
					{
						// Underline off
						switch (rendering)
						{
							case RENDER_AS_HTML:
								output.print("</u>"); //$NON-NLS-1$
								break;
							case RENDER_AS_RTF:
								output.print("\\ulnone "); //$NON-NLS-1$
								break;
							default:
								break;
						}
						inUnderline = false;
					}
					else if ((t.equalsIgnoreCase("BO") || t.equalsIgnoreCase("b1")) && (!inBold)) //$NON-NLS-1$ $NON-NLS-2$
					{
						// Bold on
						switch (rendering)
						{
							case RENDER_AS_HTML:
								output.print("<b>"); //$NON-NLS-1$
								break;
							case RENDER_AS_RTF:
								output.print("\\b "); //$NON-NLS-1$
								break;
							default:
								break;
						}
						inBold = true;
					}
					else if (t.equalsIgnoreCase("KB")) //$NON-NLS-1$
					{
						// Bold off
						switch (rendering)
						{
							case RENDER_AS_HTML:
								output.print("</b>"); //$NON-NLS-1$
								break;
							case RENDER_AS_RTF:
								output.print("\\b0 "); //$NON-NLS-1$
								break;
							default:
								break;
						}
						inUnderline = false;
					}
					else if ((t.equalsIgnoreCase("UFA") || t.equalsIgnoreCase("UFP") || t.equals("UFY") || t.equalsIgnoreCase("f1")) && (inSuperscript == false)) //$NON-NLS-1$ $NON-NLS-2$ $NON-NLS-3$ $NON-NLS-4$
					{
						// Superscript on
						switch (rendering)
						{
							case RENDER_AS_HTML:
								output.print("<sup>"); //$NON-NLS-1$
								break;
							case RENDER_AS_RTF:
								output.print("\\super "); //$NON-NLS-1$
								break;
							default:
								break;
						}
						inSuperscript = true;
					}
					else if ((t.equalsIgnoreCase("UFM") || t.equalsIgnoreCase("f2")) && (inSuperscript == true)) //$NON-NLS-1$ $NON-NLS-2$
					{
						// Superscript off
						switch (rendering)
						{
							case RENDER_AS_HTML:
								output.print("</sup>"); //$NON-NLS-1$
								break;
							case RENDER_AS_RTF:
								output.print("\\nosupersub "); //$NON-NLS-1$
								break;
							default:
								break;
						}
						inSuperscript = false;
					}
					else if (t.equalsIgnoreCase("co") && (inCenter == false)) //$NON-NLS-1$
					{
						switch (rendering)
						{
							case RENDER_AS_HTML:
								output.print("<center>"); //$NON-NLS-1$
								break;
							case RENDER_AS_RTF:
								output.print("\\pard\\qc "); //$NON-NLS-1$
								break;
							default:
								break;
						}
						inCenter = true;
						ignoreBr = true;
					}
					else if (t.equalsIgnoreCase("h8") && (inCenter == true)) //$NON-NLS-1$
					{
						// Center off
						switch (rendering)
						{
							case RENDER_AS_HTML:
								output.print("</center>"); //$NON-NLS-1$
								break;
							case RENDER_AS_RTF:
								output.print("\\par \\pard "); //$NON-NLS-1$
								break;
							default:
								break;
						}
						inCenter = false;
						ignoreBr = true;
					}
					else if (t.startsWith("h") && (!inHeader)) //$NON-NLS-1$
					{
						ignoreBr = true;
						inHeader = true;
						switch (rendering)
						{
							case RENDER_AS_HTML:
								if (t.equalsIgnoreCase("h1")) //$NON-NLS-1$
									output.print("<h1>"); //$NON-NLS-1$
								else if (t.equalsIgnoreCase("h2")) //$NON-NLS-1$
									output.print("<h2>"); //$NON-NLS-1$
								else if (t.equalsIgnoreCase("h3")) //$NON-NLS-1$
									output.print("<h3>"); //$NON-NLS-1$
								else if (t.equalsIgnoreCase("h4")) //$NON-NLS-1$
									output.print("<h4>"); //$NON-NLS-1$
								break;
							case RENDER_AS_RTF:
								if (t.equalsIgnoreCase("h1")) //$NON-NLS-1$
									output.print("\\par\\par\\pard\\s1\\b\\fs48 "); //$NON-NLS-1$
								else if (t.equalsIgnoreCase("h2")) //$NON-NLS-1$
									output.print("\\par\\par\\pard\\s2\\b\\fs36 "); //$NON-NLS-1$
								else if (t.equalsIgnoreCase("h3")) //$NON-NLS-1$
									output.print("\\par\\par\\pard\\s3\\b\\fs27 "); //$NON-NLS-1$
								else if (t.equalsIgnoreCase("h4")) //$NON-NLS-1$
									output.print("\\par\\par\\pard\\s4\\b\\fs24 "); //$NON-NLS-1$
								break;
							default:
								output.println();
								break;
						}
					}
					else if ((t.startsWith("/h")) && (inHeader))
					{
						ignoreBr = true;
						inHeader = false;
						switch (rendering)
						{
							case RENDER_AS_HTML:
								if (t.equalsIgnoreCase("/h1")) //$NON-NLS-1$
									output.print("</h1>"); //$NON-NLS-1$
								else if (t.equalsIgnoreCase("/h2")) //$NON-NLS-1$
									output.print("</h2>"); //$NON-NLS-1$
								else if (t.equalsIgnoreCase("/h3")) //$NON-NLS-1$
									output.print("</h3>"); //$NON-NLS-1$
								else if (t.equalsIgnoreCase("/h4")) //$NON-NLS-1$
									output.print("</h4>"); //$NON-NLS-1$
								break;
							case RENDER_AS_RTF:
								output.print("\\b0\\par\\fs24 "); //$NON-NLS-1$
								break;
							default:
								output.println();
								break;
						}
					}
					else if (t.startsWith("bq"))	// Indent $NON-NLS-1$
					{
						switch (rendering)
						{
							case RENDER_AS_HTML:
								if (t.equals("bq1")) //$NON-NLS-1$
									output.print("<blockquote>"); //$NON-NLS-1$
								else if (t.equals("bq2")) //$NON-NLS-1$
									output.print("<blockquote><blockquote>"); //$NON-NLS-1$
								break;
							case RENDER_AS_RTF:
								if (t.equals("bq1")) //$NON-NLS-1$
									output.print("\\pard\\li720 "); //$NON-NLS-1$
								else if (t.equals("bq2")) //$NON-NLS-1$
									output.print("\\pard\\li1440 "); //$NON-NLS-1$
								break;
							default:
								output.print("     "); //$NON-NLS-1$
								break;
						}
						ignoreBr = true;
					}
					else if (t.startsWith("/bq"))	// Outdent $NON-NLS-1$
					{
						switch (rendering)
						{
							case RENDER_AS_HTML:
								if (t.equals("/bq1")) //$NON-NLS-1$
									output.print("</blockquote>"); //$NON-NLS-1$
								else if (t.equals("/bq2")) //$NON-NLS-1$
									output.print("</blockquote></blockquote>"); //$NON-NLS-1$
								break;
							case RENDER_AS_RTF:
								//output.println("\\par\\li0 "); //$NON-NLS-1$
								break;
							default:
								break;
						}
						ignoreBr = true;
					}
//					else
//						System.err.println("Ignored command: <"+t+">");
				}
				else
				{
					// System.out.println("Data: ["+t+"]");
					output.print(t);
				}
			}
			if (!ignoreBr)
				handleReturn(output);
			ignoreBr = false;

			switch (rendering)
			{
				// turn off many types of formatting stuff at the end of lines
				case RENDER_AS_HTML:
					if (inItalics)
						output.print("</i>"); //$NON-NLS-1$
					if (inBold)
						output.print("</b>"); //$NON-NLS-1$
					if (inUnderline)
						output.print("</u>"); //$NON-NLS-1$
					if (inSuperscript)
						output.print("</sup>"); //$NON-NLS-1$
					break;
				case RENDER_AS_RTF:
					if (inItalics)
						output.print("\\i0 "); //$NON-NLS-1$
					if (inBold)
						output.print("\\b0 "); //$NON-NLS-1$
					if (inUnderline)
						output.print("\\ulnone "); //$NON-NLS-1$
					if (inSuperscript)
						output.print("\\nosupersub "); //$NON-NLS-1$
					break;
				default:
					break;
			}
			inItalics = false;
			inBold = false;
			inUnderline = false;
			inSuperscript = false;
			inHeader = false;
		}
		// Put the finishing touches on the document
		switch (rendering)
		{
			case RENDER_AS_HTML:
				output.println("</body></html>"); //$NON-NLS-1$
				break;
			case RENDER_AS_RTF:
				output.println("}"); //$NON-NLS-1$
				 break;
			default:
				break;
		}
		return;
	}

	/**
	 * Deal with carriage-return.
	 */
	protected void handleReturn(PrintWriter printWriter) {
		if (isHtmlRendering())
			printWriter.println("<br>"); //$NON-NLS-1$
		else if (isRtfRendering())
			printWriter.println("\\par"); //$NON-NLS-1$
		else
			printWriter.println();
	}


	/**
	 * Give suggested file name.
	 * 
	 * @see com.webcodepro.applecommander.storage.FileFilter#getSuggestedFileName(FileEntry)
	 */
	public String getSuggestedFileName(FileEntry fileEntry) {
		String fileName = fileEntry.getFilename().trim();
		String extension = ".txt"; //$NON-NLS-1$
		if (isHtmlRendering())
			extension = ".html"; //$NON-NLS-1$
		else if (isRtfRendering())
			extension = ".rtf"; //$NON-NLS-1$

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