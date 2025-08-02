package org.applecommander.device;

import org.applecommander.capability.Capability;
import org.applecommander.codec.AddressEncoderDecoder;
import org.applecommander.codec.NibbleEncoderDecoder;
import org.applecommander.source.Source;
import org.applecommander.util.DataBuffer;

public class NibbleImage implements NibbleTrackReaderWriter {
    public static final int DISK_SIZE = 232960;
    public static final int TRACK_SIZE = 6656;

    private Source source;
    private NibbleEncoderDecoder addrCodec;
    private NibbleEncoderDecoder dataCodec;

    public NibbleImage(Source source, NibbleEncoderDecoder dataCodec) {
        this.source = source;
        this.addrCodec = new AddressEncoderDecoder();
        this.dataCodec = dataCodec;
    }

    @Override
    public boolean can(Capability capability) {
        if (capability == Capability.WRITE_TRACK) {
            return addrCodec.can(Capability.ENCODE) && dataCodec.can(Capability.ENCODE);
        }
        return false;
    }

    @Override
    public DataBuffer readTrackData(int track) {
        // TODO finish this
        return source.readBytes(track * TRACK_SIZE, TRACK_SIZE);
    }

    @Override
    public void writeTrackData(int track, DataBuffer data) {
        // TODO finish this
        assert(data.limit() == TRACK_SIZE);
        source.writeBytes(track * TRACK_SIZE, data);
    }
}
