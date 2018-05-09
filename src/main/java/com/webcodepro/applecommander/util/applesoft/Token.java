package com.webcodepro.applecommander.util.applesoft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Optional;

/**
 * A Token in the classic compiler sense, in that this represents a component of the application.
 * 
 * @author rob
 */
public class Token {
	public final int line;
	public final Type type;
	public final ApplesoftKeyword keyword;
	public final Double number;
	public final String text;
	
	private Token(int line, Type type, ApplesoftKeyword keyword, Double number, String text) {
		this.line = line;
		this.type = type;
		this.keyword = keyword;
		this.number = number;
		this.text = text;
	}
	@Override
	public String toString() {
		switch (type) {
		case EOL:
			return type.toString();
		case KEYWORD:
			return keyword.toString();
		case NUMBER:
			return String.format("%s(%f)", type, number);
		default:
			return String.format("%s(%s)", type, text);
		}
	}
	
	public void prettyPrint(PrintStream ps) {
		switch (type) {
		case EOL:
			ps.print("<EOL>");
			break;
		case COMMENT:
			ps.printf(" REM %s", text);
			break;
		case STRING:
			ps.printf("\"%s\"", text);
			break;
		case KEYWORD:
			ps.printf(" %s ", keyword.text);
			break;
		case IDENT:
		case SYNTAX:
			ps.print(text);
			break;
		case NUMBER:
			if (Math.rint(number) == number) {
				ps.print(number.intValue());
			} else {
				ps.print(number);
			}
			break;
		}
	}

	public void toBytes(ByteArrayOutputStream os) throws IOException {
		switch (type) {
		case COMMENT:
			os.write(ApplesoftKeyword.REM.code);
			os.write(text.getBytes());
			break;
		case EOL:
			os.write(0x00);
			break;
		case IDENT:
			os.write(text.getBytes());
			break;
		case KEYWORD:
			os.write(keyword.code);
			break;
		case NUMBER:
			if (Math.rint(number) == number) {
				os.write(Integer.toString(number.intValue()).getBytes());
			} else {
				os.write(Double.toString(number).getBytes());
			}
			break;
		case STRING:
			os.write('"');
			os.write(text.getBytes());
			os.write('"');
			break;
		case SYNTAX:
			Optional<ApplesoftKeyword> opt = ApplesoftKeyword.find(text);
			if (opt.isPresent()) {
				os.write(opt.get().code);
			} else {
				os.write(text.getBytes());
			}
			break;
		}
	}
	
	public static Token eol(int line) {
		return new Token(line, Type.EOL, null, null, null);
	}
	public static Token number(int line, Double number) {
		return new Token(line, Type.NUMBER, null, number, null);
	}
	public static Token ident(int line, String text) {
		return new Token(line, Type.IDENT, null, null, text);
	}
	public static Token comment(int line, String text) {
		return new Token(line, Type.COMMENT, null, null, text);
	}
	public static Token string(int line, String text) {
		return new Token(line, Type.STRING, null, null, text);
	}
	public static Token keyword(int line, ApplesoftKeyword keyword) {
		// Note that the text component is useful to have for parsing, so we replicate it...
		return new Token(line, Type.KEYWORD, keyword, null, keyword.text);
	}
	public static Token syntax(int line, int ch) {
		return new Token(line, Type.SYNTAX, null, null, String.format("%c", ch));
	}
	
	public static enum Type {
		EOL, NUMBER, IDENT, COMMENT, STRING, KEYWORD, SYNTAX
	}
}