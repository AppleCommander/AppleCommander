echo off
prompt
if [%1] [] goto:BEGIN
goto:%1
:BEGIN
cls
echo ``
echo ``
echo ``
echo ``
echo ``
echo ` +--------------------------------+`
echo ` | A brief reference guide to the |`
echo ` | internal commands of US-DOS,   |`
echo ` | Version 1.10                   |`
echo ` +--------------------------------+`
echo ``
echo ``
echo ``
echo ``
echo ``
echo ``
echo ``
echo ``
echo ``
echo ``
echo ``
echo ``
echo `Press any key to continue.`
pause
break=on
cls
echo `  This version is distributed free`
echo `of charge together with several`
echo `additional programs that expand the`
echo `capabilities of the operating`
echo `system. The set of internal`
echo `commands described here is not`
echo `final and may be expanded in the`
echo `future to make corrections and`
echo `additions. If you are interested in`
echo `a particular command, enter its`
echo `name after HELP.`
echo `  A few points to note:`
echo `  * Parameters enclosed in square`
echo `    brackets [ ] are optional.`
echo `  * d: is the name of a logical`
echo `    drive: A:, B:, C:, or D:.`
echo `  * "Path" is a sequence of`
echo `    directory names.`
echo `  * "File" is the name of the file`
echo `    on which the operation will be`
echo `    performed.`
echo ``
echo `Press any key to continue.`
pause
cls
:*
echo `List of commands:`
echo ``
echo `BREAK  CD     CLS    COPY   DATE`
echo `DEL    DIR    d:     ECHO   GOTO`
echo `IF     INPUT  LOWER  MAKE   MD`
echo `PATH   PAUSE  PROMPT RD     REN`
echo `TYPE   UPPER  VER    VERIFY VOL`
if %1. *. goto:END
echo ``
echo `BREAK command`
echo `=============`
:break
:BREAK
echo `Format: BREAK=ON/OFF`
echo ``
echo `The BREAK command enables (ON) or`
echo `disables (OFF) interruption of BAT`
echo `files using CTRL-C. The normal`
echo `setting is BREAK=OFF.`
if %1. break. goto:END
if %1. BREAK. goto:END
echo ``
echo `CD command`
echo `==========`
:cd
:CD
echo `Format: CD [d:][\]<path>`
echo ``
echo `If "path" is omitted, the command -`
if %1. . echo `Press any key to continue.`
if %1. . pause
if %1. . cls
echo `The CD command displays the active`
echo `directory if no path is specified.`
echo `Otherwise, it sets a new directory.`
if %1. cd. goto:END
if %1. CD. goto:END
echo ``
echo `CLS command`
echo `===========`
:cls
:CLS
echo `Format: CLS`
echo ``
echo `The CLS command clears the screen.`
if %1. cls. goto:END
if %1. CLS. goto:END
echo ``
echo `DATE command`
echo `============`
:date
:DATE
echo `Format: DATE [DD-MM-YYYY]`
echo ``
echo `The DATE command is used to change`
echo `the system date. If no parameter is`
echo `given, the current date is shown,`
echo `and then a new date is entered.`
if %1. date. goto:END
if %1. DATE. goto:END
echo ``
echo `DEL command`
echo `===========`
:del
:DEL
echo `Format: DEL [d:][\][path]\[file]`
if %1. . echo `Press any key to continue.`
if %1. . pause
if %1. . cls
echo ``
echo `The DEL command deletes a file or`
echo `group of files. If "file" is *.*,`
echo `the entire diskette is erased,`
echo `regardless of the directory`
echo `specified.`
if %1. del. goto:END
if %1. DEL. goto:END
echo ``
echo `DIR command`
echo `===========`
echo ``
:dir
:DIR
echo `Format: DIR [d:][\][path]\[file]`
echo ``
echo `The DIR command displays the list`
echo `of files in a directory.`
if %1. dir. goto:END
if %1. DIR. goto:END
echo ``
echo `d: command`
echo `==========`
:d:
:D:
echo `Format: d:`
echo ``
echo `The d: command changes the default`
echo `disk drive.`
if %1. d:. goto:END
if %1. D:. goto:END
echo ``
echo `Press any key to continue.`
pause
cls
echo `ECHO command`
echo `============`
:echo
:ECHO
echo `Format: ECHO ON/OFF/message`
echo ``
echo `The ECHO command enables (ON) or`
echo `disables (OFF) the display of`
echo `system commands. It can also be`
echo `used to display messages enclosed`
echo `in two single quotation marks.`
if %1. echo. goto:END
if %1. ECHO. goto:END
echo ``
echo `GOTO command`
echo `============`
:goto
:GOTO
echo `Format: GOTO:label`
echo ``
echo `The GOTO command performs an`
echo `unconditional jump in a BAT file.`
echo `Control passes to a line preceded`
echo `by : that matches the label`
echo `specified in the GOTO command.`
if %1. goto. goto:END
if %1. GOTO. goto:END
echo ``
echo `IF command`
echo `==========`
echo `Press any key to continue.`
pause
cls
:if
:IF
echo `Format: IF string1 string2 command`
echo ``
echo `If string1 and string2 are equal,`
echo `command is executed.`
if %1. if. goto:END
if %1. IF. goto:END
echo ``
echo `INPUT command`
echo `=============`
:input
:INPUT
echo `Format: INPUT`
echo ``
echo `The INPUT command waits for the`
echo `user to enter a text from the`
echo `keyboard. It then assigns each`
echo `string in the entered line, to the`
echo `conditional variables string1,`
echo `string2, etc. The strings are`
echo `separated by spaces. The command is`
echo `used only in BAT files, where a`
echo `conditional variable is replaced by`
echo `its value when encountered. When a`
echo `BAT file starts, the conditional`
echo `variables are set to the file name`
echo `and the parameters that follow it.`
if %1. input. goto:END
if %1. INPUT. goto:END
echo `Press any key to continue.`
pause
cls
echo `LOWER command`
echo `=============`
:lower
:LOWER
echo `Format: LOWER`
echo ``
echo `All messages issued by the system`
echo `are displayed using uppercase and`
echo `lowercase letters.`
if %1. lower. goto:END
if %1. LOWER. goto:END
echo ``
echo `MAKE command`
echo `============`
:make
:MAKE
echo `Format: MAKE [d:][\][path\]file`
echo ``
echo `The MAKE command lets you enter a`
echo `text file using the keyboard and`
echo `save it to a floppy disk. Press`
echo `CTRL-C to end input.`
if %1. make. goto:END
if %1. MAKE. goto:END
echo ``
echo `MD command`
echo `==========`
:md
:MD
echo `Format: MD [d:][\][/path]file`
echo ``
if %1. . echo `Press any key to continue.`
if %1. . pause
if %1. . cls
echo `The MD command creates a directory`
echo `named "name."`
if %1. md. goto:END
if %1. MD. goto:END
echo ``
echo `PATH command`
echo `============`
:path
:PATH
echo `Format: PATH [d:][path\];[d:]...`
echo ``
echo `The PATH command sets the search`
echo `path for external commands that are`
echo `not found in the current directory.`
if %1. path. goto:END
if %1. PATH. goto:END
echo ``
echo `PAUSE command`
echo `=============`
:pause
:PAUSE
echo `Format: PAUSE`
echo ``
echo `The PAUSE command stops the system`
echo `until a key is pressed.`
if %1. pause. goto:END
if %1. PAUSE. goto:END
echo ``
echo `PROMPT command`
echo `==============`
:prompt
:PROMPT
echo `Format: PROMPT [parameter]`
echo ``
if %1. . echo `Press any key to continue.`
if %1. . pause
if %1. . cls
echo `The PROMPT command changes the`
echo `operating system’s prompt symbol.`
echo `The command parameter can include`
echo `text and special codes, with the`
echo `following meanings:`
echo `  $B-";" symbol; $C-new line`
echo `  $D-system date; $G-">" symbol`
echo `  $H-backspace; $L-"<" symbol`
echo `  $N-active drive; $S-space`
echo `  $P-active directory; $^-bell`
echo `  $U-move up ; $Z-clear screen`
echo `  $$-"$" symbol; $V-US-DOS version`
echo `Usually, the PROMPT parameter is`
echo `$C$N$G.`
if %1. prompt. goto:END
if %1. PROMPT. goto:END
echo ``
echo `RD command`
echo `==========`
:rd
:RD
echo `Format: RD [d:][\][path\]file`
echo ``
echo `The RD command removes the empty`
echo `directory named "name."`
if %1. rd. goto:END
if %1. RD. goto:END
echo `Press any key to continue.`
pause
cls
echo `REN command`
echo `===========`
:ren
:REN
echo `Format: REN [d:][path\]name1 name2`
echo ``
echo `The REN command renames the file`
echo `"name1" to "name2."`
if %1. ren. goto:END
if %1. REN. goto:END
echo ``
echo `TYPE command`
echo `============`
:type
:TYPE
echo `Format: TYPE [d:][path\]file`
echo ``
echo `The TYPE command displays the`
echo `contents of a file in ASCII code.`
if %1. type. goto:END
if %1. TYPE. goto:END
echo ``
echo `UPPER command`
echo `=============`
:upper
:UPPER
echo `Format: UPPER`
echo ``
echo `The UPPER command makes all`
echo `messages from the system appear in`
echo `uppercase letters.`
if %1. upper. goto:END
if %1. UPPER. goto:END
echo ``
echo `Press any key to continue.`
pause
cls
echo `VER command`
echo `===========`
:ver
:VER
echo `Format: VER`
echo ``
echo `Displays the US-DOS version.`
if %1. ver. goto:end
if %1. VER. goto:end
echo ``
echo `VERIFY command`
echo `==============`
:verify
:VERIFY
echo `Format: VERIFY ON/OFF`
echo ``
echo `The VERIFY command turns on (ON)`
echo `or turns off (OFF) the checking of`
echo `information after it is written.`
echo `The normal setting is VERIFY ON.`
if %1. verify. goto:END
if %1. VERIFY. goto:END
echo ``
echo `VOL command`
echo `===========`
:vol
:VOL
echo `Format: VOL [d:]`
echo ``
echo `The VOL command displays the`
echo `volume name of drive d:.`
if %1. vol. goto:END
if %1. VOL. goto:END
echo ``
echo `Press any key to continue.`
pause
cls
echo `You can include explanatory`
echo `comments in a BAT file; they won’t`
echo `be executed when ECHO OFF is set.`
echo `To do this, leave a space at the`
echo `beginning of the line.`
goto:END
:%1
echo `Command %1 does not exist!`
echo `Enter %0 * to see the list`
echo `of commands.`
:END
:end

 Teodor Angelov Yordanov
 Bulgaria

echo ``
prompt $c$n$g
echo on
