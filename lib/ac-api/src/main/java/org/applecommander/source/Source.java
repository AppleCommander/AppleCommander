package org.applecommander.source;

import org.applecommander.capability.CapabilityProvider;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.Information;

import java.util.List;

/**
 * Source for an archive or disk in AppleCommander.
 */
public interface Source extends CapabilityProvider, Container {
    int getSize();
    DataBuffer readAllBytes();
    DataBuffer readBytes(int offset, int length);
    void writeBytes(int offset, DataBuffer data);
    boolean hasChanged();
    void clearChanges();
    List<Information> information();
}
