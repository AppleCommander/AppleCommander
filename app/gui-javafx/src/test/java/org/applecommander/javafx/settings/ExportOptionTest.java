package org.applecommander.javafx.settings;

import com.google.common.primitives.Bytes;
import org.applecommander.filestore.Directory;
import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.FileStore;
import org.applecommander.util.DataBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;

public class ExportOptionTest {
    @Test
    public void testRawBinaryDataForkOnly() throws IOException {
        exerciseExportOption(ExportOption.RAW_BINARY, Assertions::assertArrayEquals,
                "TESTFILE", List.of("TESTFILE"));
    }

    @Test
    public void testRawBinaryWithResourceFork() throws IOException {
        exerciseExportOption(ExportOption.RAW_BINARY, Assertions::assertArrayEquals,
                "TESTFILE", List.of("TESTFILE", "TESTFILE-rsrc"));
    }

    @Test
    public void testAppleSingleDataForkOnly() throws IOException {
        exerciseExportOption(ExportOption.APPLE_SINGLE, this::assertArrayContains,
                "TESTFILE", List.of("TESTFILE.as"));
    }

    @Test
    public void testAppleSingleWithResourceFork() throws IOException {
        exerciseExportOption(ExportOption.APPLE_SINGLE, this::assertArrayContains,
                "TESTFILE", List.of("TESTFILE.as"));
    }

    @Test
    public void testAttributePreservationDataForkOnly() throws IOException {
        exerciseExportOption(ExportOption.ATTRIBUTE_PRESERVATION, Assertions::assertArrayEquals,
                "TESTFILE", List.of("TESTFILE#TTAAAA.txt"));
    }

    @Test
    public void testAttributePreservationWithResourceFork() throws IOException {
        exerciseExportOption(ExportOption.ATTRIBUTE_PRESERVATION, Assertions::assertArrayEquals,
                "TESTFILE", List.of("TESTFILE#TTAAAA.txt", "TESTFILE_rsrc_#TTAAAA.txt"));
    }

    public void assertArrayContains(final byte[] expected, final byte[] actual) {
        assertTrue(actual.length > expected.length, "Expecting actual bytes to be longer than expected bytes");
        if (Bytes.indexOf(actual, expected) == -1) {
            fail("expected bytes are supposed to be contained in actual bytes");
        }
    }

    public void exerciseExportOption(ExportOption exportOption, BiConsumer<byte[],byte[]> arrayTest,
                                     String baseFilename, List<String> expectedFilenames) throws IOException {
        Objects.requireNonNull(exportOption, "exportOption must not be null");
        Objects.requireNonNull(baseFilename, "baseFilename must not be null");
        Objects.requireNonNull(expectedFilenames, "expectedFilenames must not be null");
        assert expectedFilenames.size() == 1 || expectedFilenames.size() == 2;

        Path directory = Files.createTempDirectory("test-");
        directory.toFile().deleteOnExit();

        final byte[] expectedDataFork = "THIS IS A DATA FORK".getBytes();
        final byte[] expectedResourceFork = "THIS IS A RESOURCE FORK".getBytes();

        TestFileEntry.Builder builder = TestFileEntry.builder()
                .name(baseFilename)
                .dataFork(expectedDataFork);
        if (expectedFilenames.size() > 1) {
            builder.resourceFork(expectedResourceFork);
        }
        FileEntry fileEntry = builder.get();

        List<Path> results = exportOption.copyToPath(directory, fileEntry);
        assertNotNull(results);
        assertEquals(expectedFilenames.size(), results.size());

        // Note that we do assume data fork is first in the list
        Path result = results.getFirst();
        assertNotNull(result);
        result.toFile().deleteOnExit();
        assertEquals(expectedFilenames.getFirst(), result.getFileName().toString());
        byte[] actualDataFork = Files.readAllBytes(result);
        assertNotNull(actualDataFork);
        arrayTest.accept(expectedDataFork, actualDataFork);

        if (expectedFilenames.size() > 1) {
            result = results.getLast();
            assertNotNull(result);
            result.toFile().deleteOnExit();
            assertEquals(expectedFilenames.getLast(), result.getFileName().toString());
            byte[] actualResourceFork = Files.readAllBytes(result);
            assertNotNull(actualResourceFork);
            arrayTest.accept(expectedResourceFork, actualResourceFork);
        }
    }

    public static class TestFileEntry implements FileEntry {
        private final Directory parent;
        private final boolean deleted;
        private final FileStore fileStore;
        private final String name;
        private final String filetype;
        private final DataBuffer dataFork;
        private final DataBuffer resourceFork;

        private TestFileEntry(Directory parent, boolean deleted, FileStore fileStore, String name, String filetype,
                              DataBuffer dataFork, DataBuffer resourceFork) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(filetype);
            Objects.requireNonNull(dataFork);
            this.parent = parent;
            this.deleted = deleted;
            this.fileStore = fileStore;
            this.name = name;
            this.filetype = filetype;
            this.dataFork = dataFork;
            this.resourceFork = resourceFork;
        }

        @Override
        public Directory getParent() {
            return parent;
        }

        @Override
        public boolean isDeleted() {
            return deleted;
        }

        @Override
        public FileStore getFileStore() {
            return fileStore;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getSize() {
            int size = dataFork.limit();
            if (resourceFork != null) {
                size += resourceFork.limit();
            }
            return size;
        }

        @Override
        public DataBuffer getDataFork() {
            return dataFork;
        }

        @Override
        public Optional<DataBuffer> getResourceFork() {
            return Optional.ofNullable(resourceFork);
        }

        @Override
        public <T> Optional<T> get(Class<T> iface) {
            return Optional.empty();
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Directory parent;
            private boolean deleted;
            private FileStore fileStore;
            private String name = "TESTFILE";
            private String filetype = "TXT";
            private DataBuffer dataFork = DataBuffer.wrap("THIS IS A TEST FILE".getBytes());
            private DataBuffer resourceFork;

            public Builder parent(Directory parent) {
                Objects.requireNonNull(parent);
                this.parent = parent;
                return this;
            }
            public Builder deleted(boolean deleted) {
                this.deleted = deleted;
                return this;
            }
            public Builder fileStore(FileStore fileStore) {
                Objects.requireNonNull(fileStore);
                this.fileStore = fileStore;
                return this;
            }
            public Builder name(String name) {
                Objects.requireNonNull(name);
                this.name = name;
                return this;
            }
            public Builder filetype(String filetype) {
                Objects.requireNonNull(filetype);
                this.filetype = filetype;
                return this;
            }
            public Builder dataFork(byte[] dataFork) {
                Objects.requireNonNull(dataFork);
                return dataFork(DataBuffer.wrap(dataFork));
            }
            public Builder dataFork(DataBuffer dataFork) {
                Objects.requireNonNull(dataFork);
                this.dataFork = dataFork;
                return this;
            }
            public Builder resourceFork(byte[] resourceFork) {
                Objects.requireNonNull(resourceFork);
                return resourceFork(DataBuffer.wrap(resourceFork));
            }
            public Builder resourceFork(DataBuffer resourceFork) {
                Objects.requireNonNull(resourceFork);
                this.resourceFork = resourceFork;
                return this;
            }
            public TestFileEntry get() {
                return new TestFileEntry(parent, deleted, fileStore, name, filetype, dataFork, resourceFork);
            }
        }
    }
}
