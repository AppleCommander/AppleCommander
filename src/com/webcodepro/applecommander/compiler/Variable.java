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
	public static final int TYPE_STRING = 1;
	public static final int TYPE_INTEGER = 2;
	public static final int TYPE_FLOAT = 3;
	private String name;
	private int type;
	
	/**
	 * Construct a Variable.
	 */
	public Variable(String name, int type) {
		this.name = name;
		this.type = type;
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
}
