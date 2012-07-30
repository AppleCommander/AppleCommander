;*********************************************************
;                                                        *
; APPLECOMMANDER boot code.                              *
; Copyright (c) 2002, Rob Greene                         *
;                                                        *
; This code is published under the GPL license.  See the *
; AppleCommander site for license information.           *
;                                                        *
;*********************************************************

; Assemble this code with the cc65 toolchain:
;
; cl65 AppleCommander-boot.s -t none --start-addr $0800
; then copy resulting AppleCommander-boot to:
; .../src/com/webcodepro/applecommander/storage/AppleCommander-boot.dump
;

; Define as ASCII string with no attributes
.macro  asc Arg
	.repeat	.strlen(Arg), I
	.byte	.strat(Arg, I) | $80
	.endrep
.endmacro

; Define as ASCII string with trailing CR
.macro	asccr Arg
	.repeat	.strlen(Arg), I
	.byte	.strat(Arg, I) | $80
	.endrep
	.byte   $8d
.endmacro

.org $0800
;
; Zero page variables.  Used by Apple ][ ROM routines
; as well as this code.
;
XEND = $2C
ADDR = $26
;
; Keyboard switches
;
KEYBOARD = $C000
KEYCLEAR = $C010
;
; Disk ][ interface locations
;
MOTOROFF = $C088
;
; General Apple ][ ROM locations
;
TEXT = $FB2F
HOME = $FC58
GR = $FB40
COLOR = $F864
HLIN = $F819
PRINT = $FDF0
REBOOT = $FAA6
CALCADDR = $F847
DELAY = $FCA8
;
; Image offset locations (from upper-left).
; Note that, due to space, the XOFFSET isn't
; currently used - but, because of the rotating
; image, it doesn't really matter!
;
XOFFSET = 14
YOFFSET = 13
;
; The boot rom (probably $C600) uses the first byte
; to indicate the number of sectors to load.  Normally
; this is just one sector, so this program is constrained
; to 256 bytes - just in case of compatibility problems.
; This also avoids problems with sector ordering.
; Zero is always zero!
;
.byte 1
;
; General setup.
;
	LDA MOTOROFF,X
	JSR TEXT
	JSR HOME
	JSR GR
;
; Draw the AppleCommander logo (well, sorta).
; DATA1 and DATA2 contain 4 values - the color value,
; the xstart (start of line), xend (end of line), as
; well as the Y position.
;
	LDX #DATA2-DATA1
LOGO:
	LDA DATA1-1,X
	LSR
	LSR
	LSR
	LSR
	JSR COLOR
	LDA DATA2-1,X
	LSR
	LSR
	LSR
	LSR
	STA XEND
	LDA DATA1-1,X
	AND #$F
	TAY
	LDA DATA2-1,X
	AND #$F
	CLC
	ADC #YOFFSET
	JSR HLIN
	DEX
	BNE LOGO
;
; Display AppleCommander message.
;
DISPMSG:
	LDA MESSAGE,X
	BEQ WAIT
	JSR PRINT
	INX
	BNE DISPMSG
;
; Check for a keypress
;
WAIT:
	LDA KEYBOARD
	BPL SETUP
	STA KEYCLEAR
	JMP REBOOT
;
; Rotate the screen (isn't that retro)!
;
SETUP:
	LDX #19
ROTATE:
	TXA
	JSR CALCADDR
	LDY #0
	LDA (ADDR),Y
	PHA
SHIFT:
	INY
	LDA (ADDR),Y
	DEY
	STA (ADDR),Y
	INY
	CPY #39
	BNE SHIFT
	PLA
	STA (ADDR),Y
	DEX
	BPL ROTATE
;
; Introduce a pause between rotations.
;
KEYLOOP:
	LDA #$08
	JSR DELAY
	DEX
	BNE KEYLOOP
	BEQ WAIT
;
; The image data codes the upper nybble with one
; value and the lower nybble with the second value
; in an effort to conserve space.  Thus, 17 HLINs
; are stored in 34 bytes instead of 68.
;
; DATA1 consists of color and x1 (start) position.
;
DATA1:
	.byte $C8, $C7, $C6, $C3, $C8, $C2 ; green
	.byte $D1, $D1 ; yellow
	.byte $90, $90 ; orange
	.byte $10, $10 ; red
	.byte $31, $31 ; purple
	.byte $62, $63, $68 ; blue
;
; DATA2 consists of x2 (end) and y position.
;
DATA2:
	.byte $90, $81, $72, $53, $B3, $C4
	.byte $D5, $D6
	.byte $C7, $B8
	.byte $B9, $CA
	.byte $DB, $DC
	.byte $CD, $5E, $BE
;
; Text message to display at bottom of screen.
;
MESSAGE:
	asccr "THIS DISK CREATED WITH APPLECOMMANDER"
	asccr "VISIT APPLECOMMANDER.SF.NET"
	.byte $8d
	asc "INSERT ANOTHER DISK AND PRESS ANY KEY"
	.byte $00
