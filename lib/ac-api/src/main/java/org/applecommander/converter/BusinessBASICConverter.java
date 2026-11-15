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

import com.google.common.base.Strings;
import org.applecommander.filestore.FileEntry;
import org.applecommander.transfer.ProdosAttributes;
import org.applecommander.util.DataBuffer;

/// Detokenize an Apple /// Business BASIC (BA3) file.
///
/// Apple /// Business BASIC memory format:
///
/// ```
/// (skip first 2 bytes)
/// Line
/// ...
/// Line
/// ```
///
/// where `Line` is:
///
/// * +0 (byte) - Offset to next line - `$00` is end of program.
/// * +1 (word) - Line number.
/// * +3... - Tokenized line.
/// * `$00` - End of line marker.
///
/// Tokens have their high bit set. If the first token is `$FF`, the next byte is the
/// token; otherwise the token is ANDed with `$7F` into the first part of the table.
/// (This allows for more than 128 tokens.)  For example, `END` is `$80` but `AS` is
/// `$FF` `$9C`.
///
/// @author David Schmidt (original author)
public class BusinessBASICConverter implements BASICConverter {
    // Individual tokens that we look for when processing
    public static final int FOR = 0x81;
    public static final int NEXT = 0x82;
    public static final int GOTO = 0xba;
    public static final int GOSUB = 0xbe;
    public static final int REM = 0xc0;
    /// The full Business BASIC token table. The table generally begins at `$80`.
    /// Any of the values above `$FF` in the table are addressed by a preceding
    /// `$FF` in the program.
    public static final String[] TOKENS = {    // starts at $80
        // $00
        "END",      "FOR",      "NEXT",     "INPUT",    "OUTPUT",   "DIM",      "READ",     "WRITE",
        // $08
        "OPEN",     "CLOSE",    null,       "TEXT",     null,       "BYE",      null,       null,
        // $10
        null,       null,       null,       "WINDOW",   "INVOKE",   "PERFORM",  null,       null,
        // $18
        "FRE",      "HPOS",     "VPOS",     "ERRLIN",   "ERR",      "KBD",      "EOF",      "TIME$",
        // $20
        "DATE$",    "PREFIX$",  "EXFN.",    "EXFN%.",   "OUTREC",   "INDENT",   null,       null,
        // $28
        null,       null,       null,       null,       null,       "POP",      "HOME",     null,
        // $30
        "SUB$(",    "OFF",      "TRACE",    "NOTRACE",  "NORMAL",   "INVERSE",  "SCALE(",   "RESUME",
        // $38
        null,       "LET",      "GOTO",     "IF",       "RESTORE",  "SWAP",     "GOSUB",    "RETURN",
        // $40
        "REM",      "STOP",     "ON",       null,       "LOAD",     "SAVE",     "DELETE",   "RUN",
        // $48
        "RENAME",   "LOCK",     "UNLOCK",   "CREATE",   "EXEC",     "CHAIN",    null,       null,
        // $50
        null,       "CATALOG",  null,       null,       "DATA",     "IMAGE",    "CAT",      "DEF",
        // $58
        null,       "PRINT",    "DEL",      "ELSE",     "CONT",     "LIST",     "CLEAR",    "GET",
        // $60
        "NEW",      "TAB",      "TO",       "SPC(",     "USING",    "THEN",     null,       "MOD",
        // $68
        "STEP",     "AND",      "OR",       "EXTENSION","DIV",      null,       "FN",       "NOT",
        // $70
        null,       null,       null,       null,       null,       null,       null,       null,
        // $78
        null,       null,       null,       null,       null,       null,       null,       "tf7",
        // $80
        "TAB(",     "TO",       "SPC(",     "USING",    "THEN",     null,       "MOD",      "STEP",
        // $88
        "AND",      "OR",       "EXTENSION","DIV",      null,       "FN",       "NOT",      null,
        // $90
        null,       null,       null,       null,       null,       null,       null,       null,
        // $98
        null,       null,       null,       null,       "AS",       "SGN(",     "INT(",     "ABS(",
        // $A0
        null,       "TYP(",     "REC(",     null,       null,       null,       null,       null,
        // $A8
        null,       null,       null,       null,       null,       "PDL(",     "BUTTON(",  "SQR(",
        // $B0
        "RND(",     "LOG(",     "EXP(",     "COS(",     "SIN(",     "TAN(",     "ATN(",     null,
        // $B8
        null,       null,       null,       null,       null,       null,       null,       null,
        // $C0
        null,       null,       null,       "STR$(",    "HEX$(",    "CHR$(",    "LEN(",     "VAL(",
        // $C8
        "ASC(",     "TEN(",     null,       null,       "CONV(",    "CONV&(",   "CONV$(",   "CONV%(",
        // $D0
        "LEFT$(",   "RIGHT$(",  "MID$(",    "INSTR(",   null,       null,       null,       null
    };

    private final DataBuffer program;

    public BusinessBASICConverter(FileEntry fileEntry) {
        ProdosAttributes attributes = fileEntry.getProdosAttributes();
        this.program = attributes.dataFork().orElseGet(fileEntry::getDataFork);
    }
    public BusinessBASICConverter(DataBuffer program) {
        this.program = program;
    }

    @Override
    public void generate(EventHandler handler) {
        // Uncertain what the 1st two bytes are, but we don't need to use them
        program.position(2);
        int nextOffset = 0;
        State state = State.START;
        boolean done = false;
        int indent = 0;
        boolean needsSpace = false;
        boolean inGotoGosub = false;
        while (program.hasRemaining() && !done) {
            switch (state) {
                case START -> {
                    nextOffset = program.readUnsignedByte();
                    if (nextOffset == 0) {
                        done = true;
                    }
                    else {
                        int lineNumber = program.readUnsignedShort();
                        handler.startOfLine(lineNumber + Strings.repeat(" ", indent+2));
                        state = State.CODE;
                        needsSpace = false;
                        inGotoGosub = false;
                    }
                }
                case CODE -> {
                    int value = program.readUnsignedByte();
                    if (value == 0) {
                        handler.endOfLine();
                        state = State.START;
                        continue;
                    }

                    String token = null;
                    if (value == 0xff) {
                        value = program.readUnsignedByte();
                        token = TOKENS[value];
                    }
                    else if (value >= 0x80) {
                        if (value == FOR) indent += 2;
                        if (value == NEXT) indent -= 2;
                        token = TOKENS[value & 0x7f];
                    }

                    final String skipSpace = ":=,;()+-*/^";
                    if (needsSpace && skipSpace.indexOf(value) == -1) {
                        handler.syntax(" ");
                    }
                    needsSpace = false;

                    if (value == REM) {
                        handler.comment(scanUntil(program, "REM", 0));
                        handler.endOfLine();
                        state = State.START;
                        needsSpace = true;
                    }
                    else if (value == GOTO || value == GOSUB) {
                        handler.token(token);
                        needsSpace = true;
                        inGotoGosub = true;
                    }
                    else if (token != null) {
                        handler.token(token);
                        needsSpace = !(token.endsWith("(") || token.endsWith("$"));
                    }
                    else if (value == '"') {
                        handler.string(scanUntil(program, "\"", '"'));
                    }
                    else if (isCharacter(value)) {
                        handler.identifier(scanWhile(program, value,
                                ch -> isCharacter(ch) || isDigit(ch) || ch == '$' || ch == '%'));
                        needsSpace = true;
                    }
                    else if (isDigit(value) || value == '.') {
                        if (inGotoGosub && isDigit(value)) {
                            handler.targetLine(scanWhile(program, value, Character::isDigit));
                        }
                        else {
                            handler.number(scanWhile(program, value, ch -> isDigit(ch) || ch == '.' || ch == '-'));
                        }
                        needsSpace = true;
                    }
                    else {
                        handler.syntax(Character.toString(value));
                        needsSpace = value == ')';
                        if (value != ',') inGotoGosub = false;
                    }
                }
            }
        }
    }

    private enum State {
        START,
        CODE
    }
}
