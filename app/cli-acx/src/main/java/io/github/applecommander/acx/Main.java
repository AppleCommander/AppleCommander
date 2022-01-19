/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2019-2022 by Robert Greene and others
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
package io.github.applecommander.acx;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import io.github.applecommander.acx.command.ConvertCommand;
import io.github.applecommander.acx.command.CopyFileCommand;
import io.github.applecommander.acx.command.CreateDiskCommand;
import io.github.applecommander.acx.command.DeleteCommand;
import io.github.applecommander.acx.command.DiskMapCommand;
import io.github.applecommander.acx.command.ExportCommand;
import io.github.applecommander.acx.command.ImportCommand;
import io.github.applecommander.acx.command.InfoCommand;
import io.github.applecommander.acx.command.ListCommand;
import io.github.applecommander.acx.command.LockCommand;
import io.github.applecommander.acx.command.MkdirCommand;
import io.github.applecommander.acx.command.RenameDiskCommand;
import io.github.applecommander.acx.command.RenameFileCommand;
import io.github.applecommander.acx.command.RmdirCommand;
import io.github.applecommander.acx.command.UnlockCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;

/**
 * Primary entry point into the 'acx' utility. 
 */
@Command(name = "acx", mixinStandardHelpOptions = true, versionProvider = VersionProvider.class,
    descriptionHeading = "%n",
    commandListHeading = "%nCommands:%n",
    optionListHeading = "%nOptions:%n",
    description = "'acx' experimental utility", 
    subcommands = {
            ConvertCommand.class,
            CopyFileCommand.class,
    		CreateDiskCommand.class,
    		DeleteCommand.class,
    		DiskMapCommand.class,
            ExportCommand.class,
            HelpCommand.class,
            ImportCommand.class,
            InfoCommand.class,
            ListCommand.class,
            LockCommand.class,
            MkdirCommand.class,
            RenameFileCommand.class,
            RenameDiskCommand.class,
            RmdirCommand.class,
            UnlockCommand.class
    })
public class Main {
    private static Logger LOG = Logger.getLogger(Main.class.getName());
    private static final Level LOG_LEVELS[] = { Level.OFF, Level.SEVERE, Level.WARNING, Level.INFO, 
            Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST };
    
    static {
    	System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s%n");
    	setAllLogLevels(Level.WARNING);
    }
    private static void setAllLogLevels(Level level) {
    	Logger rootLogger = LogManager.getLogManager().getLogger("");
		rootLogger.setLevel(level);
		for (Handler handler : rootLogger.getHandlers()) {
		    handler.setLevel(level);
		}
    }
    
    // This flag is read in PrintExceptionMessageHandler.
    @Option(names = { "--debug" }, description = "Show detailed stack traces.")
    static boolean enableStackTrace;
    
    @Option(names = { "-v", "--verbose" }, description = "Be verbose. Multiple occurrences increase logging.")
    public void setVerbosity(boolean[] flag) {
    	// The "+ 2" is due to the default of the levels
        int loglevel = Math.min(flag.length + 2, LOG_LEVELS.length);
        Level level = LOG_LEVELS[loglevel-1];
        setAllLogLevels(level);
    }
    
    @Option(names = { "--quiet" }, description = "Turn off all logging.")
    public void setQuiet(boolean flag) {
    	setAllLogLevels(Level.OFF);
    }

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new Main());
        cmd.setExecutionExceptionHandler(new PrintExceptionMessageHandler());
        cmd.setCaseInsensitiveEnumValuesAllowed(true);
        if (args.length == 0) {
            cmd.usage(System.out);
            System.exit(1);
        }
        
        LOG.info(() -> String.format("Log level set to %s.", Logger.getGlobal().getLevel()));
        int exitCode = cmd.execute(args);
        LOG.fine("Exiting with code " + exitCode);
        System.exit(exitCode);
    }
}
