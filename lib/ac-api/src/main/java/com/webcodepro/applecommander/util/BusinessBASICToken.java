/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2008-2022 by David Schmidt
 * david__schmidt at users.sourceforge.net
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

/**
 * Represents an Apple /// Business BASIC Token.
 * @see com.webcodepro.applecommander.util.ApplesoftTokenizer 
 * @author David Schmidt
 */
public class BusinessBASICToken {
	private int lineNumber;
	private byte tokenValue;
	private String tokenString;
	private String stringValue;

	public BusinessBASICToken(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public BusinessBASICToken(byte tokenValue, String tokenString) {
		this.tokenValue = tokenValue;
		this.tokenString = tokenString;
	}

	public BusinessBASICToken(String stringValue) {
		this.stringValue = stringValue;
	}

	public boolean isCommandSeparator() {
		return ":".equals(stringValue); //$NON-NLS-1$
	}

	public boolean isLineNumber() {
		return !isToken() && !isString();
	}

	public boolean isEndOfCommand() {
		return isLineNumber() || isCommandSeparator();
	}

	public boolean isToken() {
		return tokenString != null;
	}

	public boolean isString() {
		return stringValue != null;
	}

	public boolean isExpressionSeparator() {
		return isCommandSeparator()
			|| ",".equals(stringValue) //$NON-NLS-1$
			|| ";".equals(stringValue); //$NON-NLS-1$
	}

	public boolean isIndenter() {
		return isToken() && tokenString.equals("FOR"); //$NON-NLS-1$
	}

	public boolean isOutdenter() {
		return isToken() && tokenString.equals("NEXT"); //$NON-NLS-1$
	}

	/**
	 * Get the line number.
	 */
	public int getLineNumber() {
		return lineNumber;
	}

	/**
	 * Get the string value.
	 */
	public String getStringValue() {
		return stringValue;
	}

	/**
	 * Get the token.
	 */
	public String getTokenString() {
		return tokenString;
	}

	/**
	 * Get the token.
	 */
	public byte getTokenValue() {
		return tokenValue;
	}

	/**
	 * Render the token as a useful String.
	 */
	public String toString() {
		if (isLineNumber()) {
			return Integer.toString(getLineNumber());
		} else if (isToken()) {
			return getTokenString() + " " + Integer.toHexString(getTokenValue()); //$NON-NLS-1$
		} else {
			return getStringValue();
		}
	}
}