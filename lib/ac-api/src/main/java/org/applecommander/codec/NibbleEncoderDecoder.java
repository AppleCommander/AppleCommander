package org.applecommander.codec;

import org.applecommander.capability.CapabilityProvider;
import org.applecommander.util.DataBuffer;

// TODO rename this Codec?
public interface NibbleEncoderDecoder extends CapabilityProvider {
    DataBuffer decode(DataBuffer rawData);
    DataBuffer encode(DataBuffer data);
}
