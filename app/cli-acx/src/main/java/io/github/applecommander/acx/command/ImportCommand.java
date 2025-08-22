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
package io.github.applecommander.acx.command;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;
import com.webcodepro.applecommander.util.AppleUtil;
import com.webcodepro.applecommander.util.StreamUtil;
import com.webcodepro.applecommander.util.TranslatorStream;
import com.webcodepro.applecommander.util.readerwriter.FileEntryReader;
import com.webcodepro.applecommander.util.readerwriter.OverrideFileEntryReader;
import com.webcodepro.shrinkit.HeaderBlock;
import com.webcodepro.shrinkit.NuFileArchive;
import com.webcodepro.shrinkit.ThreadRecord;
import io.github.applecommander.acx.base.ReadWriteDiskCommandOptions;
import io.github.applecommander.acx.converter.IntegerTypeConverter;
import io.github.applecommander.acx.fileutil.FileUtils;
import io.github.applecommander.applesingle.AppleSingle;
import io.github.applecommander.applesingle.FileDatesInfo;
import io.github.applecommander.applesingle.ProdosFileInfo;
import io.github.applecommander.bastools.api.Configuration;
import io.github.applecommander.bastools.api.Parser;
import io.github.applecommander.bastools.api.TokenReader;
import io.github.applecommander.bastools.api.Visitors;
import io.github.applecommander.bastools.api.model.Program;
import io.github.applecommander.bastools.api.model.Token;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

@Command(name = "import", description = "Import file onto disk.",
         aliases = { "put" })
public class ImportCommand extends ReadWriteDiskCommandOptions {
    private static Logger LOG = Logger.getLogger(ImportCommand.class.getName());

    @ArgGroup(heading = "%nInput source:%n", multiplicity = "1")
    private InputData inputData;

    @ArgGroup(heading = "%nProcessing options:%n")
    private Processor processor = new Processor();
    
    @ArgGroup(heading = "%nGeneral overrides:%n", exclusive = false)
    private Overrides overrides = new Overrides();
    
    @Option(names = { "--dir" }, description = "Write file(s) to directory.")
    private Optional<String> directoryName;
    
    @Option(names = { "-f", "--force" }, description = "Over-write existing files.")
    private boolean overwriteFlag;

    @Override
    public int handleCommand() throws Exception {
        final int MAX_ADDRESS = 0xFFFF;
        final int MAX_AUXTYPE = 0xFFFF;

        if (overrides.fileAddress.isPresent()){
            final int fileAddress = overrides.fileAddress.get();
            if (fileAddress < 0 || fileAddress > MAX_ADDRESS) {
                String errormsg = String.format("address(%d) is out of range(0-%d).", fileAddress, MAX_ADDRESS);
                throw new IllegalArgumentException(errormsg);
            }
        }

       if (overrides.auxType.isPresent()){
            final int auxType = overrides.auxType.get();
            if (auxType < 0 || auxType > MAX_AUXTYPE) {
                String errormsg = String.format("auxiliary type(%d) is out of range(0-%d).", auxType, MAX_AUXTYPE);
                throw new IllegalArgumentException(errormsg);
            }
        }

        DirectoryEntry directory = disk.getFormattedDisks()[0];
        if (directoryName.isPresent()) {
            String[] dirs = directoryName.get().split("/");
            for (String dir : dirs) {
                Optional<FileEntry> fileEntry = directory.getFiles().stream()
                        .filter(f -> dir.equalsIgnoreCase(f.getFilename()))
                        .filter(f -> !f.isDeleted())
                        .findFirst();
                Optional<DirectoryEntry> dirEntry = fileEntry
                        .filter(FileEntry::isDirectory)
                        .map(DirectoryEntry.class::cast);
                directory = dirEntry.orElseThrow(() -> 
                    new IOException(String.format("Directory '%s' not found.", dir)));
            }
        }

        FileUtils copier = new FileUtils(overwriteFlag);
        FileEntryReader inputReader = inputData.get();
        for (FileEntryReader processorReader : processor.apply(inputReader)) {
            FileEntryReader reader = OverrideFileEntryReader.builder()
                    .filename(overrides.fileName)
                    .prodosFiletype(overrides.fileType)
                    .binaryAddress(overrides.fileAddress)
                    .auxiliaryType(overrides.auxType)
                    .build(processorReader);
            
            copier.copyFile(directory, reader);
        }
        
        return 0;
    }

    public static class InputData {
        private FileEntryReader fileEntryReader;
        
        public FileEntryReader get() {
            return fileEntryReader;
        }

        @Option(names = { "--stdin" }, description = "Import from standard input.")
        public void stdinFlag(boolean flag) {
            try {
                byte[] data = System.in.readAllBytes();
                fileEntryReader = OverrideFileEntryReader.builder()
                        .fileData(data)
                        .filename("UNKNOWN")
                        .prodosFiletype("BIN")
                        .build();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        
        @Parameters(description = "File to import.")
        public void fromFile(final String filename) {
            try {
                Path path = Path.of(filename);
                byte[] data = Files.readAllBytes(path);
                fileEntryReader = OverrideFileEntryReader.builder()
                        .fileData(data)
                        .filename(path.getFileName().toString())
                        .prodosFiletype("BIN")
                        .build();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public static class Processor {
        private Function<FileEntryReader,List<FileEntryReader>> fileEntryReaderFn = this::handleRawFile;

        public List<FileEntryReader> apply(FileEntryReader reader) {
            return fileEntryReaderFn.apply(reader);
        }

        @Option(names = { "--raw", "--binary" }, description = { "Import as a raw binary file." })
        public void setRawBinaryMode(boolean flag) {
            fileEntryReaderFn = this::handleRawFile;
        }

        @Option(names = { "--text", "--text-high" }, description = { 
                "Import as a text file, setting high bit and ",
                "replacing newline characters with $8D." })
        public void setTextModeSetHighBit(boolean textFlag) {
            fileEntryReaderFn = this::handleTextModeSetHighBit;
        }

        @Option(names = { "--text-low" }, description = { 
                "Import as a text file, clearing high bit and ",
                "replacing newline characters with $0D." })
        public void setTextModeClearHighBit(boolean textFlag) {
            fileEntryReaderFn = this::handleTextModeClearHighBit;
        }
        
        @Option(names = { "--dos" }, description = "Use standard 4-byte DOS header.")
        public void setDosMode(boolean dosFlag) {
            fileEntryReaderFn = this::handleDosMode;
        }
        
        @Option(names = { "--geos" }, description = "Interpret as a GEOS conversion file.")
        public void setGeosMode(boolean geosFlag) {
            fileEntryReaderFn = this::handleGeosMode;
        }
        
        @Option(names = { "--basic" }, description = "Tokenize an AppleSoft BASIC program.")
        public void setApplesoftTokenizerMode(boolean tokenizeMode) {
            fileEntryReaderFn = this::handleApplesoftTokenizeMode;
        }
        
        @Option(names = { "--as", "--applesingle" }, description = "Import Apple Single file.")
        public void setAppleSingleMode(boolean applesingleMode) {
            fileEntryReaderFn = this::handleAppleSingleMode;
        }
 
        @Option(names = { "--shk", "--nufx", "--shrinkit", "--bxy" }, 
                description = "Import files from SHK archive.")
        public void setShrinkitMode(boolean shrinkitMode) {
            fileEntryReaderFn = this::handleShrinkitMode;
        }

        private List<FileEntryReader> handleRawFile(FileEntryReader reader) {
            return Arrays.asList(reader);
        }

        private List<FileEntryReader> handleTextModeSetHighBit(FileEntryReader reader) {
            InputStream inputStream = new ByteArrayInputStream(reader.getFileData().get());
            return handleTextMode(TranslatorStream.builder(inputStream)
                    .lfToCr().setHighBit().get(), reader);
        }
        private List<FileEntryReader> handleTextModeClearHighBit(FileEntryReader reader) {
            InputStream inputStream = new ByteArrayInputStream(reader.getFileData().get());
            return handleTextMode(TranslatorStream.builder(inputStream)
                    .lfToCr().clearHighBit().get(), reader);
        }
        private List<FileEntryReader> handleTextMode(InputStream inputStream, FileEntryReader reader) {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                StreamUtil.copy(inputStream, outputStream);
                final byte[] translatedData = outputStream.toByteArray();
                return OverrideFileEntryReader.builder()
                        .fileData(translatedData)
                        .prodosFiletype("TXT")
                        .buildList(reader);
            } catch (IOException cause) {
                throw new UncheckedIOException(cause);
            }
        }

        private List<FileEntryReader> handleDosMode(FileEntryReader reader) {
            try {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(reader.getFileData().get());
                byte[] header = new byte[4];
                if (inputStream.read(header) != 4) {
                    throw new IOException("Unable to read DOS header.");
                }
                int address = AppleUtil.getWordValue(header, 0);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                inputStream.transferTo(outputStream);
                return OverrideFileEntryReader.builder()
                        .fileData(outputStream.toByteArray())
                        .binaryAddress(address)
                        .buildList(reader);
            } catch (IOException cause) {
                throw new UncheckedIOException(cause);
            }
        }

        private List<FileEntryReader> handleGeosMode(FileEntryReader reader) {
            return OverrideFileEntryReader.builder()
                    .prodosFiletype("GEO")
                    .binaryAddress(0)
                    .buildList(reader);
        }
        
        private List<FileEntryReader> handleApplesoftTokenizeMode(FileEntryReader reader) {
            try {
                File fakeTempSource = File.createTempFile("ac-", "bas");
                fakeTempSource.deleteOnExit();
                Configuration config = Configuration.builder().sourceFile(fakeTempSource).build();
                Queue<Token> tokens = TokenReader.tokenize(new ByteArrayInputStream(reader.getFileData().get()));
                Parser parser = new Parser(tokens);
                Program program = parser.parse();
                byte[] tokenData = Visitors.byteVisitor(config).dump(program);
                return OverrideFileEntryReader.builder()
                        .fileData(tokenData)
                        .prodosFiletype("BAS")
                        .binaryAddress(config.startAddress)
                        .buildList(reader);
            } catch (IOException cause) {
                throw new UncheckedIOException(cause);
            }
        }
        
        private List<FileEntryReader> handleAppleSingleMode(FileEntryReader reader) {
            try {
                AppleSingle as = AppleSingle.read(reader.getFileData().get());
                if (as.getProdosFileInfo() == null) {
                    throw new IOException("This AppleSingle does not contain a ProDOS file.");
                }
                if (as.getDataFork() == null || as.getDataFork().length == 0) {
                    throw new IOException("This AppleSingle does not contain a data fork.");
                }
                ProdosFileInfo info = as.getProdosFileInfo();
                String fileType = ProdosFormatDisk.getFiletype(info.getFileType());

                OverrideFileEntryReader.Builder builder = OverrideFileEntryReader.builder();
                builder.filename(Optional.ofNullable(as.getRealName()));
                builder.prodosFiletype(fileType);
                builder.locked((info.getAccess()&0xc2) != 0xc2); //Unlocked if destroy, rename and write are all enabled
                builder.fileData(as.getDataFork());
                builder.resourceData(Optional.ofNullable(as.getResourceFork()));
                if (ProdosFormatDisk.fileTypeNeedsAddress(fileType)) {
                    builder.binaryAddress(info.getAuxType());
                }
                builder.auxiliaryType(info.getAuxType());
                
                if (as.getFileDatesInfo() != null) {
                    FileDatesInfo dates = as.getFileDatesInfo();
                    builder.creationDate(Date.from(dates.getCreationInstant()));
                    builder.lastModificationDate(Date.from(dates.getModificationInstant()));
                }
                
                return builder.buildList(reader);
            } catch (IOException cause) {
                throw new UncheckedIOException(cause);
            }
        }
        
        private List<FileEntryReader> handleShrinkitMode(FileEntryReader reader) {
            try {
                List<FileEntryReader> files = new ArrayList<>();
                ByteArrayInputStream inputStream = new ByteArrayInputStream(reader.getFileData().get());
                NuFileArchive nufx = new NuFileArchive(inputStream);
                for (HeaderBlock header : nufx.getHeaderBlocks()) {
                    OverrideFileEntryReader.Builder builder = OverrideFileEntryReader.builder()
                            .filename(header.getFilename())
                            .prodosFiletype(ProdosFormatDisk.getFiletype((int)header.getFileType()))
                            .auxiliaryType((int)header.getExtraType())
                            .creationDate(header.getCreateWhen())
                            .lastModificationDate(header.getModWhen());
                    
                    ThreadRecord dataFork = header.getDataForkThreadRecord();
                    ThreadRecord resourceFork = header.getResourceForkThreadRecord();
                    if (dataFork == null) {
                        LOG.info(() -> String.format("No data fork for '%s', skipping it.", 
                                header.getFilename()));
                        continue;
                    }
                    
                    builder.fileData(dataFork.getBytes());
                    if (resourceFork != null) {
                        builder.resourceData(resourceFork.getBytes());
                    }
                    files.add(builder.build());
                }
                return files;
            } catch (IOException cause) {
                throw new UncheckedIOException(cause);
            }
        }
    }
    
    public static class Overrides {
        @Option(names = { "-t", "--type" }, description = "ProDOS or native file type.  "
                + "(Each filesystem translates between it's native types and ProDOS.)")
        private Optional<String> fileType;
        
        @Option(names = { "-a", "--addr" }, description = "File address. " 
                + "(Note that address may only be set on file types that use address.)", 
                converter = IntegerTypeConverter.class)
        private Optional<Integer> fileAddress;
        
        @Option(names = { "-n", "--name" }, description = "File name.")
        private Optional<String> fileName;
        
        @Option(names = { "--aux", "--auxtype" }, description = "Aux. Type. "
                + "(For a filesystem that supports aux type.)",
                converter = IntegerTypeConverter.class)
        private Optional<Integer> auxType;
    }
}
