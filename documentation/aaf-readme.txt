Apple Archive Format
Version 1.01

COPYRIGHT 1990 by Jonathan A. Chandross.
All Rights Reserved.

The AAF is a simple archive format designed for shipping around Apple
source code.  An archive consists of a series of lines.  Each line is
interpreted according to the first character of the line.  The
interpretations are:

	=       the name of the unpacked file starting here
	-       a source line to be unpacked into the current file
	+       end of archive.

Lines beginning with any other character are simply output to the console.

A simple example of this format:
	From: random@foobar.com
	To: freeworld
	Subject: First program.
			R.
	=helloworld.c
	-main()
	-{
	-       printf("Hello World\n");
	-}
	=Read.Me
	-Test out a C compiler; just compile and execute.
	+ All done.
	J. Random Hacker
	random@foobar.com

This file would create the files "helloworld.c" and "Read.Me".  When
the archive was unpacked, all of the non-source and non-file-name
lines would be output to the console until the '+' was encountered.

Using '=' to specify a file name and '-' to specify a line of source
allows a standard USENET or email file to be unpacked without removing
any preamble or trailing information.

The '+' at the end of the file indicates the end of an archive.  Lines
occurring past this point will not be processed at all.

The '=' and "+" sentinels are due to Doug Gwyn.


Jonathan A. Chandross
jac@paul.rutgers.edu
November 1990
