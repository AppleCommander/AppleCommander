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

/**
 * Compile the given file as an Applesoft file.
 * <p>
 * Date created: Nov 2, 2002 10:04:10 PM
 * @author: Rob Greene
 */
public class ApplesoftCompiler implements ApplesoftTokens {
	private ApplesoftTokenizer tokenizer;
	private ApplesoftToken tokenAlreadySeen;
	private StringBuffer sourceAssembly = new StringBuffer();
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
			sourceAssembly.append(":\n");
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
				addAssembly(variable.getName(), "DS", "5");
			} else if (variable.isTypeInteger()) {
				addAssembly(variable.getName(), "DS", "2");
			} else if (variable.isTypeString()) {
				// FIXME
			}
		}
		programCode.append(sourceAssembly);
		sourceLine.setLength(0);
		sourceAssembly.setLength(0);
		return programCode.toString().getBytes();
	}
	
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
				// This is actually valid (during development anyway)
				//e.printStackTrace();
				return null;
			}
		}
		return method;
	}
	
	protected void addAssembly(String label, String mnemonic, String parameter) {
		if (label != null) {
			sourceAssembly.append(label);
			sourceAssembly.append(":\n");
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
					if (value.endsWith("%")) {
						variable = new Variable("VAR" + value, Variable.TYPE_INTEGER, value);
					} else if (value.endsWith("$")) {
						variable = new Variable("VAR" + value, Variable.TYPE_STRING, value);
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
			addAssembly(null, "LD" + registerHi, "#>" + variable.getName());
			addAssembly(null, "LD" + registerLo, "#<" + variable.getName());
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
			addAssembly(null, "LDY", "#>" + variable.getName());
			addAssembly(null, "LDA", "#<" + variable.getName());
			addAssembly(null, "JSR","MOVFM");
		} else {
			throw new CompileException("Unable to convert to load FAC for: "
				+ variable.getName());
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


//	/**
//	 * Process and individual token.
//	 */
//	protected void processToken(StringBuffer sourceAssembly, 
//	StringBuffer sourceLine, ApplesoftToken token, ApplesoftTokenizer tokenizer) {
//		String expr = null;
//		switch (token.getTokenValue()) {
//			case FOR:
//						String loopVariable = evaluateExpression(sourceAssembly, sourceLine);
//						if (!isFloatVariable(loopVariable)) {
//							throw new IllegalArgumentException("FOR loop argument must be a float");
//						}
//						token = tokenizer.getNextToken();
//						if (token.getTokenValue() != EQUALS) {
//							throw new IllegalArgumentException("FOR requires =");
//						}
//						sourceLine.append(token.getTokenString());
//						String startValue = evaluateExpression(sourceAssembly, sourceLine);
//						token = tokenizer.getNextToken();
//						if (token.getTokenValue() != TO) {
//							throw new IllegalArgumentException("FOR requires TO");
//						}
//						sourceLine.append(token.getTokenString());
//						String endValue = evaluateExpression(sourceAssembly, sourceLine);
//						if (isFloatVariable(loopVariable)) {
//							// FIXME: Assumes start/end are integer
//							sourceAssembly.append("\tLDY ");
//							sourceAssembly.append(startValue);
//							sourceAssembly.append("\n");
//							sourceAssembly.append("\tLDA ");
//							sourceAssembly.append(startValue);
//							sourceAssembly.append("\n");
//							sourceAssembly.append("\tJSR $E2F2 ; GIVAYF\n");
//							sourceAssembly.append("\tLDY #>");
//							sourceAssembly.append(loopVariable);
//							sourceAssembly.append("\n");
//							sourceAssembly.append("\tLDX #<");
//							sourceAssembly.append(loopVariable);
//							sourceAssembly.append("\n");
//							sourceAssembly.append("\tJSR $EB2B ; MOVMF\n");
//						}
//						break;
//		}
//	}
//	
//	
//	protected String addIntegerConstant(String value) {
//		String name = "INT" + value;
//		if (!integerConstants.containsKey(name)) {
//			integerConstants.put(name, value);
//		}
//		return name;
//	}
//
//	protected String addStringConstant(String value) {
//		String name = "STR" + stringConstants.size();
//		if (stringConstants.containsValue(value)) {
//			Iterator iterator = stringConstants.keySet().iterator();
//			while (iterator.hasNext()) {
//				String key = (String) iterator.next();
//				String keyValue = (String) stringConstants.get(key);
//				if (value.equals(keyValue)) {
//					name = key;
//					break;
//				}
//			}
//		} else {
//			stringConstants.put(name, value);
//		}
//		return name;
//	}
//	
//	protected String addVariable(String variableName) {
//		if (variableName.endsWith("$")) {
//			variableName = "STR" + variableName;
//			if (!stringVariables.contains(variableName)) {
//				stringVariables.add(variableName);
//			}
//		} else if (variableName.endsWith("%")) {
//			variableName = "INT" + variableName;
//			if (!integerVariables.contains(variableName)) {
//				integerVariables.add(variableName);
//			}
//		} else {
//			variableName = "FP" + variableName;
//			if (!floatVariables.contains(variableName)) {
//				floatVariables.add(variableName);
//			}
//		}
//		return variableName;
//	}
}
