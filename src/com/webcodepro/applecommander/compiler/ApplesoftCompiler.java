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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Compile the given file as an Applesoft file.
 * <p>
 * Date created: Nov 2, 2002 10:04:10 PM
 * @author: Rob Greene
 */
public class ApplesoftCompiler implements ApplesoftTokens {
	private ApplesoftTokenizer tokenizer;

	private List stringVariables = new ArrayList();
	private List integerVariables = new ArrayList();
	private List floatVariables = new ArrayList();
	private Map stringConstants = new HashMap();
	private Map integerConstants = new HashMap();
	private Map floatConstants = new HashMap();

	/**
	 * Constructor for ApplesoftCompiler.
	 */
	public ApplesoftCompiler(FileEntry fileEntry) {
		super();
		tokenizer = new ApplesoftTokenizer(fileEntry);
	}

	/**
	 * Compile the given FileEntry and return the assembly code.
	 */
	public byte[] compile() {
		ByteArrayOutputStream assemblyStream = new ByteArrayOutputStream();
		PrintWriter assemblyWriter = new PrintWriter(assemblyStream);
		StringBuffer sourceLine = new StringBuffer();
		StringBuffer sourceAssembly = new StringBuffer();
		while (tokenizer.hasMoreTokens()) {
			ApplesoftToken token = tokenizer.getNextToken();
			if (token == null) {
				break;
			} else if (token.isLineNumber()) {
				if (sourceLine.length() > 0) {
					assemblyWriter.println(sourceLine.toString());
					assemblyWriter.println(sourceAssembly.toString());
					sourceLine.setLength(0);
					sourceAssembly.setLength(0);
				}
				sourceAssembly.append("LINE");
				sourceAssembly.append(token.getLineNumber());
				sourceAssembly.append(":\n");
				sourceLine.append("* ");
				sourceLine.append(token.getLineNumber());
				sourceLine.append(" ");
			} else if (token.isToken()) {
				sourceLine.append(token.getTokenString());
				processToken(sourceAssembly, sourceLine, token, tokenizer);
			} else if (token.isString()) {
				sourceLine.append(token.getStringValue());
				// FIXME - process string/expressions!
			}
		}
		if (sourceLine.length() > 0) {
			assemblyWriter.println(sourceLine.toString());
			assemblyWriter.println(sourceAssembly.toString());
			sourceLine.setLength(0);
			sourceAssembly.setLength(0);
		}
		
		if (!stringConstants.isEmpty()) {
			assemblyWriter.println("\n* String constant values:");
			Iterator iterator = stringConstants.keySet().iterator();
			while (iterator.hasNext()) {
				String name = (String) iterator.next();
				assemblyWriter.print(name);
				assemblyWriter.print(":\tASC ");
				assemblyWriter.println(stringConstants.get(name));
				assemblyWriter.println("\tHEX 00");
			}
		}
		if (!integerConstants.isEmpty()) {
			assemblyWriter.println("\n* Integer constant values:");
			Iterator iterator = integerConstants.keySet().iterator();
			while (iterator.hasNext()) {
				String name = (String) iterator.next();
				assemblyWriter.print(name);
				assemblyWriter.print(":\tDW ");
				assemblyWriter.println(integerConstants.get(name));
			}
		}
		
		if (!integerVariables.isEmpty()) {
			assemblyWriter.println("\n* Integer variables:");
			for (int i = 0; i<integerVariables.size(); i++) {
				assemblyWriter.print(integerVariables.get(i));
				assemblyWriter.println(": DW 0");
			}
		}
		if (!floatVariables.isEmpty()) {
			assemblyWriter.println("\n* Float variables:");
			for (int i = 0; i<floatVariables.size(); i++) {
				assemblyWriter.print(floatVariables.get(i));
				assemblyWriter.println(": DS 5");
			}
		}
		
		assemblyWriter.close();
		return assemblyStream.toByteArray();
	}
	
	/**
	 * Process and individual token.
	 */
	protected void processToken(StringBuffer sourceAssembly, 
	StringBuffer sourceLine, ApplesoftToken token, ApplesoftTokenizer tokenizer) {
		String expr = null;
		switch (token.getTokenValue()) {
			case HOME:	sourceAssembly.append("\tJSR $FC58\n");
						break;
			case STOP:
			case RETURN:
			case END:	sourceAssembly.append("\tRTS\n");
						break;
			case TEXT:	sourceAssembly.append("\tJSR $FB2F\n");
						break;
			case HGR:	sourceAssembly.append("\tJSR $F3E2\n");
						break;
			case HGR2:	sourceAssembly.append("\tJSR $F3D8\n");
						break;
			case GR:	sourceAssembly.append("\tJSR $FB40\n");
						break;
			case INVERSE:
						sourceAssembly.append("\tLDA #$3F\n");
						sourceAssembly.append("\tSTA $32\n");
						break;
			case NORMAL:
						sourceAssembly.append("\tLDA #$FF\n");
						sourceAssembly.append("\tSTA $32\n");
						break;
			case FLASH:
						sourceAssembly.append("\tLDA #$7F\n");
						sourceAssembly.append("\tSTA $32\n");
						break;
			case VTAB:	expr = evaluateExpression(sourceAssembly, sourceLine);
						sourceAssembly.append("\tLDA ");
						sourceAssembly.append(expr);
						sourceAssembly.append("\n");
						sourceAssembly.append("\tSTA $25\n");
						sourceAssembly.append("\tJSR $FC66\n");
						break;
			case HTAB:	expr = evaluateExpression(sourceAssembly, sourceLine);
						sourceAssembly.append("\tLDA ");
						sourceAssembly.append(expr);
						sourceAssembly.append("\n");
						sourceAssembly.append("\tSTA $24\n");
						break;
			case HCOLOR:
						expr = evaluateExpression(sourceAssembly, sourceLine);
						sourceAssembly.append("\tLDX ");
						sourceAssembly.append(expr);
						sourceAssembly.append("\n");
						sourceAssembly.append("\tJSR $F6F0\n");
						break;
			case PRINT:
						expr = evaluateExpression(sourceAssembly, sourceLine);
						if (isIntegerVariable(expr)) {
							throw new IllegalArgumentException("Integer not supported in print: " + expr);
						} else if (isFloatVariable(expr)) {
							sourceAssembly.append("\tLDY #>");
							sourceAssembly.append(expr);
							sourceAssembly.append("\n");
							sourceAssembly.append("\tLDA #<");
							sourceAssembly.append(expr);
							sourceAssembly.append("\n");
							sourceAssembly.append("\tJSR $EAF9\t; MOVFM\n");
							sourceAssembly.append("\tJSR $ED2E\t; PRNTFAC\n");
						} else if (isStringVariable(expr)) {
							sourceAssembly.append("\tLDY #0\n");
							sourceAssembly.append(":loop\tLDA ");
							sourceAssembly.append(expr);
							sourceAssembly.append(",Y\n");
							sourceAssembly.append("\tBEQ :end\n");
							sourceAssembly.append("\tJSR COUT\n");
							sourceAssembly.append("\tINY\n");
							sourceAssembly.append("\tBNE :loop\n");
							sourceAssembly.append(":end\n");
						} else {
							throw new IllegalArgumentException("Invalid expr in print: " + expr);
						}
						break;
			case FOR:
						String loopVariable = evaluateExpression(sourceAssembly, sourceLine);
						if (!isFloatVariable(loopVariable)) {
							throw new IllegalArgumentException("FOR loop argument must be a float");
						}
						token = tokenizer.getNextToken();
						if (token.getTokenValue() != EQUALS) {
							throw new IllegalArgumentException("FOR requires =");
						}
						sourceLine.append(token.getTokenString());
						String startValue = evaluateExpression(sourceAssembly, sourceLine);
						token = tokenizer.getNextToken();
						if (token.getTokenValue() != TO) {
							throw new IllegalArgumentException("FOR requires TO");
						}
						sourceLine.append(token.getTokenString());
						String endValue = evaluateExpression(sourceAssembly, sourceLine);
						if (isFloatVariable(loopVariable)) {
							// FIXME: Assumes start/end are integer
							sourceAssembly.append("\tLDY ");
							sourceAssembly.append(startValue);
							sourceAssembly.append("\n");
							sourceAssembly.append("\tLDA ");
							sourceAssembly.append(startValue);
							sourceAssembly.append("\n");
							sourceAssembly.append("\tJSR $E2F2 ; GIVAYF\n");
							sourceAssembly.append("\tLDY #>");
							sourceAssembly.append(loopVariable);
							sourceAssembly.append("\n");
							sourceAssembly.append("\tLDX #<");
							sourceAssembly.append(loopVariable);
							sourceAssembly.append("\n");
							sourceAssembly.append("\tJSR $EB2B ; MOVMF\n");
						}
						break;
		}
	}
	
	/**
	 * Evaluate an expression and return the variable name that
	 * contains the value.
	 */
	protected String evaluateExpression(StringBuffer sourceAssembly, StringBuffer sourceLine) {
		// FIXME: no type checking available
		ApplesoftToken token = tokenizer.getNextToken();
		if (token.isString()) {
			String value = token.getStringValue();
			sourceLine.append(value);
			if (isIntegerNumber(value)) {
				return addIntegerConstant(value);
			} else if (value.startsWith("\"")) {
				return addStringConstant(value);
			} else {	// assume variable name
				return addVariable(value);
			}
		} else {
			throw new IllegalArgumentException("Oops!");
		}
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
	
	protected String addIntegerConstant(String value) {
		String name = "INT" + value;
		if (!integerConstants.containsKey(name)) {
			integerConstants.put(name, value);
		}
		return name;
	}

	protected String addStringConstant(String value) {
		String name = "STR" + stringConstants.size();
		if (stringConstants.containsValue(value)) {
			Iterator iterator = stringConstants.keySet().iterator();
			while (iterator.hasNext()) {
				String key = (String) iterator.next();
				String keyValue = (String) stringConstants.get(key);
				if (value.equals(keyValue)) {
					name = key;
					break;
				}
			}
		} else {
			stringConstants.put(name, value);
		}
		return name;
	}
	
	protected String addVariable(String variableName) {
		if (variableName.endsWith("$")) {
			variableName = "STR" + variableName;
			if (!stringVariables.contains(variableName)) {
				stringVariables.add(variableName);
			}
		} else if (variableName.endsWith("%")) {
			variableName = "INT" + variableName;
			if (!integerVariables.contains(variableName)) {
				integerVariables.add(variableName);
			}
		} else {
			variableName = "FP" + variableName;
			if (!floatVariables.contains(variableName)) {
				floatVariables.add(variableName);
			}
		}
		return variableName;
	}
	
	protected boolean isIntegerVariable(String name) {
		return integerVariables.contains(name) || integerConstants.containsKey(name);
	}

	protected boolean isFloatVariable(String name) {
		return floatVariables.contains(name) || floatConstants.containsKey(name);
	}

	protected boolean isStringVariable(String name) {
		return stringVariables.contains(name) || stringConstants.containsKey(name);
	}
}
