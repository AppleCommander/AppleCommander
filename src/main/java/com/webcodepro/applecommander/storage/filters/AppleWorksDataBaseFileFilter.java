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

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.StorageBundle;
import com.webcodepro.applecommander.util.AppleUtil;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Export an AppleWorks database file to a text file.
 * Current export is limited to a standard comma-separated file (CSV)
 * file format which should be suitable to load into a database or a
 * spreadsheet.
 * <p>
 * Data base files start with a variable length header, followed by
 * 600 bytes for each report format (if any), the standard values
 * record, then variable length information for each record. Note that
 * the first data record contains the standard (presumably default)
 * values. The category entries are in the same order that the category
 * names appear in the header record.
 * <p>
 * See: http://www.gno.org/pub/apple2/doc/apple/filetypes/ftn.19.xxxx
 * <p>
 * Date Created: February 15, 2003
 * @author Rob Greene
 */
public class AppleWorksDataBaseFileFilter implements FileFilter {
	private TextBundle textBundle = StorageBundle.getInstance();
	/**
	 * The number of bytes in the remainder of the header record.
	 */
	private static final int HEADER_LENGTH_WORD = 0;
	/**
	 * Number of categories per record. Values from $01 to $1E.
	 */
	private static final int HEADER_CATEGORIES_BYTE = 35;
	/**
	 * Number of records in file. If DBMinVers is non-zero,
	 * the high bit of this word may be set.  If it is,
	 * there are more than eight reports and the remaining
	 * 15 bits contain the true number of records defined.
	 */
	private static final int HEADER_RECORDS_WORD = 36;
	/**
	 * Number of reports in a file, maximum of 8 (20 for AW 3.0).
	 */
	private static final int HEADER_REPORTS_BYTE = 38;
	/**
	 * DBMinVers. Ths minimum version of AppleWorks needed 
	 * to read this file. This will be $0 unless there are
	 * more than 8 report formats; it will then contain the
	 * version number 30 ($1E) or greater.
	 */
	private static final int HEADER_DBMINVERS_BYTE = 218;
	/**
	 * Name of category. Maximum length of 20 bytes. If this
	 * is the last category in the file, the header record will
	 * end here. This record is 22 bytes in length and will
	 * be repeated up to $1E times.
	 */
	private static final int HEADER_CATEGORY_STRING = 357;
	/**
	 * Each category name takes up a maximum of 22 bytes.
	 */
	private static final int HEADER_CATEGORY_LENGTH = 22;
	/**
	 * Each report is defined in 600 bytes. The current file
	 * filter does not deal with reports.
	 */
	private static final int REPORT_LENGTH = 600;
	/**
	 * Count of the number of bytes in the remainder of the
	 * data record.
	 */
//	private static final int DATA_LENGTH_WORD = 0;
	/**
	 * Data control record indicating that a number of
	 * categories need to be skipped. This (minus $80)
	 * is a count of the number of categories to be skipped.
	 * For example, $82 means skip two categories.
	 */
	private static final int DATA_CONTROL_SKIP = 0x80;
	/**
	 * Indicates the end of a record.
	 */
	private static final int DATA_CONTROL_END = 0xff;
	/**
	 * Indicates a date entry.
	 */
	private static final int SPECIAL_CONTROL_DATE = 0xc0;
	/**
	 * ASCII year code, like "84" ($38 $34).
	 */
	private static final int DATE_YEAR_OFFSET = 1;
	/**
	 * ASCII month code. "A" means January, "L" means
	 * December.
	 */
	private static final int DATE_MONTH_OFFSET = 3;
	/**
	 * List of months used for date conversion.
	 */
	private static final String[] months = {
		StorageBundle.getInstance().get("AppleWorksDataBaseFileFilter.January"), //$NON-NLS-1$
		StorageBundle.getInstance().get("AppleWorksDataBaseFileFilter.February"), //$NON-NLS-1$
		StorageBundle.getInstance().get("AppleWorksDataBaseFileFilter.March"), //$NON-NLS-1$
		StorageBundle.getInstance().get("AppleWorksDataBaseFileFilter.April"), //$NON-NLS-1$
		StorageBundle.getInstance().get("AppleWorksDataBaseFileFilter.May"), //$NON-NLS-1$
		StorageBundle.getInstance().get("AppleWorksDataBaseFileFilter.June"), //$NON-NLS-1$
		StorageBundle.getInstance().get("AppleWorksDataBaseFileFilter.July"), //$NON-NLS-1$
		StorageBundle.getInstance().get("AppleWorksDataBaseFileFilter.August"), //$NON-NLS-1$
		StorageBundle.getInstance().get("AppleWorksDataBaseFileFilter.September"), //$NON-NLS-1$
		StorageBundle.getInstance().get("AppleWorksDataBaseFileFilter.October"), //$NON-NLS-1$
		StorageBundle.getInstance().get("AppleWorksDataBaseFileFilter.November"), //$NON-NLS-1$
		StorageBundle.getInstance().get("AppleWorksDataBaseFileFilter.December") //$NON-NLS-1$
	};
	/**
	 * ASCII day of the month, like "31" ($33 $31).
	 */
	private static final int DATE_DAY_OFFSET = 4;
	/**
	 * The length of a date is 6 bytes.
	 */
	private static final int DATE_LENGTH = 6;
	/**
	 * Indicates a time entry.
	 */
	private static final int SPECIAL_CONTROL_TIME = 0xd4;
	/** 
	 * ASCII hour code. "A" means 00 (the hour after
	 * midnight). "X" means 23, the hour before midnight.
	 */
	private static final int TIME_HOUR_OFFSET = 1;
	/**
	 * ASCII minute code. Values from "00" to "59".
	 */
	private static final int TIME_MINUTE_OFFSET = 2;
	/**
	 * Length of a time entry is 4 bytes.
	 */
	private static final int TIME_LENGTH = 4;
	/**
	 * Create an AppleWorksDataBaseFileFilter.
	 */
	public AppleWorksDataBaseFileFilter() {
		super();
	}
	/**
	 * Process the given FileEntry and return a byte array with filtered data.
	 * @see com.webcodepro.applecommander.storage.FileFilter#filter(FileEntry)
	 */
	public byte[] filter(FileEntry fileEntry) {
		byte[] fileData = fileEntry.getFileData();
		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
		PrintWriter printWriter = new PrintWriter(byteArray, true);
		// process header information:
		int headerLength = AppleUtil.getWordValue(fileData, HEADER_LENGTH_WORD)
										+ 2;	// does not include this word!
		int categoryCount = AppleUtil.getUnsignedByte(
										fileData[HEADER_CATEGORIES_BYTE]);
		int recordCount = AppleUtil.getWordValue(fileData, HEADER_RECORDS_WORD);
		int reportCount = AppleUtil.getUnsignedByte(
										fileData[HEADER_REPORTS_BYTE]);
		int dbMinVers = AppleUtil.getUnsignedByte(
										fileData[HEADER_DBMINVERS_BYTE]);
		if (dbMinVers > 0x00 && (recordCount & 0x8000) != 0) {
			recordCount &= 0x7fff;	// adjust for "more than 8 reports" flag
		}
		int offset = HEADER_CATEGORY_STRING;
		for (int i=0; i<categoryCount; i++) {
			String name = AppleUtil.getProdosString(fileData, offset);
			if (i > 0) printWriter.print(","); //$NON-NLS-1$
			printWriter.print('"');
			printWriter.print(name);
			printWriter.print('"');
			offset+= HEADER_CATEGORY_LENGTH;
		}
		printWriter.println();
		if (offset != headerLength) {
			throw new IndexOutOfBoundsException(
				textBundle.get("AppleWorksDataBaseFileFilter.InvalidHeaderLengthError")); //$NON-NLS-1$
		}
		// skip reports:
		offset+= (reportCount * REPORT_LENGTH);
		// process data:
		for (int i=0; i<recordCount+1; i++) {
			// length = record length + 2 bytes for the length word itself.
			int length = AppleUtil.getWordValue(fileData, offset) + 2;
			int data = offset + 2;
			int column = 0;
			while (AppleUtil.getUnsignedByte(fileData[data]) != DATA_CONTROL_END) {
				if (column > 0) printWriter.print(',');
				int controlByte = AppleUtil.getUnsignedByte(fileData[data]);
				if (controlByte < DATA_CONTROL_SKIP) {
					String string = AppleUtil.getPascalString(fileData, data);
					int specialChar = AppleUtil.getUnsignedByte(fileData[data+1]);
					data+= string.length() + 1;
					switch (specialChar) {
						case SPECIAL_CONTROL_DATE:
							convertDate(printWriter, string);
							break;
						case SPECIAL_CONTROL_TIME:
							convertTime(printWriter, string);
							break;
						default:
							printWriter.print('"');
							printWriter.print(string);
							printWriter.print('"');
							break;
					}
				} else {
					int repeats = controlByte - DATA_CONTROL_SKIP;
					while (repeats > 0) {
						printWriter.print("\",\""); //$NON-NLS-1$
						repeats--;
					}
					data++; // move on to the next column
				}
				column++;
			}
			offset+= length;
			printWriter.println();
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
	 * Convert the date entry.
	 */
	protected void convertDate(PrintWriter printWriter, String date) {
		if (date.length() != DATE_LENGTH) {
			printWriter.print(textBundle.
					format("AppleWorksDataBaseFileFilter.InvalidDate", date)); //$NON-NLS-1$
		}
		
		printWriter.print((char)('0' + (date.charAt(DATE_YEAR_OFFSET) - 0x30)));
		printWriter.print((char)('0' + (date.charAt(DATE_YEAR_OFFSET + 1) - 0x30)));
		printWriter.print('-');
		printWriter.print(months[date.charAt(DATE_MONTH_OFFSET) - 'A'].substring(0,3));
		printWriter.print('-');
		printWriter.print((char)('0' + (date.charAt(DATE_DAY_OFFSET) - 0x30)));
		printWriter.print((char)('0' + (date.charAt(DATE_DAY_OFFSET + 1) - 0x30)));
	}
	/**
	 * Convert the time entry.
	 */
	protected void convertTime(PrintWriter printWriter, String time) {
		if (time.length() != TIME_LENGTH) {
			printWriter.print(textBundle.
					format("AppleWorksDataBaseFileFilter.InvalidTime", time)); //$NON-NLS-1$
		}

		printWriter.print(time.charAt(TIME_HOUR_OFFSET) - 'A');
		printWriter.print(':');
		printWriter.print((char)('0' + time.charAt(TIME_MINUTE_OFFSET) - 0x30));
		printWriter.print((char)('0' + time.charAt(TIME_MINUTE_OFFSET + 1) - 0x30));
	}
}
