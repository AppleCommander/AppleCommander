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
package io.github.applecommander.acx.command;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.util.filestreamer.FileStreamer;
import com.webcodepro.applecommander.util.filestreamer.FileTuple;
import io.github.applecommander.acx.base.ReusableCommandOptions;
import io.github.applecommander.acx.converter.DiskConverter;
import org.applecommander.bastools.api.Configuration;
import org.applecommander.bastools.api.ModernTokenReader;
import org.applecommander.bastools.api.Parser;
import org.applecommander.bastools.api.model.Program;
import org.applecommander.bastools.api.model.Token;
import org.applecommander.bastools.api.proofreaders.*;
import picocli.CommandLine.*;
import picocli.CommandLine.Model.CommandSpec;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Perform "checking" of a program that was printed in a magazine.
 * Note the structure of AbstractProofCommand, HiddenProofCommand, and ProofCommand.
 */
@Command(name = "proof", description = "Proof-read/calculate program checksums (as printed in magazines) on file.")
public abstract class AbstractProofCommand extends ReusableCommandOptions {
    private static final Logger LOG = Logger.getLogger(AbstractProofCommand.class.getName());

    // This is different from the base options as the disk entry is optional.
    @Option(names = { "-d", "--disk" }, description = "Image to process [$ACX_DISK_NAME].",
            converter = DiskConverter.class, defaultValue = "${ACX_DISK_NAME}")
    private List<FormattedDisk> disks;

    @Option(names = { "-k", "--number" }, description = "Select disk number to access [$ACX_DISK_NUMBER].",
            defaultValue = "${ACX_DISK_NUMBER}")
    private Integer diskNumber;

    @Option(names = "--debug", description = "Print debug output.")
    private static boolean debugFlag;

    @Parameters(arity = "1", description = "Program name")
    private String programName;

    public int handle(Function<Configuration,Object> proofReaderFn) throws Exception {
        Configuration.Builder builder = Configuration.builder()
                .sourceFile(new File(programName));
        if (debugFlag) builder.debugStream(System.out);

        // Build is configured differently depending on if we find it in a disk image or as source.
        Optional<FileEntry> optFileEntry = findFileEntry();
        optFileEntry.ifPresent(fileEntry -> {
            builder.startAddress(fileEntry.getAddress());
        });

        Object checker = proofReaderFn.apply(builder.build());
        switch (checker) {
            case ApplesoftInputBufferProofReader inputBufferProofReader -> {
                inputBufferProofReader.addProgram(getProgram(optFileEntry));
            }
            case ApplesoftTokenizedProofReader tokenizedProofReader -> {
                if (optFileEntry.isPresent()) {
                    tokenizedProofReader.addBytes(optFileEntry.map(FileEntry::getFileData).orElseThrow());
                }
                else {  // We fall back to the source code if we don't have tokens
                    tokenizedProofReader.addProgram(getProgram(optFileEntry));
                }
            }
            default -> throw new RuntimeException("Unknown type of proofer/checker: " + checker);
        }
        return 0;
    }

    public Program getProgram(Optional<FileEntry> optFileEntry) throws IOException {
        File sourceFile = optFileEntry.map(fileEntry -> {
            LOG.warning("BASIC source generated from tokenized program. May not match proof-reader codes.");
            FileFilter filter = fileEntry.getSuggestedFilter();
            try {
                File tmpFile = File.createTempFile("proof-", ".bas");
                tmpFile.deleteOnExit();
                try (FileOutputStream out = new FileOutputStream(tmpFile)) {
                    out.write(filter.filter(fileEntry));
                }
                return tmpFile;
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }).orElse(new File(programName));

        Queue<Token> tokens = ModernTokenReader.tokenize(sourceFile);
        Parser parser = new Parser(tokens);
        return parser.parse();
    }

    public Optional<FileEntry> findFileEntry() {
        final List<String> basicTypes = List.of("A", "BAS");
        if (disks != null && !disks.isEmpty()) {
            return FileStreamer.forDisks(disks)
                    .matchGlobs(programName)
                    .stream()
                    .filter(FileTuple::isFile)
                    .map(tuple -> tuple.fileEntry)
                    .filter(fileEntry -> basicTypes.contains(fileEntry.getFiletype()))
                    .findFirst();
        }
        return Optional.empty();
    }

    @Command(hidden = true, name = "proof")
    public static class HiddenProofCommand extends AbstractProofCommand implements Callable<Integer> {
        public static final Map<String,Function<Configuration,Object>> PROOF_READER_FNS = Map.of(
                "apple-checker", NibbleAppleChecker::new,
                "checkit",       NibbleCheckit::new,
                "kp2",           MicrosparcKeyPerfect2::new,
                "key-perfect-2", MicrosparcKeyPerfect2::new,
                "kp4",           MicrosparcKeyPerfect4::new,
                "key-perfect-4", MicrosparcKeyPerfect4::new,
                "kp5",           MicrosparcKeyPerfect5::new,
                "key-perfect-5", MicrosparcKeyPerfect5::new,
                "proofreader",   ComputeAutomaticProofreader::new
        );

        @Spec
        private CommandSpec spec;

        @Override
        public int handleCommand() throws Exception {
            String commandName = spec.commandLine().getCommandName();
            Function<Configuration,Object> proofReaderFn = PROOF_READER_FNS.getOrDefault(commandName, key -> {
                throw new UnmatchedArgumentException(spec.commandLine(), "Unknown subcommand: " + key);
            });
            return handle(proofReaderFn);
        }
    }

    @Command(name = "proof", footer = {
            "",
            "* Note this command can be called with the tokenizer as the subcommand ('acx checkit' etc).",
            ""
    })
    public static class ProofCommand extends AbstractProofCommand implements Callable<Integer>  {
        @ArgGroup(heading = "%nTokenizer Selection:%n", multiplicity = "1")
        private final ProofReaderSelection proofReader = new ProofReaderSelection();

        @Override
        public int handleCommand() throws Exception {
            return handle(proofReader.proofReaderFn);
        }
    }

    public static class ProofReaderSelection {
        Function<Configuration,Object> proofReaderFn;

        @Option(names = "--checkit", description = "Apply Nibble Checkit (ca 1988) to code")
        public void selectNibbleCheckit(boolean flag) {
            this.proofReaderFn = NibbleCheckit::new;
        }

        @Option(names = "--proofreader", description = "Apply Compute! Apple Automatic Proofreader (ca 1985) to code")
        public void selectComputeProofreader(boolean flag) {
            this.proofReaderFn = ComputeAutomaticProofreader::new;
        }

        @Option(names = "--apple-checker", description = "Apply Nibble Apple Checker 3.0 (ca 1982) to code")
        public void selectNibbleAppleChecker(boolean flag) {
            this.proofReaderFn = NibbleAppleChecker::new;
        }

        @Option(names = { "--key-perfect-2", "--kp2" }, description = "Apply MicroSPARC Key Perfect V2 (ca 1981) to code")
        public void selectKeyPerfectV2(boolean flag) {
            this.proofReaderFn = MicrosparcKeyPerfect2::new;
        }

        @Option(names = { "--key-perfect-4", "--kp4" }, description = "Apply MicroSPARC Key Perfect V4 (ca 1981) to code")
        public void selectKeyPerfectV4(boolean flag) {
            this.proofReaderFn = MicrosparcKeyPerfect4::new;
        }

        @Option(names = { "--key-perfect-5", "--kp5" }, description = "Apply MicroSPARC Key Perfect V5 (ca 1985) to code")
        public void selectKeyPerfectV5(boolean flag) {
            this.proofReaderFn = MicrosparcKeyPerfect5::new;
        }
    }
}
