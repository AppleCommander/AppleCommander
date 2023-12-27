/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2019-2023 by Robert Greene and others
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
package io.github.applecommander.acx.command;

import io.github.applecommander.acx.PrintExceptionMessageHandler;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Command(name = "shell",
        descriptionHeading = "%n",
        commandListHeading = "%nCommands:%n",
        optionListHeading = "%nOptions:%n",
        description = "Enter 'acx' shell mode to support multiple commands.",
        subcommands = {
                CompareCommand.class,
                ConvertCommand.class,
                CopyFileCommand.class,
                CreateDiskCommand.class,
                DeleteCommand.class,
                DiskMapCommand.class,
                DumpCommand.class,
                ExportCommand.class,
                FindDuplicateFilesCommand.class,
                CommandLine.HelpCommand.class,
                ImportCommand.class,
                InfoCommand.class,
                ListCommand.class,
                LockCommand.class,
                MkdirCommand.class,
                ReadCommand.class,
                RenameFileCommand.class,
                RenameDiskCommand.class,
                RmdirCommand.class,
                UnlockCommand.class,
                WriteCommand.class
        })
public class ShellCommand implements Callable<Integer> {
    private static final String PROMPT = "ACX> ";

    private static boolean exit = false;
    @Command(name = "exit", aliases = { "quit" }, description = "Exit shell environment")
    public void exitShell() {
        exit = true;
    }

    private static String defaultDiskName = null;
    @Command(name = "default", description = "Display or set the default disk")
    public void defaultDisk(@Parameters(description = "Disk to be set as default", defaultValue = "") String diskName) {
        if (diskName == null || diskName.isEmpty()) {
            if (defaultDiskName == null) {
                System.out.println("No default disk has been set.");
            }
            else {
                System.out.printf("The default disk is '%s'.\n", defaultDiskName);
            }
        }
        else {
            // we don't know the commands being used, so can't really test for existence...
            defaultDiskName = diskName;
        }
    }

    @Override
    public Integer call() throws IOException {
        final boolean isTTY = (System.console() != null);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (!exit) {
                System.out.print(PROMPT);
                String line = reader.readLine();
                if (line == null) break;
                if (!isTTY) {
                    System.out.println(line);
                }

                CommandLine cmd = new CommandLine(new ShellCommand());
                cmd.setExecutionExceptionHandler(new PrintExceptionMessageHandler());
                cmd.setCaseInsensitiveEnumValuesAllowed(true);

                List<String> args = getArgs(line);
                if (defaultDiskName != null) {
                    System.setProperty("ACX_DISK_NAME", defaultDiskName);
                }
                cmd.execute(args.toArray(new String[0]));
            }
        }

        return 0;
    }

    private static List<String> getArgs(String line) {
        // See https://stackoverflow.com/questions/7804335/split-string-on-spaces-in-java-except-if-between-quotes-i-e-treat-hello-wor
        final Pattern splitter = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");

        Matcher m = splitter.matcher(line);
        List<String> args = new ArrayList<>();
        while (m.find()) {
            String s = m.group(1);
            if (s.startsWith("\"") && s.endsWith("\"")) {
                s = s.substring(1, s.length()-1);
            }
            args.add(s);
        }
        return args;
    }
}
