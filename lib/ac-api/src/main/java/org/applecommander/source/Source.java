package org.applecommander.source;

import org.applecommander.capability.CapabilityProvider;
import org.applecommander.hint.HintProvider;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.Information;

import java.util.List;
import java.util.Optional;

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
public interface Source extends CapabilityProvider, HintProvider, Container {
    int getSize();
    String getName();
    DataBuffer readAllBytes();
    DataBuffer readBytes(int offset, int length);
    void writeBytes(int offset, DataBuffer data);
    boolean hasChanged();
    void clearChanges();
    List<Information> information();

    /**
     * Indicates if the source image is approximately equal to this size
     * (less any image over-read).
     * Currently, hardcoded to allow up to 10 extra bytes at the end of a
     * disk image.  Must be at least the requested size!
     */
    default boolean isApproxEQ(int value) {
        return getSize() >= value && getSize() <= value + 10;
    }
    /**
     * Indicates if the source image is approximately less than this
     * size (less any image over-read).
     * Currently, hardcoded to allow up to 10 extra bytes at the end of a
     * disk image.  Must be at least the requested size!
     */
    default boolean isApproxLE(int value) {
        return getSize() <= value + 10;
    }

    /**
     * Test if this name has a given file extension.
     */
    default boolean extensionLike(String extension) {
        String name = getName().toLowerCase();
        String ext1 = String.format(".%s", extension.toLowerCase());
        String ext2 = String.format(".%s.gz", extension.toLowerCase());
        return name.endsWith(ext1) || name.endsWith(ext2);
    }

    interface Factory {
        Optional<Source> fromObject(Object object);
        Optional<Source> fromSource(Source source);
    }
}
