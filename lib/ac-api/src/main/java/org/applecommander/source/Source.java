package org.applecommander.source;

import org.applecommander.capability.CapabilityProvider;
import org.applecommander.util.DataBuffer;

/**
 * Source for an archive or disk in AppleCommander.
 */
public interface Source extends CapabilityProvider {
    DataBuffer readBytes(int offset, int length);
    void writeBytes(int offset, DataBuffer data);
}
