package com.webcodepro.applecommander.util;

/**
 * Represents an ApplesoftToken.
 * @see com.webcodepro.applecommander.util.ApplesoftTokenizer 
 * @author Rob
 */
public class ApplesoftToken {
	private int lineNumber;
	private byte tokenValue;
	private String tokenString;
	private String stringValue;
	
	public ApplesoftToken(int lineNumber) {
		this.lineNumber = lineNumber;
	}
	
	public ApplesoftToken(byte tokenValue, String tokenString) {
		this.tokenValue = tokenValue;
		this.tokenString = tokenString;
	}
	
	public ApplesoftToken(String stringValue) {
		this.stringValue = stringValue;
	}
	
	public boolean isLineNumber() {
		return !isToken() && !isString();
	}
	
	public boolean isToken() {
		return tokenString != null;
	}
	
	public boolean isString() {
		return stringValue != null;
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

}
