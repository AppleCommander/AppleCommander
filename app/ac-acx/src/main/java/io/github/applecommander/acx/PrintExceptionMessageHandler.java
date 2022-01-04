package io.github.applecommander.acx;

import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

// Note: Taken from https://picocli.info/#_business_logic_exceptions
public class PrintExceptionMessageHandler implements IExecutionExceptionHandler {
    public int handleExecutionException(Exception ex,
                                        CommandLine cmd,
                                        ParseResult parseResult) {

        if (Main.enableStackTrace) {
            ex.printStackTrace(System.err);
        }
        else {
            // bold red error message
            cmd.getErr().println(cmd.getColorScheme().errorText(ex.getMessage()));
        }

        return cmd.getExitCodeExceptionMapper() != null
                    ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                    : cmd.getCommandSpec().exitCodeOnExecutionException();
    }
}