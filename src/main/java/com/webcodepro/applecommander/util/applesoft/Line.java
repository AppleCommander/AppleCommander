package com.webcodepro.applecommander.util.applesoft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/** An AppleSoft BASIC Line representation. */
public class Line {
	public final int lineNumber;
	public final List<Statement> statements = new ArrayList<>();
	
	public Line(int lineNumber) {
		this.lineNumber = lineNumber;
	}
	
	public void prettyPrint(PrintStream ps) {
		boolean first = true;
		for (Statement statement : statements) {
			if (first) {
				first = false;
				ps.printf("%5d ", lineNumber);
			} else {
				ps.printf("%5s ", ":");
			}
			statement.prettyPrint(ps);
			ps.println();
		}
	}

	public int toBytes(int address, ByteArrayOutputStream os) throws IOException {
		ByteArrayOutputStream tmp = new ByteArrayOutputStream();
		for (Statement stmt : statements) {
			if (tmp.size() > 0) tmp.write(':');
			stmt.toBytes(tmp);
		}
		
		int nextAddress = address + tmp.size() + 5;
		os.write(nextAddress);
		os.write(nextAddress >> 8);
		os.write(lineNumber);
		os.write(lineNumber >> 8);
		tmp.writeTo(os);
		os.write(0x00);
		return nextAddress;
	}
}
