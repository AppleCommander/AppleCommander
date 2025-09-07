package com.webcodepro.applecommander.storage.filters;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import org.applecommander.util.DataBuffer;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * List an MBASIC/GBASIC program from a CP/M disk.
 * This is a process of discovery and may not be _entirely_ correct.
 * But, we can hexdump the saved program and with emulators run CP/M and do a LIST there.
 *
 * @see <a href="https://github.com/z88dk/techdocs/blob/master/targets/apple2/gbasic.asm">GBASIC assembly</a>
 */
public class MBASICFileFilter implements FileFilter {
    public static final Map<Integer,Token> TOKENS;
    static {
        TOKENS = new HashMap<>();
        for (Token token : Token.values()) {
            TOKENS.put(token.value(), token);
        }
    }

    @Override
    public byte[] filter(FileEntry fileEntry) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(out, true);

        DataBuffer code = DataBuffer.wrap(fileEntry.getFileData());
        code.readUnsignedByte();        // ignore the first byte
        State state = State.LINE_START;
        while (state != State.END && code.hasRemaining()) {
            state = switch (state) {
                case LINE_START -> {
                    if (code.readUnsignedShort() == 0) {
                        yield State.END;
                    }
                    int lineNbr = code.readUnsignedShort();
                    pw.printf("%d ", lineNbr);
                    yield State.TOKEN;
                }
                case TOKEN -> {
                    int value = code.readUnsignedByte();
                    if (value == 0) {
                        pw.println();
                        yield State.LINE_START;
                    }
                    if (value == 11) {
                        // $0B - EMBEDED OCTAL CONSTANT
                        pw.printf("&O%o", code.readUnsignedShort());
                    }
                    else if (value == 12) {
                        // $0C - EMBEDED HEX CONSTANT
                        pw.printf("&H%X", code.readUnsignedShort());
                    }
                    else if (value == 13) {
                        // $0D - A LINE NUMBER UNCONVERTED TO POINTER
                        pw.printf("%d", code.readUnsignedShort());
                    }
                    else if (value == 14) {
                        // $0E - A LINE NUMBER UNCONVERTED TO POINTER
                        pw.printf("%d", code.readUnsignedShort());
                    }
                    else if (value == 15) {
                        // $0F - SINGLE BYTE (TWO BYTE WITH TOKEN) INTEGER
                        pw.printf("%d", code.readUnsignedByte());
                    }
                    else if (value >= 17 && value < 27) {
                        // $11-$1B - FIRST OF 10 (0-9) INTEGER SPECIAL TOKENS
                        pw.printf("%d", value-17);
                    }
                    else if (value == 28) {
                        // $1C - REGULAR 16 BIT TWO'S COMPLEMENT INT
                        pw.printf("%d", code.readUnsignedShort());
                    }
                    else if (value == 0x3a) {
                        int pos = code.position();
                        int token2 = code.getUnsignedByte(pos);
                        int token3 = code.getUnsignedByte(pos+1);
                        // Comments seem to be a ":REM'" (3A 8F EA) but only show as "'"...
                        if (token2 == Token.REM.value() && token3 == Token.apos.value()) {
                            pw.print(Token.apos.text());
                            code.readUnsignedByte();    // skip the REM token
                            code.readUnsignedByte();    // skip the apostrophe token
                        }
                        // IF ELSE ... seems to be "IF :ELSE", so detect ":ELSE" and just show "ELSE"
                        else if (token2 == Token.ELSE.value()) {
                            pw.print(Token.ELSE.text());
                            code.readUnsignedByte();    // skip over ELSE token
                        }
                        else {
                            pw.print(":");
                        }
                    }
                    else {
                        if (value == 0x07) {
                            pw.print("<BEL>");
                        }
                        else {
                            if (value == 0xff) {
                                value = 0xff00 | code.readUnsignedByte();
                            }
                            Token token = TOKENS.get(value);
                            if (token == null) {
                                pw.print((char) value);
                            } else {
                                pw.print(token.text());
                            }
                        }
                    }
                    yield State.TOKEN;
                }
                // LOL. Java requires all possibilities. The IDE complains that this is unreachable. <shrug>
                case END -> State.END;
            };
        }
        return out.toByteArray();
    }

    @Override
    public String getSuggestedFileName(FileEntry fileEntry) {
        String filename = fileEntry.getFilename();
        if (filename.toLowerCase().endsWith(".bas")) {
            return filename;
        }
        return String.format("%s.bas", filename);
    }

    private enum State {
        LINE_START,
        TOKEN,
        END
    }

    public enum Token {
        // Function tokens, all follow $FF and have high bit set
        LEFT(0xff81, "LEFT$"),
        RIGHT(0xff82, "RIGHT$"),
        MID(0xff83, "MID$"),
        SGN(0xff84),
        INT(0xff85),
        ABS(0xff86),
        SQR(0xff87),
        RND(0xff88),
        SIN(0xff89),
        LOG(0xff8a),
        EXP(0xff8b),
        COS(0xff8c),
        TAN(0xff8d),
        ATN(0xff8e),
        FRE(0xff8f),

        POS(0xff90),
        LEN(0xff91),
        STR(0xff92, "STR$"),
        VAL(0xff93),
        ASC(0xff94),
        CHR(0xff95, "CHR$"),
        PEEK(0xff96),
        SPACE(0xff97, "SPACE$"),
        OCT(0xff98, "OCT$"),
        HEX(0xff99, "HEX$"),
        LPOS(0xff9a),
        CINT(0xff9b),
        CSGN(0xff9c),
        CDBL(0xff9d),
        FIX(0xff9e),

        CVI(0xffaa),
        CVS(0xffab),
        CVD(0xffac),
        EOF(0xffae),
        LOC(0xffaf),

        LOF(0xffb0),
        MKI(0xffb1, "MKI$"),
        MKS$(0xffb2),
        MKD$(0xffb3),
        VPOS(0xffb4),
        PDL(0xffb5),
        BUTTONfn(0xffb6, "BUTTON"),

        // Normal tokens; all > $80
        END(0x81),
        FOR(0x82),
        NEXT(0x83),
        DATA(0x84),
        INPUT(0x85),
        DIM(0x86),
        READ(0x87),
        LET(0x88),
        GOTO(0x89),
        RUN(0x8a),
        IF(0x8b),
        RESTORE(0x8c),
        GOSUB(0x8d),
        RETURN(0x8e),
        REM(0x8f),

        STOP(0x90),
        PRINT(0x91),
        CLEAR(0x92),
        LIST(0x93),
        NEW(0x94),
        ON(0x95),
        DEF(0x96),
        POKE(0x97),
        CONT(0x98),
        LPRINT(0x9b),
        LLIST(0x9c),
        WIDTH(0x9d),
        ELSE(0x9e),
        TRACE(0x9f),    // AKA TRON

        NOTRACE(0xa0),  // AKA TROFF
        SWAP(0xa1),
        ERASE(0xa2),
        EDIT(0xa3),
        ERROR(0xa4),
        RESUME(0xa5),
        DELETE(0xa6),
        AUTO(0xa7),
        RENUM(0xa8),
        DEFSTR(0xa9),
        DEFINT(0xaa),
        DEFSNG(0xab),
        DEFDBL(0xac),
        LINE(0xad),
        WHILE(0xaf),

        WEND(0xb0),
        CALL(0xb1),
        WRITE(0xb2),
        COMMON(0xb3),
        CHAIN(0xb4),
        OPTION(0xb5),
        RANDOMIZE(0xb6),
        SYSTEM(0xb7),
        OPEN(0xb8),
        FIELD(0xb9),
        GET(0xba),
        PUT(0xbb),
        CLOSE(0xbc),
        LOAD(0xbd),
        MERGE(0xbe),
        FILES(0xbf),

        NAME(0xc0),
        KILL(0xc1),
        LSET(0xc2),
        RSET(0xc3),
        SAVE(0xc4),
        RESET(0xc5),
        TEXT(0xc6),
        HOME(0xc7),
        VTAB(0xc8),
        HTAB(0xc9),
        INVERSE(0xca),
        NORMAL(0xcb),
        GR(0xcc),
        COLOR(0xcd),
        BUTTON(0xce),
        VLIN(0xcf),

        PLOT(0xd0),
        HGR(0xd1),
        HPLOT(0xd2),
        HCOLOR(0xd3),
        BEEP(0xd4),
        WAIT(0xd5),
        TO(0xdd),
        THEN(0xde),
        TAB(0xdf, "TAB("),

        STEP(0xe0),
        USR(0xe1),
        FN(0xe2),
        SPC(0xe3),
        NOT(0xe4),
        ERL(0xe5),
        ERR(0xe6),
        STRING$(0xe7),
        USING(0xe8),
        INSTR(0xe9),
        apos(0xea, "'"),     // comment
        VARPTR(0xeb),
        SCRN(0xec),
        HSCRN(0xed),
        INKEY$(0xee),
        gt(0xef, ">"),

        eq(0xf0, "="),
        lt(0xf1, "<"),
        plus(0xf2, "+"),
        minus(0xf3, "-"),
        star(0xf4, "*"),
        slash(0xf5, "/"),
        exponent(0xf6, "^"),
        AND(0xf7),
        OR(0xf8),
        XOR(0xf9),
        EQV(0xfa),
        IMP(0xfb),
        MOD(0xfc),
        bkslash(0xfd, "\\");

        private final int value;
        private final String text;

        Token(int value, String text) {
            this.value = value;
            this.text = text;
        }
        Token(int value) {
            this.value = value;
            this.text = null;
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
}
