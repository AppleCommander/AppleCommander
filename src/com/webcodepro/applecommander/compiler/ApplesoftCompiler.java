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

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.util.ApplesoftToken;
import com.webcodepro.applecommander.util.ApplesoftTokenizer;
import com.webcodepro.applecommander.util.ApplesoftTokens;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Compile the given Applesoft file.  The result will be an assembly
 * program (ultimately assembled).  This is not intended to be anything
 * sophisticated.
 * <p>
 * Date created: Nov 2, 2002 10:04:10 PM
 * @author Rob Greene
 */
public class ApplesoftCompiler implements ApplesoftTokens {
	private TextBundle textBundle = CompilerBundle.getInstance();
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
			getClass().getResourceAsStream("AppleMemoryAddresses.properties"); //$NON-NLS-1$
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
			sourceLine.append("* "); //$NON-NLS-1$
			sourceLine.append(token.getLineNumber());
			sourceLine.append(" "); //$NON-NLS-1$
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
				throw new CompileException(textBundle.
						get("ApplesoftCompiler.ExpectLineNumberError")); //$NON-NLS-1$
			}
			sourceAssembly.append("LINE"); //$NON-NLS-1$
			sourceAssembly.append(token.getLineNumber());
			sourceAssembly.append("\n"); //$NON-NLS-1$
			do {
				evaluateCommand();
				token = peekToken();
				if (token != null && token.isCommandSeparator()) {
					token = nextToken();
				}
			} while (token != null &&  token.isCommandSeparator());
			programCode.append(sourceLine);
			programCode.append("\n"); //$NON-NLS-1$
			programCode.append(sourceAssembly);
			programCode.append("\n"); //$NON-NLS-1$
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
			buf.append("* Addresses:\n"); //$NON-NLS-1$
			for (int i=0; i<usedAddresses.size(); i++) {
				String label = (String) usedAddresses.get(i);
				buf.append(label);
				buf.append(" = "); //$NON-NLS-1$
				buf.append((String) knownAddresses.get(label));
				buf.append("\n"); //$NON-NLS-1$
			}
			buf.append("\n"); //$NON-NLS-1$
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
				sourceAssembly.append("\n"); //$NON-NLS-1$
				sourceAssembly.append("* Variables:\n"); //$NON-NLS-1$
			}
			Variable variable = (Variable) variables.get(i);
			if (variable.isConstantInteger()) {
				addAssembly(variable.getName(), "DW", variable.getValue()); //$NON-NLS-1$
			} else if (variable.isConstantFloat()) {
				// FIXME
			} else if (variable.isConstantString()) {
				addAssembly(variable.getName(), "ASC", variable.getValue()); //$NON-NLS-1$
				addAssembly(null, "HEX", "00"); //$NON-NLS-1$ //$NON-NLS-2$
			} else if (variable.isTypeFloat()) {
				addAssembly(variable.getName(), "HEX", "8400000000");	// = 0.0 //$NON-NLS-1$ //$NON-NLS-2$
			} else if (variable.isTypeInteger()) {
				addAssembly(variable.getName(), "DS", "2"); //$NON-NLS-1$ //$NON-NLS-2$
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
				System.err.println(textBundle.
						format("ApplesoftCompiler.UnableToLocateError", //$NON-NLS-1$
								method.getName()));
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				System.err.println(textBundle.
						format("ApplesoftCompiler.UnableToLocateError", //$NON-NLS-1$
								method.getName()));
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				System.err.println(textBundle.
						format("ApplesoftCompiler.UnableToLocateError", //$NON-NLS-1$
								method.getName()));
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
		buf.append("evaluate"); //$NON-NLS-1$
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
			sourceAssembly.append(" "); //$NON-NLS-1$
			sourceAssembly.append(mnemonic);
			if (parameter != null) {
				sourceAssembly.append(" "); //$NON-NLS-1$
				sourceAssembly.append(parameter);
				if (!usedAddresses.contains(parameter) 
				&& knownAddresses.containsKey(parameter)) {
					usedAddresses.add(parameter);
				}
			}
			sourceAssembly.append("\n"); //$NON-NLS-1$
		}
	}

	public void evaluateHOME() {
		addAssembly(null, "JSR", "HOME"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void evaluateTEXT() {
		addAssembly(null, "JSR", "TEXT"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void evaluateRETURN() {
		addAssembly(null, "RTS", null); //$NON-NLS-1$
	}
	
	public void evaluateEND() {
		evaluateRETURN();
	}
	
	public void evaluateHGR() {
		addAssembly(null, "JSR", "HGR"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void evaluateHGR2() {
		addAssembly(null, "JSR", "HGR2"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void evaluateGR() {
		addAssembly(null, "JSR", "GR"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void evaluateINVERSE() {
		addAssembly(null, "LDA", "#$3F"); //$NON-NLS-1$ //$NON-NLS-2$
		addAssembly(null, "STA", "INVFLAG"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void evaluateNORMAL() {
		addAssembly(null, "LDA", "#$FF"); //$NON-NLS-1$ //$NON-NLS-2$
		addAssembly(null, "STA", "INVFLAG"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void evaluateFLASH() {
		addAssembly(null, "LDA", "#$7F"); //$NON-NLS-1$ //$NON-NLS-2$
		addAssembly(null, "STA", "INVFLAG"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	protected Variable evaluateExpression() throws CompileException {
		// FIXME: no type checking available
		// FIXME: needs to evaluate all valid expressions
		ApplesoftToken token = peekToken();
		if (token.isEndOfCommand()) {
			return null;
		}
		token = nextToken();
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
					variable = new Variable("INT" + value, Variable.CONST_INTEGER, value); //$NON-NLS-1$
				} else if (value.startsWith("\"")) { //$NON-NLS-1$
					variable = new Variable("STR" + variables.size(), Variable.CONST_STRING, value); //$NON-NLS-1$
				} else {	// assume variable name
					if (value.endsWith("$")) { //$NON-NLS-1$
						variable = new Variable("VAR" + value, Variable.TYPE_STRING, value); //$NON-NLS-1$
					} else if (value.endsWith("%") || isIntegerOnlyMath()) { //$NON-NLS-1$
						variable = new Variable("VAR" + value, Variable.TYPE_INTEGER, value); //$NON-NLS-1$
					} else {
						variable = new Variable("VAR" + value, Variable.TYPE_FLOAT, value); //$NON-NLS-1$
					}
				}
				variables.add(variable);
			}
			return variable; 
		}
		throw new CompileException(textBundle.get("ApplesoftCompiler.UnableToEvaluateError")); //$NON-NLS-1$
	}
	
	protected Variable evaluateNumber() throws CompileException {
		Variable variable = evaluateExpression();
		if (variable.isNumber()) {
			return variable;
		}
		throw new CompileException(textBundle.get("ApplesoftCompiler.NumberRequiredError")); //$NON-NLS-1$
	}
	
	/**
	 * Answer with the line number label.  Used by GOTO and ON expr GOTO
	 * statements.
	 */
	protected String getLineNumberLabel() throws CompileException {
		ApplesoftToken token = nextToken();
		if (token.isString() && isIntegerNumber(token.getStringValue())) {
			return "LINE" + token.getStringValue(); //$NON-NLS-1$
		}
		throw new CompileException(textBundle.
				format("ApplesoftCompiler.ExpectingLineNumberError",  //$NON-NLS-1$
						token.toString()));
	}
	
	protected void addLoadByteValue(Variable variable, char register) throws CompileException {
		if (variable.isConstantInteger()) {
			addAssembly(null, "LD" + register, "#" + variable.getValue()); //$NON-NLS-1$ //$NON-NLS-2$
		} else if(variable.isTypeInteger()) {
			addAssembly(null, "LD" + register, variable.getName()); //$NON-NLS-1$
		} else if (variable.isConstantFloat() || variable.isTypeFloat()) {
			addAssembly(null, "LDY", "#>" + variable.getName()); //$NON-NLS-1$ //$NON-NLS-2$
			addAssembly(null, "LDA", "#<" + variable.getName()); //$NON-NLS-1$ //$NON-NLS-2$
			addAssembly(null, "JSR","MOVFM"); //$NON-NLS-1$ //$NON-NLS-2$
			addAssembly(null, "JSR", "QINT"); //$NON-NLS-1$ //$NON-NLS-2$
			addAssembly(null, "LD" + register, "FACLO"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			throw new CompileException(textBundle.
					format("ApplesoftCompiler.InvalidByteFormatError", //$NON-NLS-1$
							variable.getName()));
		}
	}

	protected void addLoadWordValue(Variable variable, char registerHi, char registerLo) throws CompileException {
		if (variable.isConstantInteger()) {
			addAssembly(null, "LD" + registerHi, "#>" + variable.getValue()); //$NON-NLS-1$ //$NON-NLS-2$
			addAssembly(null, "LD" + registerLo, "#<" + variable.getValue()); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (variable.isTypeInteger()) {
			addAssembly(null, "LD" + registerHi, variable.getName() + "+1"); //$NON-NLS-1$ //$NON-NLS-2$
			addAssembly(null, "LD" + registerLo, variable.getName()); //$NON-NLS-1$
		} else if (variable.isConstantFloat() || variable.isTypeFloat()) {
			addLoadFac(variable);
			addAssembly(null, "JSR", "QINT"); //$NON-NLS-1$ //$NON-NLS-2$
			addAssembly(null, "LD" + registerHi, "FACMO"); //$NON-NLS-1$ //$NON-NLS-2$
			addAssembly(null, "LD" + registerLo, "FACLO"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			throw new CompileException(textBundle.
					format("ApplesoftCompiler.InvalidWordFormatError", //$NON-NLS-1$
							variable.getName()));
		}
	}

	protected void addLoadAddress(Variable variable, char registerHi, char registerLo) {
		addAssembly(null, "LD" + registerHi, "#>" + variable.getName()); //$NON-NLS-1$ //$NON-NLS-2$
		addAssembly(null, "LD" + registerLo, "#<" + variable.getName()); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	protected void addLoadFac(Variable variable) throws CompileException {
		if (variable.isConstantFloat() || variable.isTypeFloat()) {
			addLoadAddress(variable, 'Y', 'A');
			addAssembly(null, "JSR","MOVFM"); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (variable.isConstantInteger() || variable.isTypeInteger()) {
			addLoadWordValue(variable, 'A', 'Y');
			addAssembly(null, "JSR", "GIVAYF"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			throw new CompileException(textBundle.
					format("ApplesoftCompiler.InvalidFloatTypeError", //$NON-NLS-1$
							variable.getName()));
		}
	}
	
	protected void addCopyFac(Variable variable) throws CompileException {
		if (variable.isTypeFloat()) {
			addLoadAddress(variable, 'Y', 'X');
			addAssembly(null, "JSR", "MOVMF"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			throw new CompileException(
					textBundle.get("ApplesoftCompiler.CannotCopyToFloatError")); //$NON-NLS-1$
		}
	}
	
	public void evaluateHTAB() throws CompileException {
		addLoadByteValue(evaluateExpression(), 'A');
		addAssembly(null, "STA", "CH"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void evaluateVTAB() throws CompileException {
		addLoadByteValue(evaluateExpression(), 'X');
		addAssembly(null, "DEX", null); //$NON-NLS-1$
		addAssembly(null, "STX", "CV"); //$NON-NLS-1$ //$NON-NLS-2$
		addAssembly(null, "JSR", "LF"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void evaluateHCOLOR() throws CompileException {
		addLoadByteValue(evaluateExpression(), 'X');
		addAssembly(null, "JSR", "SETHCOL"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void evaluatePRINT() throws CompileException {
		ApplesoftToken token = null;
		do {
			Variable variable = evaluateExpression();
			if (variable == null) {
				addAssembly(null, "JSR", "PRCR"); //$NON-NLS-1$ //$NON-NLS-2$
			} else if (variable.isConstantFloat() || variable.isTypeFloat()) {
				addLoadFac(variable);
				addAssembly(null, "JSR", "PRNTFAC"); //$NON-NLS-1$ //$NON-NLS-2$
			} else if (variable.isConstantInteger() || variable.isTypeInteger()) {
				addLoadWordValue(variable, 'X', 'A');
				addAssembly(null, "JSR", "LINPRT"); //$NON-NLS-1$ //$NON-NLS-2$
			} else if (variable.isConstantString()) {
				addLoadAddress(variable, 'Y', 'A');
				addAssembly(null, "JSR", "STROUT"); //$NON-NLS-1$ //$NON-NLS-2$
			} else if (variable.isTypeString()) {
				throw new CompileException(textBundle.get("ApplesoftCompiler.StringPrintUnsupported")); //$NON-NLS-1$
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
		addAssembly(null, "JMP", getLineNumberLabel()); //$NON-NLS-1$
	}
	
	protected void checkSyntax(byte tokenValue, String expectedToken) throws CompileException {
		ApplesoftToken token = nextToken();
		if (token.getTokenValue() != tokenValue) {
			// FIXME: Should this read token from master token list?
			throw new CompileException(textBundle.
					format("ApplesoftCompiler.SyntaxError", expectedToken)); //$NON-NLS-1$
		}
	}

	protected void checkSyntax(String stringValue, String expectedToken) throws CompileException {
		ApplesoftToken token = nextToken();
		if (!stringValue.equals(token.getStringValue())) {
			// FIXME: Should this read token from master token list?
			throw new CompileException(textBundle.
					format("ApplesoftCompiler.SyntaxError", expectedToken)); //$NON-NLS-1$
		}
	}
	
	public void evaluateFOR() throws CompileException {
		Variable loopVariable = evaluateExpression();
		if (!loopVariable.isTypeFloat() && !loopVariable.isTypeInteger()) {
			throw new CompileException(
					textBundle.get("ApplesoftCompiler.ForStatementUnsupportedTypeError")); //$NON-NLS-1$
		}
		checkSyntax(EQUALS, "="); //$NON-NLS-1$
		Variable startValue = evaluateNumber();
		checkSyntax(TO, "TO"); //$NON-NLS-1$
		Variable endValue = evaluateNumber();
		// FIXME: Need to handle STEP
		String loopName = "FOR" + loopVariables.size(); //$NON-NLS-1$
		loopVariables.add(loopName);
		addLoadFac(startValue);
		addCopyFac(loopVariable);
		addAssembly(loopName, null, null);
		addLoadFac(endValue);
		addLoadAddress(loopVariable, 'Y', 'A');
		addAssembly(null, "JSR", "FCOMP"); //$NON-NLS-1$ //$NON-NLS-2$
		addAssembly(null, "CMP", "#$FF");	// loopVariable > endValue //$NON-NLS-1$ //$NON-NLS-2$
		addAssembly(null, "BEQ", "END" + loopName); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void evaluateHPLOT() throws CompileException {
		boolean firstCoordinate = true;
		while (peekToken() != null && !peekToken().isEndOfCommand()) {
			if (!firstCoordinate) {
				checkSyntax(TO, "TO"); //$NON-NLS-1$
			}
			Variable coordX = evaluateNumber();
			checkSyntax(",", ", (comma)"); //$NON-NLS-1$ //$NON-NLS-2$
			Variable coordY = evaluateNumber();
			if (firstCoordinate) {
				addLoadWordValue(coordX, 'Y', 'X');
				addLoadByteValue(coordY, 'A');
				addAssembly(null, "JSR", "HPOSN"); //$NON-NLS-1$ //$NON-NLS-2$
				firstCoordinate = false;
			} else {
				addLoadWordValue(coordX, 'X', 'A');
				addLoadByteValue(coordY, 'Y');
				addAssembly(null, "JSR", "HLIN"); //$NON-NLS-1$ //$NON-NLS-2$
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
			addAssembly(null, "LDY", "#1"); //$NON-NLS-1$ //$NON-NLS-2$
			addAssembly(null, "JSR", "SNGFLT"); //$NON-NLS-1$ //$NON-NLS-2$
			addLoadAddress(variable, 'Y', 'A');
			addAssembly(null, "JSR", "FADD"); //$NON-NLS-1$ //$NON-NLS-2$
			addCopyFac(variable);
		} else if (variable.isTypeInteger()) {
			addAssembly(null, "INC", variable.getName()); //$NON-NLS-1$
			addAssembly(null, "BNE", ":1"); //$NON-NLS-1$ //$NON-NLS-2$
			addAssembly(null, "INC", variable.getName() + "+1"); //$NON-NLS-1$ //$NON-NLS-2$
			addAssembly(":1", null, null); //$NON-NLS-1$
		}
		String loopName = (String) loopVariables.pop();
		addAssembly(null, "JMP", loopName); //$NON-NLS-1$
		addAssembly("END" + loopName, null, null); //$NON-NLS-1$
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
