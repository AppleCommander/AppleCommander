import com.webcodepro.applecommander.storage.DiskFactory;
import org.applecommander.filestore.FileStoreFactory;
import org.applecommander.source.Source;

module org.applecommander.api {
    requires java.desktop;
    requires java.logging;
    requires com.google.gson;
    requires org.apache.commons.csv;

    // TODO automatic modules
    requires acdasm;
    requires bastools.api;
    requires ShrinkItArchive;

    // Legacy APIs
    exports com.webcodepro.applecommander.storage;
    exports com.webcodepro.applecommander.storage.compare;
    exports com.webcodepro.applecommander.storage.filters;
    exports com.webcodepro.applecommander.storage.filters.imagehandlers;
    exports com.webcodepro.applecommander.storage.os.cpm;
    exports com.webcodepro.applecommander.storage.os.dos33;
    exports com.webcodepro.applecommander.storage.os.gutenberg;
    exports com.webcodepro.applecommander.storage.os.nakedos;
    exports com.webcodepro.applecommander.storage.os.pascal;
    exports com.webcodepro.applecommander.storage.os.prodos;
    exports com.webcodepro.applecommander.storage.os.rdos;
    exports com.webcodepro.applecommander.ui;
    exports com.webcodepro.applecommander.util;
    exports com.webcodepro.applecommander.util.readerwriter;
    exports com.webcodepro.applecommander.util.filestreamer;

    // FormattedDisk discovery mechanisms
    uses DiskFactory;
    provides com.webcodepro.applecommander.storage.DiskFactory
        with com.webcodepro.applecommander.storage.os.cpm.CpmDiskFactory,
             com.webcodepro.applecommander.storage.os.dos33.DosDiskFactory,
             com.webcodepro.applecommander.storage.os.gutenberg.GutenbergDiskFactory,
             com.webcodepro.applecommander.storage.os.nakedos.NakedosDiskFactory,
             com.webcodepro.applecommander.storage.os.pascal.PascalDiskFactory,
             com.webcodepro.applecommander.storage.os.prodos.ProdosDiskFactory,
             com.webcodepro.applecommander.storage.os.rdos.RdosDiskFactory;

    // Revised APIs
    exports org.applecommander.archive.shrinkit;
    exports org.applecommander.archive.zip;
    exports org.applecommander.capability;
    exports org.applecommander.device;
    exports org.applecommander.device.nibble;
    exports org.applecommander.filestore;
    exports org.applecommander.hint;
    exports org.applecommander.image;
    exports org.applecommander.os;
    exports org.applecommander.os.dos;
    exports org.applecommander.os.pascal;
    exports org.applecommander.source;
    exports org.applecommander.usage;
    exports org.applecommander.util;

    // FileStore discovery mechanisms
    uses FileStoreFactory;
    provides org.applecommander.filestore.FileStoreFactory
        with org.applecommander.archive.shrinkit.ShrinkitFileStoreFactory,
             org.applecommander.archive.zip.ZipFileStoreFactory,
             // Legacy shim
             com.webcodepro.applecommander.storage.DiskFileStoreFactory;

    // Source discovery mechanisms
    uses Source.Factory;
    provides org.applecommander.source.Source.Factory
             // These are "fromObject" sources
        with org.applecommander.source.FileSource.Factory,
             com.webcodepro.applecommander.storage.FileEntrySource.Factory,
             org.applecommander.source.FileEntrySource.Factory,
             // These are "fromSource" (wrapper) sources
             org.applecommander.image.DiskCopyImage.Factory,
             org.applecommander.image.UniversalDiskImage.Factory,
             com.webcodepro.applecommander.storage.ShrinkitSourceFactory;
}
