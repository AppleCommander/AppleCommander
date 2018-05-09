package com.webcodepro.applecommander.util.applesoft;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** All elements of AppleSoft that are tokenized in some manner.  "Keyword" was picked as it is not the word token. ;-) */
public enum ApplesoftKeyword {
    END(0x80, "END"), 
    FOR(0x81, "FOR"), 
    NEXT(0x82, "NEXT"), 
    DATA(0x83, "DATA"), 
    INPUT(0x84, "INPUT"), 
    DEL(0x85, "DEL"), 
    DIM(0x86, "DIM"), 
    READ(0x87, "READ"), 
    GR(0x88, "GR"), 
    TEXT(0x89, "TEXT"), 
    PR(0x8A, "PR#"), 
    IN(0x8B, "IN#"), 
    CALL(0x8C, "CALL"), 
    PLOT(0x8D, "PLOT"), 
    HLIN(0x8E, "HLIN"), 
    VLIN(0x8F, "VLIN"), 
    HGR2(0x90, "HGR2"), 
    HGR(0x91, "HGR"), 
    HCOLOR(0x92, "HCOLOR="), 
    HPLOT(0x93, "HPLOT"), 
    DRAW(0x94, "DRAW"), 
    XDRAW(0x95, "XDRAW"), 
    HTAB(0x96, "HTAB"), 
    HOME(0x97, "HOME"), 
    ROT(0x98, "ROT="), 
    SCALE(0x99, "SCALE="), 
    SHLOAD(0x9A, "SHLOAD"), 
    TRACE(0x9B, "TRACE"), 
    NOTRACE(0x9C, "NOTRACE"), 
    NORMAL(0x9D, "NORMAL"), 
    INVERSE(0x9E, "INVERSE"), 
    FLASH(0x9F, "FLASH"), 
    COLOR(0xA0, "COLOR="), 
    POP(0xA1, "POP"), 
    VTAB(0xA2, "VTAB"), 
    HIMEM(0xA3, "HIMEM:"), 
    LOMEM(0xA4, "LOMEM:"), 
    ONERR(0xA5, "ONERR"), 
    RESUME(0xA6, "RESUME"), 
    RECALL(0xA7, "RECALL"), 
    STORE(0xA8, "STORE"), 
    SPEED(0xA9, "SPEED="), 
    LET(0xAA, "LET"), 
    GOTO(0xAB, "GOTO"), 
    RUN(0xAC, "RUN"), 
    IF(0xAD, "IF"), 
    RESTORE(0xAE, "RESTORE"), 
    amp(0xAF, "&"), 
    GOSUB(0xB0, "GOSUB"), 
    RETURN(0xB1, "RETURN"), 
    REM(0xB2, "REM"), 
    STOP(0xB3, "STOP"), 
    ON(0xB4, "ON"), 
    WAIT(0xB5, "WAIT"), 
    LOAD(0xB6, "LOAD"), 
    SAVE(0xB7, "SAVE"), 
    DEF(0xB8, "DEF"), 
    POKE(0xB9, "POKE"), 
    PRINT(0xBA, "PRINT"), 
    CONT(0xBB, "CONT"), 
    LIST(0xBC, "LIST"), 
    CLEAR(0xBD, "CLEAR"), 
    GET(0xBE, "GET"), 
    NEW(0xBF, "NEW"), 
    TAB(0xC0, "TAB("), 
    TO(0xC1, "TO"), 
    FN(0xC2, "FN"), 
    SPC(0xC3, "SPC("), 
    THEN(0xC4, "THEN"), 
    AT(0xC5, "AT"), 
    NOT(0xC6, "NOT"), 
    STEP(0xC7, "STEP"), 
    add(0xC8, "+"), 
    sub(0xC9, "-"), 
    mul(0xCA, "*"), 
    div(0xCB, "/"), 
    pow(0xCC, "^"), 
    AND(0xCD, "AND"), 
    OR(0xCE, "OR"), 
    gt(0xCF, ">"), 
    eq(0xD0, "="), 
    lt(0xD1, "<"), 
    SGN(0xD2, "SGN"), 
    INT(0xD3, "INT"), 
    ABS(0xD4, "ABS"), 
    USR(0xD5, "USR"), 
    FRE(0xD6, "FRE"), 
    SCRN(0xD7, "SCRN("), 
    PDL(0xD8, "PDL"), 
    POS(0xD9, "POS"), 
    SQR(0xDA, "SQR"), 
    RND(0xDB, "RND"), 
    LOG(0xDC, "LOG"), 
    EXP(0xDD, "EXP"), 
    COS(0xDE, "COS"), 
    SIN(0xDF, "SIN"), 
    TAN(0xE0, "TAN"), 
    ATN(0xE1, "ATN"), 
    PEEK(0xE2, "PEEK"), 
    LEN(0xE3, "LEN"), 
    STR(0xE4, "STR$"), 
    VAL(0xE5, "VAL"), 
    ASC(0xE6, "ASC"), 
    CHR(0xE7, "CHR$"), 
    LEFT(0xE8, "LEFT$"), 
    RIGHT(0xE9, "RIGHT$"), 
    MID(0xEA, "MID$");
	
	/**
	 * The AppleSoft token value.  Token is overloaded, so "code" is good enough.
	 */
	public final int code;
	/**
	 * Full text of the token.
	 */
	public final String text;
	/**
	 * Token parts as seen by the StreamTokenizer.
	 */
	public final List<String> parts;
	/** 
	 * Indicates that this needs _just_ a closing right parenthesis since the 
	 * opening left parenthesis is included in the token 
	 */
	public boolean needsRParen;
	
	private ApplesoftKeyword(int code, String text) {
		this.code = code;
		this.text = text;
		
		try {
			// A bit brute-force, but should always match the tokenizer configuration!
			List<String> list = new ArrayList<>();
			StreamTokenizer t = tokenizer(new StringReader(text));
			while (t.nextToken() != StreamTokenizer.TT_EOF) {
				switch (t.ttype) {
				case StreamTokenizer.TT_WORD:
					list.add(t.sval);
					break;
				default:
					list.add(String.format("%c", t.ttype));
					break;
				}
			}
			this.parts = Collections.unmodifiableList(list);
			this.needsRParen = parts.contains("(");
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public boolean equalsIgnoreCase(String value) {
		return this.text.equalsIgnoreCase(value);
	}
	
	@Override
	public String toString() {
		return String.format("%s (%02x)", text, code);
	}
	
	/** Utility method to create a shared definition for AppleSoft file parsing. */
	public static StreamTokenizer tokenizer(Reader r) {
		StreamTokenizer tokenizer = new StreamTokenizer(r);
		tokenizer.resetSyntax();
		tokenizer.wordChars('a', 'z');
		tokenizer.wordChars('A', 'Z');
		tokenizer.wordChars(128 + 32, 255);
		tokenizer.whitespaceChars(0, ' ');
		tokenizer.quoteChar('"');
		tokenizer.parseNumbers();
		// This resets part of parseNumbers to match AppleSoft tokenization!
		tokenizer.ordinaryChar('-');
		tokenizer.eolIsSignificant(true);
		return tokenizer;
	}
	
	/** Utility method to locate a keyword ignoring case. */
	public static Optional<ApplesoftKeyword> find(String value) {
		Objects.requireNonNull(value);
		for (ApplesoftKeyword kw : values()) {
			if (value.equalsIgnoreCase(kw.parts.get(0))) {
				return Optional.of(kw);
			}
		}
		return Optional.empty();
	}
}
