/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2003 by Robert Greene
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
import java.text.NumberFormat;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.util.AppleUtil;

/**
 * Export an AppleWorks SpreadSheet file. 
 * The spread-sheet file starts with a 300 byte header, followed by
 * multiple cells.
 * <p>
 * See: http://www.gno.org/pub/apple2/doc/apple/filetypes/ftn.1b.xxxx
 * <p>
 * Date Created: Feb 23, 2003
 * @author Rob Greene
 */
public class AppleWorksSpreadSheetFileFilter implements FileFilter {
	/**
	 * SSMinVers. Ths minimum version of AppleWorks needed 
	 * to read this file. If this file contains version 3.0-
	 * specific functions (such as calculated labels or new
	 * functions), this byte will contain the version number
	 * 30 ($1E).  Otherwise it will be zero ($00).
	 */
	private static final int HEADER_SSMINVERS_BYTE = 242;
	/**
	 * Value Constant mask bytes +000 and +001.
	 */
	private static final int CELL_VALUE_CONSTANT_MASK = 0xa0e0;
	/**
	 * Value Constant id byte +000 and +001.
	 */
	private static final int CELL_VALUE_CONSTANT_ID = 0xa000;
	/**
	 * Value Label mask bytes +000 and +001.
	 */
	private static final int CELL_VALUE_LABEL_MASK = 0xa088;
	/**
	 * Value Label id bytes +000 and +001.
	 */
	private static final int CELL_VALUE_LABEL_ID = 0x8088;
	/**
	 * Value Formula mask bytes +000 and +001.
	 */
	private static final int CELL_VALUE_FORMULA_MASK = 0xa080;
	/**
	 * Value Formula id bytes +000 and +001.
	 */
	private static final int CELL_VALUE_FORMULA_ID = 0x8080;
	/**
	 * Propagated Value Cells mask byte +000.
	 */
	private static final int CELL_PROPAGATED_VALUE_MASK = 0xa0;
	/**
	 * Propagated Value Cells id byte +000.
	 */
	private static final int CELL_PROPAGATED_VALUE_ID = 0x20;
	/**
	 * "Regular" Label Cell mask byte +000.
	 */
	private static final int CELL_LABEL_VALUE_MASK = 0xe0;
	/**
	 * "Regular" Label Cell id byte +000.
	 */
	private static final int CELL_LABEL_VALUE_ID = 0x00;
	/**
	 * This is the value that the formulas begin at.
	 */
	private static final int FORMULA_OFFSET = 0xc0;
	/**
	 * These are the formulas starting at FORMULA_OFFSET.
	 */
	private static final String[] formulaText = {
		"@Deg", "@Rad", "@Pi", "@True", "@False", "@Not", "@IsBlank",  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
		"@IsNA", "@IsError", "@Exp", "@Ln", "@Log", "@Cos", "@Sin", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
		"@Tan", "@ACos", "@ASin", "@ATan2", "@ATan", "@Mod", "@FV",  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
		"@PV", "@PMT", "@Term", "@Rate", "@Round", "@Or", "@And",  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
		"@Sum", "@Avg", "@Choose", "@Count", "@Error", "@IRR", "@If", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
		"@Int", "@Lookup", "@Max", "@Min", "@NA", "@NPV", "@Sqrt", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
		"@Abs", null, "<>", ">=", "<=", "=", ">", "<", ",", "^", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
		")", "-", "+", "/", "*", "(", "-", "+", "..", null, null, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
		null 
	};
	/**
	 * The @Error formula requires 3 trailing zero bytes.
	 */
	private static final int FORMULA_ERROR_CODE = 0xe0;
	/**
	 * The @NA formula requires 3 trailing zero bytes.
	 */
	private static final int FORMULA_NA_CODE = 0xe7;
	/**
	 * The $FD formula control byte indicates the next 8 bytes
	 * are a SANE number.
	 */
	private static final int FORMULA_SANE_CODE = 0xfd;
	/**
	 * The $FE formula control byte indicates that the next
	 * 3 bytes indicate row, column reference.
	 */
	private static final int FORMULA_ROW_COLUMN_CODE = 0xfe;
	/**
	 * The $FF formula control byte indicates that a Pascal
	 * string follows.  The next byte is the length of the string.
	 */
	private static final int FORMULA_STRING_CODE = 0xff;
	/**
	 * Process the given FileEntry and return a byte array with filtered data.
	 * @see com.webcodepro.applecommander.storage.FileFilter#filter(FileEntry)
	 */
	public byte[] filter(FileEntry fileEntry) {
		byte[] fileData = fileEntry.getFileData();
		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
		PrintWriter printWriter = new PrintWriter(byteArray, true);
		boolean isVersion30 = (fileData[HEADER_SSMINVERS_BYTE] == 30);
		int offset = 300 + (isVersion30 ? 2 : 0);
		int rowLength = AppleUtil.getWordValue(fileData, offset);
		while (rowLength != 0xffff) {
			int rowNumber = AppleUtil.getWordValue(fileData, offset+2);
			offset+= 4;
			processRow(printWriter, fileData, offset, rowNumber);
			offset+= rowLength - 2;
			rowLength = AppleUtil.getWordValue(fileData, offset);
		}
		// return CSV file:
		return byteArray.toByteArray();
	}
	/**
	 * Give suggested file name.
	 * @see com.webcodepro.applecommander.storage.FileFilter#getSuggestedFileName(FileEntry)
	 */
	public String getSuggestedFileName(FileEntry fileEntry) {
		return fileEntry.getFilename() + ".csv"; //$NON-NLS-1$
	}
	/**
	 * Process an entire row.
	 */
	public void processRow(PrintWriter printWriter, byte[] fileData, int offset, int rowNumber) {
		int column = 0;
		while (true) {
			int rowControl = AppleUtil.getUnsignedByte(fileData[offset]);
			if (rowControl <= 0x7f) {			// process row
				if (column > 0) printWriter.print(","); //$NON-NLS-1$
				processCell(printWriter, fileData, offset+1, rowControl, 
					rowNumber, column);
				offset+= rowControl;
			} else if (rowControl < 0xff) {	// skip rows
				if (column > 0) printWriter.print(","); //$NON-NLS-1$
				int columns = rowControl - 0x80;
				skipColumns(column, printWriter, columns);
				column+= columns;
			} else {							// end of row ($FF)
				printWriter.println();
				break;
			}
			offset++;
			column++;
		}
	}
	/**
	 * Skip the given number of columns.
	 */
	protected void skipColumns(int column, PrintWriter printWriter, int columns) {
		while (columns > 0) {
			if (column > 0) printWriter.print(","); //$NON-NLS-1$
			printWriter.print("\",\""); //$NON-NLS-1$
			columns--;
			column++;
		}
	}
	/**
	 * Process an individual cell.
	 */
	protected void processCell(PrintWriter printWriter, byte[] fileData, 
			int offset, int length, int currentRow, int currentColumn) {
		int byte0 = AppleUtil.getUnsignedByte(fileData[offset]);
		int byte1 = AppleUtil.getUnsignedByte(fileData[offset+1]);
		int cellFlag = (byte0 << 8) + byte1;
		if ((cellFlag & CELL_VALUE_CONSTANT_MASK) == CELL_VALUE_CONSTANT_ID) {
			double value = AppleUtil.getSaneNumber(fileData, offset+2);
			printWriter.print(value);
		} else if ((cellFlag & CELL_VALUE_LABEL_MASK) == CELL_VALUE_LABEL_ID) {
			// This is AW 3.0 or later, skipping until an example is found.
		} else if ((cellFlag & CELL_VALUE_FORMULA_MASK) == CELL_VALUE_FORMULA_ID) {
			int i = 10;
			printWriter.print('"');
			while (i < length) {
				int controlByte = AppleUtil.getUnsignedByte(fileData[offset+i]);
				i++;
				switch (controlByte) {
					case FORMULA_ERROR_CODE:
						i+= 3;	// skip 3 zero bytes
						break;
					case FORMULA_NA_CODE:
						i+= 3;	// skip 3 zero bytes
						break;
					case FORMULA_SANE_CODE:
						double value = AppleUtil.getSaneNumber(fileData, offset+i);
						printWriter.print(value);
						i+= 8;	// skip past SANE number
						break;
					case FORMULA_ROW_COLUMN_CODE:
						int column = fileData[offset+i];
						int row = AppleUtil.getSignedWordValue(fileData, offset+i+1);
						printWriter.print(getColumnReference(currentColumn + column));
						printWriter.print(getRowReference(currentRow + row));
						i+= 3;	// skip past row/column reference
						break;
					case FORMULA_STRING_CODE:
						String string = AppleUtil.getPascalString(fileData, offset+i);
						printWriter.print(string);
						i+= string.length() + 1;	// skip past string
						break;
					default:
						printWriter.print(formulaText[controlByte - FORMULA_OFFSET]);
						break;
				}
			}
			printWriter.print('"');
		} else if ((byte0 & CELL_PROPAGATED_VALUE_MASK) == CELL_PROPAGATED_VALUE_ID) {
			char ch = (char) AppleUtil.getUnsignedByte(fileData[offset+1]);
			printWriter.print('"');
			for (int i=0; i<8; i++) {	// 8 is an arbitrary cell width
				printWriter.print(ch);
			}
			printWriter.print('"');
		} else if ((byte0 & CELL_LABEL_VALUE_MASK) == CELL_LABEL_VALUE_ID) {
			String string = AppleUtil.getString(fileData, offset+1, length-1);
			printWriter.print('"');
			printWriter.print(string);
			printWriter.print('"');
		} else {
			printWriter.print("\"Unknown Cell Contents!\""); //$NON-NLS-1$
		}
	}
	/**
	 * Build a column reference (convert to A or whatever it should be).
	 */
	protected String getColumnReference(int column) {
		int pos1 = column / 26;
		int pos2 = column % 26;
		StringBuffer buf = new StringBuffer();
		if (pos1 > 0) {
			buf.append((char)('@' + pos1));
		}
		buf.append((char)('@' + pos2));
		return buf.toString();
	}
	/**
	 * Build a row reference.
	 */
	protected String getRowReference(int row) {
		NumberFormat formatter = NumberFormat.getInstance();
		return formatter.format(row);
	}
}
