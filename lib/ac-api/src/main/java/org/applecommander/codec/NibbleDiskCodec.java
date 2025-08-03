package org.applecommander.codec;

import org.applecommander.capability.CapabilityProvider;
import org.applecommander.util.DataBuffer;

public interface NibbleDiskCodec extends CapabilityProvider {
    int encodedSize();
    int decodedSize();
    DataBuffer decode(DataBuffer rawData);
    DataBuffer encode(DataBuffer data);
}
