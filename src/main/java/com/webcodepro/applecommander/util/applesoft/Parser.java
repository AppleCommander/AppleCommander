package com.webcodepro.applecommander.util.applesoft;

import java.util.Objects;
import java.util.Queue;

import com.webcodepro.applecommander.util.applesoft.Token.Type;

/** 
 * The Parser will read a series of Tokens and build a Program.
 * Note that this is not a compiler and does not "understand" the program. 
 */
public class Parser {
	private final Queue<Token> tokens;
	
	public Parser(Queue<Token> tokens) {
		Objects.requireNonNull(tokens);
		this.tokens = tokens;
	}
	
	public Program parse() {
		Program program = new Program();
		while (!tokens.isEmpty()) {
			Line line = readLine();
			program.lines.add(line);
		}
		return program;
	}
	
	public Line readLine() {
		Line line = new Line(expectNumber());
		while (!tokens.isEmpty() && tokens.peek().type != Type.EOL) {
			Statement statement = readStatement();
			if (statement != null) {
				line.statements.add(statement);
			} else {
				break;
			}
		}
		if (!tokens.isEmpty() && tokens.peek().type == Type.EOL) {
			tokens.remove();	// Skip that EOL
		}
		return line;
	}
	
	public Statement readStatement() {
		Statement statement = new Statement();
		while (!tokens.isEmpty()) {
			if (tokens.peek().type == Type.EOL) break;
			Token t = tokens.remove();
			if (":".equals(t.text)) break;
			statement.tokens.add(t);
		}
		return statement;
	}
	
	public int expectNumber() {
		Token c = tokens.remove();
		if (c.type != Type.NUMBER) {
			throw new RuntimeException("Expected a number in line #" + c.line);
		}
		return c.number.intValue();
	}
}
