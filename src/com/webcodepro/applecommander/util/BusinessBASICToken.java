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