/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2003 by Robert Greene
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
package com.webcodepro.applecommander.compiler;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.util.ApplesoftToken;
import com.webcodepro.applecommander.util.ApplesoftTokenizer;
import com.webcodepro.applecommander.util.ApplesoftTokens;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

/**
 * Compile the given Applesoft file.  The result will be an assembly
 * program (ultimately assembled).  This is not intended to be anything
 * sophisticated.
 * <p>
 * Date created: Nov 2, 2002 10:04:10 PM
 * @author: Rob Greene
 */
public class ApplesoftCompiler implements ApplesoftTokens {
	/**
	 * Tokenized the Applesoft program.
	 */
	private ApplesoftTokenizer tokenizer;
	/**
	 * Holds a token that was "peeked" at, if any.  A null indicates
	 * that there is no value peeked at.
	 */
	private ApplesoftToken tokenAlreadySeen;
	/**
	 * Used internally to construct the assembly representation of
	 * the Applesoft code.  This variable should really be passed
	 * between methods.
	 */
	private StringBuffer sourceAssembly = new StringBuffer();
	/**
	 * Used internally to construct the original Applesoft source
	 * line. This variable should really be passed between methods.
	 */
	private StringBuffer sourceLine = new StringBuffer();
	/**
	 * Contains a list of all known Apple ROM addresses that may be used
	 * by the compiled program.  This map is keyed by the name of the
	 * address and the value is the address.
	 */
	private Map knownAddresses = new HashMap();
	/**
	 * Lists the names of all addresses used by the compiled program.
	 * To identify the value, use the knownAddresses map.
	 */
	private List usedAddresses = new ArrayList();
	/**
	 * Contains a list of all variables declared or used by the
	 * program.
	 */
	private List variables = new ArrayList();
	/**
	 * Dynamically created map of commands to Methods.
	 */
	private Map commandMethods = new HashMap();
	/**
	 * Track FOR loop variables.
	 */
	private Stack loopVariables = new Stack();
	/**
	 * Indicates integer math operations only.
	 */
	private boolean integerOnlyMath;

	/**
	 * Constructor for ApplesoftCompiler.
	 */
	public ApplesoftCompiler(FileEntry fileEntry) {
		super();
		tokenizer = new ApplesoftTokenizer(fileEntry);
		initializeKnownAddresses();
	}
	
	/**
	 * Load known memory addresses from AppleMemoryAddresses.properties.
	 */
	protected void initializeKnownAddresses() {
		InputStream inputStream = 
			getClass().getResourceAsStream("AppleMemoryAddresses.properties");
		Properties properties = new Properties();
		try {
			properties.load(inputStream);
			Enumeration keys = properties.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				knownAddresses.put(key, properties.getProperty(key));
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * Answers true if there are more tokens to process.
	 */
	protected boolean hasMoreTokens() {
		return peekToken() != null;
	}
	
	/**
	 * Get the next token.
	 */
	protected ApplesoftToken nextToken() {
		ApplesoftToken token = tokenAlreadySeen;
		if (tokenAlreadySeen != null) {
			tokenAlreadySeen = null;
		} else {
			token = tokenizer.getNextToken();
		}
		if (token == null) {
			// No more tokens
		} else if (token.isLineNumber()) {
			sourceLine.append("* ");
			sourceLine.append(token.getLineNumber());
			sourceLine.append(" ");
		} else if (token.isToken()) {
			sourceLine.append(token.getTokenString());
		} else if (token.isString()) {
			sourceLine.append(token.getStringValue());
		}
		return token;
	}
	
	/**
	 * Take a peek at the next token.
	 */
	protected ApplesoftToken peekToken() {
		if (tokenAlreadySeen == null) {
			tokenAlreadySeen = tokenizer.getNextToken();
		}
		return tokenAlreadySeen;
	}

	/**
	 * Compile the given FileEntry and return the assembly code.
	 */
	public byte[] compile() throws CompileException {
		StringBuffer programCode = new StringBuffer();
		while (hasMoreTokens()) {
			ApplesoftToken token = nextToken();
			if (!token.isLineNumber()) {
				throw new CompileException("Expecting a line number.");
			}
			sourceAssembly.append("LINE");
			sourceAssembly.append(token.getLineNumber());
			sourceAssembly.append("\n");
			do {
				evaluateCommand();
				token = peekToken();
				if (token != null && token.isCommandSeparator()) {
					token = nextToken();
				}
			} while (token != null &&  token.isCommandSeparator());
			programCode.append(sourceLine);
			programCode.append("\n");
			programCode.append(sourceAssembly);
			programCode.append("\n");
			sourceLine.setLength(0);
			sourceAssembly.setLength(0);
		}
		programCode.insert(0, buildUsedAddresses());
		programCode.append(buildVariableSection());
		sourceLine.setLength(0);
		sourceAssembly.setLength(0);
		return programCode.toString().getBytes();
	}
	
	/**
	 * Build a list of ROM and Zero-page addresses that are used
	 * within this specific program.  
	 */
	protected StringBuffer buildUsedAddresses() {
		StringBuffer buf = new StringBuffer();
		if (usedAddresses.size() > 0) {
			buf.append("* Addresses:\n");
			for (int i=0; i<usedAddresses.size(); i++) {
				String label = (String) usedAddresses.get(i);
				buf.append(label);
				buf.append(" = ");
				buf.append((String) knownAddresses.get(label));
				buf.append("\n");
			}
			buf.append("\n");
		}
		return buf;
	}
	
	/**
	 * Build the variable section that is placed at the end of the
	 * assembly listing.
	 * <p>
	 * <b>Warning:</b> This method re-uses the global sourceAssembly
	 * variables since the addAssembly method is used to format the
	 * code.
	 */
	protected StringBuffer buildVariableSection() {
		sourceAssembly.setLength(0);
		for (int i=0; i<variables.size(); i++) {
			if (i == 0) {
				sourceAssembly.append("\n");
				sourceAssembly.append("* Variables:\n");
			}
			Variable variable = (Variable) variables.get(i);
			if (variable.isConstantInteger()) {
				addAssembly(variable.getName(), "DW", variable.getValue());
			} else if (variable.isConstantFloat()) {
				// FIXME
			} else if (variable.isConstantString()) {
				addAssembly(variable.getName(), "ASC", variable.getValue());
				addAssembly(null, "HEX", "00");
			} else if (variable.isTypeFloat()) {
				addAssembly(variable.getName(), "HEX", "8400000000");	// = 0.0
			} else if (variable.isTypeInteger()) {
				addAssembly(variable.getName(), "DS", "2");
			} else if (variable.isTypeString()) {
				// FIXME
			}
		}
		return sourceAssembly;
	}
	
	protected void evaluateCommand() {
		ApplesoftToken token = nextToken();
		while (token != null && token.isCommandSeparator()) {
			token = nextToken();
		}
		if (token == null || !token.isToken()) {
			return;	// end of line (no command on line...)
		}
		Method method = getMethod(token);
		if (method != null) {
			try {
				method.invoke(this, new Object[0]);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		} else {
			// FIXME: Just during development - this should throw an exception!
			while (peekToken() != null && !peekToken().isCommandSeparator() && !peekToken().isLineNumber()) {
				nextToken();
			}
		}
	}
	
	protected Method getMethod(ApplesoftToken token) {
		StringBuffer buf = new StringBuffer();
		buf.append("evaluate");
		buf.append(token.getTokenString().trim());
		for (int i=buf.length()-1; i>=0; i--) {
			if (buf.charAt(i) == '=') {
				buf.deleteCharAt(i);
			}
		}
		String tokenName = buf.toString();
		Method method = (Method) commandMethods.get(tokenName);
		if (method == null) {
			try {
				method = getClass().getMethod(tokenName, new Class[0]);
				commandMethods.put(tokenName, method);
			} catch (SecurityException e) {
				e.printStackTrace();
				return null;
			} catch (NoSuchMethodException e) {
				// FIXME: This is actually valid (during development anyway)
				//e.printStackTrace();
				return null;
			}
		}
		return method;
	}
	
	protected void addAssembly(String label, String mnemonic, String parameter) {
		if (label != null) {
			sourceAssembly.append(label);
//			sourceAssembly.append(":\n");
		}
		if (mnemonic != null) {
			sourceAssembly.append(" ");
			sourceAssembly.append(mnemonic);
			if (parameter != null) {
				sourceAssembly.append(" ");
				sourceAssembly.append(parameter);
				if (!usedAddresses.contains(parameter) 
				&& knownAddresses.containsKey(parameter)) {
					usedAddresses.add(parameter);
				}
			}
			sourceAssembly.append("\n");
		}
	}

	public void evaluateHOME() {
		addAssembly(null, "JSR", "HOME");
	}

	public void evaluateTEXT() {
		addAssembly(null, "JSR", "TEXT");
	}
	
	public void evaluateRETURN() {
		addAssembly(null, "RTS", null);
	}
	
	public void evaluateEND() {
		evaluateRETURN();
	}
	
	public void evaluateHGR() {
		addAssembly(null, "JSR", "HGR");
	}
	
	public void evaluateHGR2() {
		addAssembly(null, "JSR", "HGR2");
	}
	
	public void evaluateGR() {
		addAssembly(null, "JSR", "GR");
	}
	
	public void evaluateINVERSE() {
		addAssembly(null, "LDA", "#$3F");
		addAssembly(null, "STA", "INVFLAG");
	}
	
	public void evaluateNORMAL() {
		addAssembly(null, "LDA", "#$FF");
		addAssembly(null, "STA", "INVFLAG");
	}
	
	public void evaluateFLASH() {
		addAssembly(null, "LDA", "#$7F");
		addAssembly(null, "STA", "INVFLAG");
	}
	
	protected Variable evaluateExpression() throws CompileException {
		// FIXME: no type checking available
		// FIXME: needs to evaluate all valid expressions
		ApplesoftToken token = nextToken();
		if (token.isString()) {
			String value = token.getStringValue();
			Variable variable = null;
			for (int i=0; i<variables.size(); i++) {
				variable = (Variable) variables.get(i);
				if (value.equals(variable.getValue())) {
					break;
				}
				variable = null;
			}
			if (variable == null) {
				if (isIntegerNumber(value)) {
					variable = new Variable("INT" + value, Variable.CONST_INTEGER, value);
				} else if (value.startsWith("\"")) {
					variable = new Variable("STR" + variables.size(), Variable.CONST_STRING, value);
				} else {	// assume variable name
					if (value.endsWith("$")) {
						variable = new Variable("VAR" + value, Variable.TYPE_STRING, value);
					} else if (value.endsWith("%") || isIntegerOnlyMath()) {
						variable = new Variable("VAR" + value, Variable.TYPE_INTEGER, value);
					} else {
						variable = new Variable("VAR" + value, Variable.TYPE_FLOAT, value);
					}
				}
				variables.add(variable);
			}
			return variable; 
		} else {
			throw new CompileException("Unable to evaluate expression!");
		}
	}
	
	protected Variable evaluateNumber() throws CompileException {
		Variable variable = evaluateExpression();
		if (variable.isNumber()) {
			return variable;
		}
		throw new CompileException("A number is required.");
	}
	
	/**
	 * Answer with the line number label.  Used by GOTO and ON expr GOTO
	 * statements.
	 */
	protected String getLineNumberLabel() throws CompileException {
		ApplesoftToken token = nextToken();
		if (token.isString() && isIntegerNumber(token.getStringValue())) {
			return "LINE" + token.getStringValue();
		}
		throw new CompileException("Expecting a line number but found " 
			+ token.toString());
	}
	
	protected void addLoadByteValue(Variable variable, char register) throws CompileException {
		if (variable.isConstantInteger()) {
			addAssembly(null, "LD" + register, "#" + variable.getValue());
		} else if(variable.isTypeInteger()) {
			addAssembly(null, "LD" + register, variable.getName());
		} else if (variable.isConstantFloat() || variable.isTypeFloat()) {
			addAssembly(null, "LDY", "#>" + variable.getName());
			addAssembly(null, "LDA", "#<" + variable.getName());
			addAssembly(null, "JSR","MOVFM");
			addAssembly(null, "JSR", "QINT");
			addAssembly(null, "LD" + register, "FACLO");
		} else {
			throw new CompileException("Unable to convert to a byte value: "
				+ variable.getName());
		}
	}

	protected void addLoadWordValue(Variable variable, char registerHi, char registerLo) throws CompileException {
		if (variable.isConstantInteger()) {
			addAssembly(null, "LD" + registerHi, "#>" + variable.getValue());
			addAssembly(null, "LD" + registerLo, "#<" + variable.getValue());
		} else if (variable.isTypeInteger()) {
			addAssembly(null, "LD" + registerHi, variable.getName() + "+1");
			addAssembly(null, "LD" + registerLo, variable.getName());
		} else if (variable.isConstantFloat() || variable.isTypeFloat()) {
			addLoadFac(variable);
			addAssembly(null, "JSR", "QINT");
			addAssembly(null, "LD" + registerHi, "FACMO");
			addAssembly(null, "LD" + registerLo, "FACLO");
		} else {
			throw new CompileException("Unable to convert to a word value: "
				+ variable.getName());
		}
	}

	protected void addLoadAddress(Variable variable, char registerHi, char registerLo) {
		addAssembly(null, "LD" + registerHi, "#>" + variable.getName());
		addAssembly(null, "LD" + registerLo, "#<" + variable.getName());
	}
	
	protected void addLoadFac(Variable variable) throws CompileException {
		if (variable.isConstantFloat() || variable.isTypeFloat()) {
			addLoadAddress(variable, 'Y', 'A');
			addAssembly(null, "JSR","MOVFM");
		} else if (variable.isConstantInteger() || variable.isTypeInteger()) {
			addLoadWordValue(variable, 'A', 'Y');
			addAssembly(null, "JSR", "GIVAYF");
		} else {
			throw new CompileException("Unable to convert to load FAC for: "
				+ variable.getName());
		}
	}
	
	protected void addCopyFac(Variable variable) throws CompileException {
		if (variable.isTypeFloat()) {
			addLoadAddress(variable, 'Y', 'X');
			addAssembly(null, "JSR", "MOVMF");
		} else {
			throw new CompileException(
				"Internal Error: Can only copy floats to numeric variables.");
		}
	}
	
	public void evaluateHTAB() throws CompileException {
		addLoadByteValue(evaluateExpression(), 'A');
		addAssembly(null, "STA", "CH");
	}

	public void evaluateVTAB() throws CompileException {
		addLoadByteValue(evaluateExpression(), 'X');
		addAssembly(null, "DEX", null);
		addAssembly(null, "STX", "CV");
		addAssembly(null, "JSR", "LF");
	}

	public void evaluateHCOLOR() throws CompileException {
		addLoadByteValue(evaluateExpression(), 'X');
		addAssembly(null, "JSR", "SETHCOL");
	}
	
	public void evaluatePRINT() throws CompileException {
		ApplesoftToken token = null;
		do {
			Variable variable = evaluateExpression();
			if (variable.isConstantFloat() || variable.isTypeFloat()) {
				addLoadFac(variable);
				addAssembly(null, "JSR", "PRNTFAC");
			} else if (variable.isConstantInteger() || variable.isTypeInteger()) {
				addLoadWordValue(variable, 'X', 'A');
				addAssembly(null, "JSR", "LINPRT");
			} else if (variable.isConstantString()) {
				addLoadAddress(variable, 'Y', 'A');
				addAssembly(null, "JSR", "STROUT");
			} else if (variable.isTypeString()) {
				throw new CompileException("Unable to print string variables yet.");
			}
			// check if we have separators to skip over...
			token = peekToken();
			if (token != null && token.isExpressionSeparator()) {
				nextToken();
			}
			// see if we should continue processing the print statement
			token = peekToken();
		} while (token != null && !token.isEndOfCommand());
	}
	
	public void evaluateGOTO() throws CompileException {
		addAssembly(null, "JMP", getLineNumberLabel());
	}
	
	protected void checkSyntax(byte tokenValue, String expectedToken) throws CompileException {
		ApplesoftToken token = nextToken();
		if (token.getTokenValue() != tokenValue) {
			// FIXME: Should this read token from master token list?
			throw new CompileException("Syntax Error: Expecting " + expectedToken);
		}
	}

	protected void checkSyntax(String stringValue, String expectedToken) throws CompileException {
		ApplesoftToken token = nextToken();
		if (!stringValue.equals(token.getStringValue())) {
			// FIXME: Should this read token from master token list?
			throw new CompileException("Syntax Error: Expecting " + expectedToken);
		}
	}
	
	public void evaluateFOR() throws CompileException {
		Variable loopVariable = evaluateExpression();
		if (!loopVariable.isTypeFloat() && !loopVariable.isTypeInteger()) {
			throw new CompileException(
				"Applesoft only allows floating-point FOR variables.");
		}
		checkSyntax(EQUALS, "=");
		Variable startValue = evaluateNumber();
		checkSyntax(TO, "TO");
		Variable endValue = evaluateNumber();
		// FIXME: Need to handle STEP
		String loopName = "FOR" + loopVariables.size();
		loopVariables.add(loopName);
		addLoadFac(startValue);
		addCopyFac(loopVariable);
		addAssembly(loopName, null, null);
		addLoadFac(endValue);
		addLoadAddress(loopVariable, 'Y', 'A');
		addAssembly(null, "JSR", "FCOMP");
		addAssembly(null, "CMP", "#$FF");	// loopVariable > endValue
		addAssembly(null, "BEQ", "END" + loopName);
	}
	
	public void evaluateHPLOT() throws CompileException {
		boolean firstCoordinate = true;
		while (peekToken() != null && !peekToken().isEndOfCommand()) {
			if (!firstCoordinate) {
				checkSyntax(TO, "TO");
			}
			Variable coordX = evaluateNumber();
			checkSyntax(",", ", (comma)");
			Variable coordY = evaluateNumber();
			if (firstCoordinate) {
				addLoadWordValue(coordX, 'Y', 'X');
				addLoadByteValue(coordY, 'A');
				addAssembly(null, "JSR", "HPOSN");
				firstCoordinate = false;
			} else {
				addLoadWordValue(coordX, 'X', 'A');
				addLoadByteValue(coordY, 'Y');
				addAssembly(null, "JSR", "HLIN");
			}
		}
	}
	
	public void evaluateNEXT() throws CompileException {
		// FIXME: Next requires variable name given...
		Variable variable = null;
		if (!peekToken().isCommandSeparator()) {
			// FIXME: This does not ensure that we only have a variable!
			variable = evaluateExpression();
		}
		if (variable.isTypeFloat()) {
			addAssembly(null, "LDY", "#1");
			addAssembly(null, "JSR", "SNGFLT");
			addLoadAddress(variable, 'Y', 'A');
			addAssembly(null, "JSR", "FADD");
			addCopyFac(variable);
		} else if (variable.isTypeInteger()) {
			addAssembly(null, "INC", variable.getName());
			addAssembly(null, "BNE", ":1");
			addAssembly(null, "INC", variable.getName() + "+1");
			addAssembly(":1", null, null);
		}
		String loopName = (String) loopVariables.pop();
		addAssembly(null, "JMP", loopName);
		addAssembly("END" + loopName, null, null);
	}

	/**
	 * Indicates if this string is a number.
	 */
	protected boolean isIntegerNumber(String value) {
		for (int i=0; i<value.length(); i++) {
			if (!Character.isDigit(value.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Indicates integer math only. 
	 */
	public boolean isIntegerOnlyMath() {
		return integerOnlyMath;
	}
	
	/**
	 * Sets integer only math.
	 */
	public void setIntegerOnlyMath(boolean integerOnlyMath) {
		this.integerOnlyMath = integerOnlyMath;
	}
}
