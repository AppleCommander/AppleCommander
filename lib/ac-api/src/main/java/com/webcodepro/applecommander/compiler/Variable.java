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

/**
 * Contains information referring to a BASIC variable. 
 * @author Rob
 */
public class Variable {
//	FIXME: Should Variable have specific subclasses that handle the types appropriately?
	public static final int TYPE_STRING = 1;
	public static final int TYPE_INTEGER = 2;
	public static final int TYPE_FLOAT = 3;
	public static final int CONST_STRING = 4;
	public static final int CONST_INTEGER = 5;
	public static final int CONST_FLOAT = 6;
	private String name;
	private String value;
	private int type;
	
	/**
	 * Construct a Variable.
	 */
	public Variable(String name, int type, String value) {
		this.name = name;
		this.type = type;
		this.value = value;
	}
	
	/**
	 * Answers with the variable name.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Answers with the variable type.
	 */
	public int getType() {
		return type;
	}
	
	/**
	 * Answers true if this variable is string.
	 */
	public boolean isTypeString() {
		return type == TYPE_STRING;
	}
	
	/**
	 * Answers true if this variable is integer.
	 */
	public boolean isTypeInteger() {
		return type == TYPE_INTEGER;
	}
	
	/**
	 * Answers true if this variable is floating point.
	 */
	public boolean isTypeFloat() {
		return type == TYPE_FLOAT;
	}
	
	/**
	 * Answers true if this is a variable.
	 */
	public boolean isVariableType() {
		return isTypeFloat() || isTypeInteger() || isTypeString();
	}
	
	/**
	 * Answers true if this is a number.
	 */
	public boolean isNumber() {
		return isConstantFloat() || isConstantInteger()
			|| isTypeFloat() || isTypeInteger();
	}
	
	/**
	 * Answers true if this is a string constant.
	 */
	public boolean isConstantString() {
		return type == CONST_STRING;
	}

	/**
	 * Answers true if this is an integer constant.
	 */
	public boolean isConstantInteger() {
		return type == CONST_INTEGER;
	}
	
	/**
	 * Answers true if this is a floating point constant.
	 */
	public boolean isConstantFloat() {
		return type == CONST_FLOAT;
	}
	
	/**
	 * Answers true if this is a constant value.
	 */
	public boolean isConstant() {
		return isConstantString() || isConstantInteger() || isConstantFloat();
	}
	
	/**
	 * Return the value.
	 */
	public String getValue() {
		return value;
	}
}
