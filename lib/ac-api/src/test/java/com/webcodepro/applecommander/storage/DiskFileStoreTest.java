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
package com.webcodepro.applecommander.storage;

import org.applecommander.capability.Capability;
import org.applecommander.filestore.*;
import org.applecommander.filestore.DirectoryEntry;
import org.applecommander.filestore.FileEntry;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Partial clone of the DiskHelperTest the exercise and verify the DiskFileStoreAdapter.
 */
public class DiskFileStoreTest {
    @Test
    public void testLoadDos33() throws IOException, DiskException {
        List<FileStore> fileStores = showDirectory("DOS 3.3.po");
        assertApplesoftFile(fileStores.getFirst(), "HELLO");
        assertIntegerFile(fileStores.getFirst(), "ANIMALS");
        assertTextFile(fileStores.getFirst(), "APPLE PROMS");
        assertBinaryFile(fileStores.getFirst(), "BOOT13");
        //assertEquals(DOS33_FORMAT, fileStores.getFirst().getFormat());
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testLoadMaster() throws IOException, DiskException {
        List<FileStore> fileStores = showDirectory("MASTER.DSK");
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testLoadGalacticAttack1() throws IOException, DiskException {
        List<FileStore> fileStores = showDirectory("galatt.dsk");
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testLoadProdos() throws IOException, DiskException {
        List<FileStore> fileStores = showDirectory("Prodos.dsk");
        assertApplesoftFile(fileStores.getFirst(), "COPY.ME");
        assertBinaryFile(fileStores.getFirst(), "SETTINGS");
        assertDisassemblyFile(fileStores.getFirst(), "PRODOS");
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testLoadMarbleMadness() throws IOException, DiskException {
        List<FileStore> fileStores = showDirectory("Marble Madness (1985)(Electronic Arts).2mg");
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testRdosBoot() throws IOException, DiskException {
        List<FileStore> fileStores = showDirectory("RDOSboot.dsk");
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testSsiSave() throws IOException, DiskException {
        List<FileStore> fileStores = showDirectory("SSIsave.dsk");
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testPhanta31() throws IOException, DiskException {
        List<FileStore> fileStores = showDirectory("PHANTA31.DSK");
        assertApplesoftFile(fileStores.getFirst(), "PHANTASIE III");
        assertBinaryFile(fileStores.getFirst(), "TWN31");
        assertTextFile(fileStores.getFirst(), "ITEM");
        assertGraphicsFile(fileStores.getFirst(), "ICE DRAGON");
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testPhanta32() throws IOException, DiskException {
        List<FileStore> fileStores = showDirectory("PHANTA32.DSK");
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testPhan2d1() throws IOException, DiskException {
        List<FileStore> fileStores = showDirectory("phan2d1.dsk");
        assertApplesoftFile(fileStores.getFirst(), "PHANTASIE II");
        assertBinaryFile(fileStores.getFirst(), "TWN21");
        assertTextFile(fileStores.getFirst(), "ITEM");
        assertGraphicsFile(fileStores.getFirst(), "ICE DRAGON");
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testPhan2d2() throws IOException, DiskException {
        List<FileStore> fileStores = showDirectory("phan2d2.dsk");
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testPhantasie1() throws IOException, DiskException {
        List<FileStore> fileStores = showDirectory("Phantasie1.dsk");
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testPhantasie2() throws IOException, DiskException {
        List<FileStore> fileStores = showDirectory("Phantasie2.dsk");
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testCavernsOfFreitag() throws IOException, DiskException {
        List<FileStore> fileStores = showDirectory("CavernsOfFreitag.dsk");
        assertGraphicsFile(fileStores.getFirst(), "TITLE.PIC");
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testUniDosD3110() throws IOException, DiskException {
        List<FileStore> fileStores = showDirectory("D3110.dsk");
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testUniDosD3151() throws IOException, DiskException {
        List<FileStore> fileStores = showDirectory("D3151.dsk");
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testLoadDos33SystemMasterWoz1() throws IOException, DiskException {
        List<FileStore> fileStores = showDirectory("DOS 3.3 System Master.woz1");
        assertApplesoftFile(fileStores.getFirst(), "HELLO");
        assertIntegerFile(fileStores.getFirst(), "COPY");
        assertBinaryFile(fileStores.getFirst(), "BOOT13");
        //assertEquals(DOS33_FORMAT, fileStores.getFirst().getFormat());
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testLoadDos33SystemMasterWoz2() throws IOException, DiskException {
        List<FileStore> fileStores = showDirectory("DOS 3.3 System Master.woz2");
        assertApplesoftFile(fileStores.getFirst(), "HELLO");
        assertIntegerFile(fileStores.getFirst(), "COPY");
        assertBinaryFile(fileStores.getFirst(), "BOOT13");
        //assertEquals(DOS33_FORMAT, fileStores.getFirst().getFormat());
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testLoadDos32SystemMasterWoz() throws IOException, DiskException {
        List<FileStore> fileStores = showDirectory("DOS 3.2 System Master.woz");
        assertIntegerFile(fileStores.getFirst(), "HELLO");
        assertBinaryFile(fileStores.getFirst(), "UPDATE 3.2");
        //assertEquals(DOS32_FORMAT, fileStores.getFirst().getFormat());
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testLoadDos32SystemMasterNib() throws IOException, DiskException {
        List<FileStore> fileStores = showDirectory("original321sysmaspls.nib");
        assertApplesoftFile(fileStores.getFirst(), "HELLO");
        assertBinaryFile(fileStores.getFirst(), "UPDATE 3.2.1");
        assertTextFile(fileStores.getFirst(), "APPLE PROMS");
        //assertEquals(DOS32_FORMAT, fileStores.getFirst().getFormat());
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testLoadNakedosSuperMonDisk() throws DiskException, IOException {
        List<FileStore> fileStores = showDirectory("Super-Mon-dev.dsk");
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testLoadGutenbergDisk() throws DiskException, IOException {
        List<FileStore> fileStores = showDirectory("Gutenberg_side1.DSK");
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testLoad3132Disk() throws DiskException, IOException {
        List<FileStore> fileStores = showDirectory("3132.DSK.gz");
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testCPMV233Disk() throws DiskException, IOException {
        List<FileStore> fileStores = showDirectory("CPMV233.DSK");
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testCPAM51BDisk() throws DiskException, IOException {
        List<FileStore> fileStores = showDirectory("CPAM51B.dsk");
        assertCanReadFiles(fileStores);
    }

    @Test
    public void testOriginal32SystemMasterStd() throws DiskException, IOException {
        List<FileStore> fileStores = showDirectory("original32sysmasstd.d13");
        assertCanReadFiles(fileStores);
    }

    protected List<FileStore> showDirectory(String imageName) {
        String fullPath = String.join("/", "src/test/resources/disks", imageName);
        Source source = Sources.create(fullPath).orElseThrow();
        FileStoreFactory.Context ctx = FileStores.inspect(source);
        if (ctx.fileStores.isEmpty()) {
            throw new RuntimeException("no disks discovered for: " + imageName);
        }
        for (FileStore fileStore : ctx.fileStores) {
            System.out.println();
            System.out.println(fileStore.getLabel());
            showFiles(fileStore.getRootDirectory(), "");
            //System.out.println(fileStore.getFreeSpace() + " bytes free.");
            //System.out.println(fileStore.getUsedSpace() + " bytes used.");
            System.out.println("This disk " + (fileStore.can(Capability.SUPPORTS_DIRECTORIES) ? "does" : "does not") +
                    " support directories.");
            //System.out.println("This disk is formatted in the " + fileStore.getFormat() + " format.");
            System.out.println();

            //showDiskUsage(fileStore);
        }
        return ctx.fileStores;
    }

    protected void showFiles(DirectoryEntry parent, String indent) {
        for (FileEntry file : parent.getFiles()) {
            if (!file.isDeleted()) {
                //List<String> data = entry.getFileColumnData(FormattedDisk.FILE_DISPLAY_NATIVE);
                //System.out.print(indent);
                //for (int d=0; d<data.size(); d++) {
                //    System.out.print(data.get(d));
                //    System.out.print(" ");
                //}
                //System.out.println();
                System.out.printf("%s%s %s %d\n", indent, file.getName(), file.getFiletype(), file.getSize());
            }
            Optional<DirectoryEntry> subdirectory = file.get(DirectoryEntry.class);
            subdirectory.ifPresent(directoryEntry -> showFiles(directoryEntry, indent + "  "));
        }
    }

    protected void assertApplesoftFile(FileStore fileStore, String filename) throws DiskException {
        assertNotNull(fileStore, filename + " test: Disk should not be null");
        FileEntry fileEntry = fileStore.getRootDirectory().findFile(filename).orElseThrow();
        assertNotNull(fileEntry, filename + " test: File not found");
        //assertInstanceOf(ApplesoftFileFilter.class, fileEntry.getSuggestedFilter(), "ApplesoftFileFilter was not chosen");
    }

    protected void assertIntegerFile(FileStore fileStore, String filename) throws DiskException {
        assertNotNull(fileStore, filename + " test: Disk should not be null");
        FileEntry fileEntry = fileStore.getRootDirectory().findFile(filename).orElseThrow();
        assertNotNull(fileEntry, filename + " test: File not found");
        //assertInstanceOf(IntegerBasicFileFilter.class, fileEntry.getSuggestedFilter(), "IntegerBasicFileFilter was not chosen");
    }

    protected void assertTextFile(FileStore fileStore, String filename) throws DiskException {
        assertNotNull(fileStore, filename + " test: Disk should not be null");
        FileEntry fileEntry = fileStore.getRootDirectory().findFile(filename).orElseThrow();
        assertNotNull(fileEntry, filename + " test: File not found");
        //assertInstanceOf(TextFileFilter.class, fileEntry.getSuggestedFilter(), "TextFileFilter was not chosen");
    }

    protected void assertBinaryFile(FileStore fileStore, String filename) throws DiskException {
        assertNotNull(fileStore, filename + " test: Disk should not be null");
        FileEntry fileEntry = fileStore.getRootDirectory().findFile(filename).orElseThrow();
        assertNotNull(fileEntry, filename + " test: File not found");
        //assertInstanceOf(BinaryFileFilter.class, fileEntry.getSuggestedFilter(), "BinaryFileFilter was not chosen");
    }

    protected void assertDisassemblyFile(FileStore fileStore, String filename) throws DiskException {
        assertNotNull(fileStore, filename + " test: Disk should not be null");
        FileEntry fileEntry = fileStore.getRootDirectory().findFile(filename).orElseThrow();
        assertNotNull(fileEntry, filename + " test: File not found");
        //assertInstanceOf(DisassemblyFileFilter.class, fileEntry.getSuggestedFilter(), "DisassemblyFileFilter was not chosen");
    }

    protected void assertGraphicsFile(FileStore fileStore, String filename) throws DiskException {
        assertNotNull(fileStore, filename + " test: Disk should not be null");
        FileEntry fileEntry = fileStore.getRootDirectory().findFile(filename).orElseThrow();
        assertNotNull(fileEntry, filename + " test: File not found");
        //assertInstanceOf(GraphicsFileFilter.class, fileEntry.getSuggestedFilter(), "GraphicsFileFilter was not chosen");
    }
    
    protected void assertCanReadFiles(List<FileStore> fileStores) throws DiskException {
        for (FileStore fileStore : fileStores) {
            assertCanReadFiles(fileStore.getRootDirectory());
        }
    }

    protected void assertCanReadFiles(DirectoryEntry parent) throws DiskException {
        for (FileEntry fileEntry : parent.getFiles()) {
            Optional<DirectoryEntry> subdirectory = fileEntry.get(DirectoryEntry.class);
            if (fileEntry.isDeleted()) {
                System.out.printf("Skipping deleted file: %s\n", fileEntry.getName());
            }
            else if (subdirectory.isPresent()) {
                assertCanReadFiles(subdirectory.get());
            }
            else {
                try {
                    byte[] data = fileEntry.getDataFork();
                    assertNotNull(data);
                } catch (Exception e) {
                    throw new AssertionError(String.format("Unable to read file '%s'", fileEntry.getName()), e);
                }
            }
        }
    }
}
