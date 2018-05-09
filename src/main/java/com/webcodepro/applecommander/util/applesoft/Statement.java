package com.webcodepro.applecommander.util.applesoft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/** A Statement is simply a series of Tokens. */
public class Statement {
	public final List<Token> tokens = new ArrayList<>();
	
	public void prettyPrint(PrintStream ps) {
		for (Token token : tokens) {
			token.prettyPrint(ps);
		}
	}

	public void toBytes(ByteArrayOutputStream os) throws IOException {
		for (Token t : tokens) t.toBytes(os);
	}
}
