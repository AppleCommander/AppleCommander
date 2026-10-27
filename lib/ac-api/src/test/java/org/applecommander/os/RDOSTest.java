package org.applecommander.os;

import com.webcodepro.applecommander.storage.DiskConstants;
import org.applecommander.device.OrderedTrackSectorDevice;
import org.applecommander.device.SectorOrder;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.os.rdos.RDOSDirectory;
import org.applecommander.os.rdos.RDOSFileEntry;
import org.applecommander.os.rdos.RDOSFileStore;
import org.applecommander.os.rdos.RDOSFileStoreFactory;
import org.applecommander.source.DataBufferSource;
import org.applecommander.source.Source;
import org.applecommander.util.DataBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RDOSTest {
    @Test
    public void testCreateAndWrite() {
        // Create a disk
        Source source = DataBufferSource.create(DiskConstants.APPLE_140KB_DISK, "RDOS-TEST").get();
        TrackSectorDevice device = new OrderedTrackSectorDevice(source, SectorOrder.DOS);
        RDOSFileStore fileStore = RDOSFileStoreFactory.create(device);
        assertNotNull(fileStore);

        // Get the root directory and verify there are no files
        RDOSDirectory directory = fileStore.getRootDirectory();
        assertNotNull(directory);
        assertEquals(2, directory.getFiles().size());

        // Verify SYSTEMBOOT exists
        RDOSFileEntry systemboot = (RDOSFileEntry) directory.findFile("SYSTEMBOOT").orElseThrow();
        assertNotNull(systemboot);
        assertEquals("T", systemboot.getType());
        DataBuffer systembootData = systemboot.getDataFork();
        final String contentPrefix = "PR#0:IN#0:";
        assertEquals(contentPrefix, systembootData.getFixedLengthString(0, contentPrefix.length()));

        // Create and verify the first file.
        final byte[] textContents = "THIS IS A THE FILE DATA.".getBytes();
        final String firstFileName = "FIRST-FILE";
        RDOSFileEntry firstFile = createFile(directory, firstFileName, "T", textContents);
        verifyFile(firstFile, firstFileName, "T", 33, 1, textContents);

        // Create and verify a second file.
        final byte[] binaryContents = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        final String secondFileName = "SECOND-FILE";
        RDOSFileEntry secondFile = createFile(directory, secondFileName, "B", binaryContents);
        verifyFile(secondFile, secondFileName, "B", 34, 1, binaryContents);

        // Read back the first file entry from the directory and then delete it.
        RDOSFileEntry firstFileEntry = (RDOSFileEntry) directory.findFile(firstFileName).orElseThrow();
        assertNotNull(firstFileEntry);
        directory.deleteFile(firstFileEntry);

        // Finally, create a 3rd file and expect it to be placed where the FIRST file was.
        final byte[] thirdFile = "THIS IS THE THIRD FILE.".getBytes();
        final String thirdFileName = "THIRD-FILE";
        RDOSFileEntry thirdFileEntry = createFile(directory, thirdFileName, "T", thirdFile);
        verifyFile(thirdFileEntry, thirdFileName, "T", 33, 1, thirdFile);
        assertEquals(4, directory.getFiles().size());
    }

    public RDOSFileEntry createFile(RDOSDirectory directory, String fileName, String fileType, byte[] dataFork) {
        RDOSFileEntry fileEntry = directory.createFile(fileName);
        assertNotNull(fileEntry);
        fileEntry.setType(fileType);
        fileEntry.setDataFork(DataBuffer.wrap(dataFork));
        return fileEntry;
    }

    public void verifyFile(RDOSFileEntry fileEntry, String expectedFileName, String expectedFileType,
                           int expectedFirstBlock, int expectedBlockCount, byte[] expectedDataFork) {
        assertEquals(expectedFileName, fileEntry.getName());
        assertEquals(expectedFileType, fileEntry.getType());
        assertEquals(expectedFirstBlock, fileEntry.getFirstBlock());
        assertEquals(expectedBlockCount, fileEntry.getBlocks());
        assertArrayEquals(expectedDataFork, fileEntry.getDataFork().getFixedLengthString(0, expectedDataFork.length).getBytes());
    }

}
