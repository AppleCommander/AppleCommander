package org.applecommander.source;

import org.applecommander.capability.CapabilityProvider;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.Information;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Source and Factory for an archive or disk in AppleCommander.
 * <p/>
 * Typical usage:
 * {@snippet lang=java:
 *    Optional<Source> sourceOpt = Source.create(Path.of(filename));
 *    if (sourceOpt.isPresent()) {
 *      // do something with sourceOpt.get()
 *      System.out.println(sourceOpt.get().getSize());
  *    }
 *    // Only happens if the initiating object is not understood
 *    throw new RuntimeException("Unable to create image source");
 * }
 */
public interface Source extends CapabilityProvider, Container {
    int getSize();
    DataBuffer readAllBytes();
    DataBuffer readBytes(int offset, int length);
    void writeBytes(int offset, DataBuffer data);
    boolean hasChanged();
    void clearChanges();
    List<Information> information();

    interface Factory {
        Optional<Source> fromObject(Object object);
        Optional<Source> fromSource(Source source);
    }

    /**
     * Standardized Source creation. Uses the ServiceLoader mechanism to identify
     * all potential Source factories.  Note that this supports a Source being
     * passed as well (which ensures any wrapping layers are added, such as 2IMG
     * or DiskCopy).
     */
    static Optional<Source> create(Object object) {
        ServiceLoader<Factory> factories = ServiceLoader.load(Factory.class);
        // First: Create source from a generalized object (thus, can be Path, Entry, URL, etc)
        Optional<Source> source = Optional.empty();
        if (object instanceof Source objSource) {
            source = Optional.of(objSource);
        }
        else {
            for (Factory factory : factories) {
                source = factory.fromObject(object);
                if (source.isPresent()) {
                    break;
                }
            }
        }
        // Second: Once we have a source, run a pass again, checking if there is a wrapper we need (2IMG, DiskCopy, etc)
        if (source.isPresent()) {
            for (Factory factory : factories) {
                Optional<Source> fromSource = factory.fromSource(source.get());
                if (fromSource.isPresent()) {
                    return fromSource;
                }
            }
        }
        return source;
    }
}
