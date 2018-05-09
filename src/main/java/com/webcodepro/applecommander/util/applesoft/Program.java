package com.webcodepro.applecommander.util.applesoft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/** A Program is a series of lines. */
public class Program {
	public final List<Line> lines = new ArrayList<>();
	
	public void prettyPrint(PrintStream ps) {
		for (Line line : lines) {
			line.prettyPrint(ps);
		}
	}

	public byte[] toBytes(int address) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		for (Line line : lines) address = line.toBytes(address, os);
		os.write(0x00);
		os.write(0x00);
		return os.toByteArray();
	}
}
