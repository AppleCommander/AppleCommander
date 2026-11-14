/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2026 by Robert Greene and others
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
package org.applecommander.converter;

import org.applecommander.filestore.FileEntry;
import org.applecommander.transfer.ProdosAttributes;
import org.applecommander.util.DataBuffer;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/// Detokenize the given Applesoft file. Generates a series of events
/// that can be used to generate a variety of outputs. The intent is
/// to allow a GUI to (for instance) allow line numbers to be clicked
/// on for navigation and to allow a CLI tool to dump the file as text.
public class ApplesoftConverter {
    private static final Token[] TOKENS;
    static {
        Token[] tokens = new Token[256];
        for (Token token : Token.values()) {
            tokens[token.value()] = token;
        }
        TOKENS = tokens;
    }

    private final int startAddress;
    private final DataBuffer program;

    public ApplesoftConverter(FileEntry fileEntry) {
        ProdosAttributes attributes = fileEntry.getProdosAttributes();
        this.startAddress = attributes.auxType();
        this.program = attributes.dataFork().orElseGet(fileEntry::getDataFork);
    }
    public ApplesoftConverter(DataBuffer program, int startAddress) {
        this.startAddress = startAddress;
        this.program = program;
    }

    public void generate(EventHandler handler) {
        program.position(0);
        int nextAddress = 0;
        State state = State.START;
        boolean done = false;
        while (program.hasRemaining() && !done) {
            switch (state) {
                case START -> {
                    nextAddress = program.readUnsignedShort();
                    if (nextAddress == 0) {
                        done = true;
                    }
                    else {
                        int lineNumber = program.readUnsignedShort();
                        handler.startOfLine(lineNumber + " ");
                        state = State.CODE;
                    }
                }
                case CODE -> {
                    int value = program.readUnsignedByte();
                    Token token = TOKENS[value];
                    if (value == 0) {
                        // End of line - did we have unexpected bytes? optionally display them!
                        int difference = nextAddress - startAddress - program.position() - 1;
                        if (difference > 0) {
                            byte[] data = new byte[difference];
                            program.read(data);
                            handler.rawBytes(data);
                        }
                        handler.endOfLine();
                        state = State.START;
                    }
                    else if (token == Token.REM) {
                        handler.comment(scanUntil(program, " REM ", 0));
                        handler.endOfLine();
                        state = State.START;
                    }
                    else if (token == Token.GOTO || token == Token.GOSUB) {
                        handler.token(" " + token.text() + " ");
                        handler.targetLine(scanWhile(program, -1, Character::isDigit));
                    }
                    else if (token != null) {
                        handler.token(" " + token.text() + " ");
                    }
                    else if (value == '"') {
                        // Strings are sometimes left without the end quote and Applesoft is happy with it!
                        handler.string(scanUntil(program, "\"", '"', 0));
                    }
                    else if (isCharacter(value)) {
                        handler.identifier(scanWhile(program, value,
                                ch -> isCharacter(ch) || isDigit(ch) || ch == '$' || ch == '%'));
                    }
                    else if (isDigit(value) || value == '.') {
                        handler.number(scanWhile(program, value, ch -> isDigit(ch) || ch == '.' || ch == '-'));
                    }
                    else {
                        handler.syntax(Character.toString(value));
                    }
                }
            }
        }
    }

    /// Test if this character is an ASCII character between A and Z. Java's tests are for Unicode.
    public boolean isCharacter(int ch) {
        return ch >= 'A' && ch <= 'Z';
    }

    /// Test if this character is a digit. Just in case the digits have extra codes for Unicode.
    public boolean isDigit(int ch) {
        return ch >= '0' && ch <= '9';
    }

    private String scanUntil(DataBuffer program, String start, int... endChars) {
        Set<Integer> endValues = new HashSet<>();
        for (int endChar : endChars) endValues.add(endChar);
        StringBuilder sb = new StringBuilder(start);
        int value = program.readUnsignedByte();
        while (!endValues.contains(value)) {
            sb.append((char)value);
            value = program.readUnsignedByte();
        }
        if (value != 0) {
            sb.append((char)value);
        }
        return sb.toString();
    }
    private String scanWhile(DataBuffer program, int firstChar, Predicate<Integer> includeFn) {
        StringBuilder sb = new StringBuilder();
        if (firstChar != -1) {
            sb.append((char)firstChar);
        }
        int value = program.readUnsignedByte();
        while (includeFn.test(value)) {
            sb.append((char)value);
            value = program.readUnsignedByte();
        }
        // Rewind to the character we don't want
        program.position(program.position()-1);
        return sb.toString();
    }

    private enum State {
        START,
        CODE,
        TARGET
    }

    public enum Token {
        END(0x80),
        FOR(0x81),
        NEXT(0x82),
        DATA(0x83),
        INPUT(0x84),
        DEL(0x85),
        DIM(0x86),
        READ(0x87),
        GR(0x88),
        TEXT(0x89),
        PRnbr(0x8a, "PR#"),
        INnbr(0x8b, "IN#"),
        CALL(0x8c),
        PLOT(0x8d),
        HLIN(0x8e),
        VLIN(0x8f),
        HGR2(0x90),
        HGR(0x91),
        HCOLOR(0x92, "HCOLOR="),
        HPLOT(0x93),
        DRAW(0x94),
        XDRAW(0x95),
        HTAB(0x96),
        HOME(0x97),
        ROT(0x98, "ROT="),
        SCALE(0x99, "SCALE="),
        SHLOAD(0x9a),
        TRACE(0x9b),
        NOTRACE(0x9c),
        NORMAL(0x9d),
        INVERSE(0x9e),
        FLASH(0x9f),
        COLOR(0xa0, "COLOR="),
        POP(0xa1),
        VTAB(0xa2),
        HIMEM(0xa3, "HIMEM:"),
        LOMEM(0xa4, "LOMEM:"),
        ONERR(0xa5),
        RESUME(0xa6),
        RECALL(0xa7),
        STORE(0xa8),
        SPEED(0xa9),
        LET(0xaa),
        GOTO(0xab),
        RUN(0xac),
        IF(0xad),
        RESTORE(0xae),
        AMPERSAND(0xaf, "&"),
        GOSUB(0xb0),
        RETURN(0xb1),
        REM(0xb2),
        STOP(0xb3),
        ON(0xb4),
        WAIT(0xb5),
        LOAD(0xb6),
        SAVE(0xb7),
        DEF(0xb8),
        POKE(0xb9),
        PRINT(0xba),
        CONT(0xbb),
        LIST(0xbc),
        CLEAR(0xbd),
        GET(0xbe),
        NEW(0xbf),
        TAB(0xc0, "TAB("),
        TO(0xc1),
        FN(0xc2),
        SPC(0xc3, "SPC("),
        THEN(0xc4),
        AT(0xc5),
        NOT(0xc6),
        STEP(0xc7),
        PLUS(0xc8, "+"),
        MINUS(0xc9, "-"),
        MULTIPLY(0xca, "*"),
        DIVIDE(0xcb, "/"),
        POWER(0xcc, "^"),
        AND(0xcd),
        OR(0xce),
        GREATERTHAN(0xcf, ">"),
        EQUALS(0xd0, "="),
        LESSTHAN(0xd1, "<"),
        SGN(0xd2),
        INT(0xd3),
        ABS(0xd4),
        USR(0xd5),
        FRE(0xd6),
        SCRN(0xd7, "SCRN("),
        PDL(0xd8),
        POS(0xd9),
        SQR(0xda),
        RND(0xdb),
        LOG(0xdc),
        EXP(0xdd),
        COS(0xde),
        SIN(0xdf),
        TAN(0xe0),
        ATN(0xe1),
        PEEK(0xe2),
        LEN(0xe3),
        STR$(0xe4),
        VAL(0xe5),
        ASC(0xe6),
        CHR$(0xe7),
        LEFT$(0xe8),
        RIGHT$(0xe9),
        MID$(0xea);

        private final int value;
        private final String text;

        Token(int value) {
            this.value = value;
            this.text = null;
        }
        Token(int value, String text) {
            this.value = value;
            this.text = text;
        }

        public int value() {
            return value;
        }
        public String text() {
            if (text == null) {
                return name();
            }
            return text;
        }
    }

    public interface EventHandler {
        void startOfLine(String lineNumber);
        void rawBytes(byte[] data);
        void token(String text);
        void comment(String comment);
        void endOfLine();
        void string(String string);
        void targetLine(String lineNumber);
        void identifier(String identifier);
        void number(String number);
        void syntax(String syntax);
    }
}
