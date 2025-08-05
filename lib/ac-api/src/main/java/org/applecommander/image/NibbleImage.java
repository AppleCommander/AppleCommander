package org.applecommander.image;

import org.applecommander.capability.Capability;
import org.applecommander.device.NibbleTrackReaderWriter;
import org.applecommander.source.Source;
import org.applecommander.util.DataBuffer;

public class NibbleImage implements NibbleTrackReaderWriter {
    public static final int DISK_SIZE = 232960;
    public static final int TRACK_SIZE = 6656;
    public static final int TRACKS_ON_DEVICE = DISK_SIZE / TRACK_SIZE;

    private Source source;

    public NibbleImage(Source source) {
        this.source = source;
    }

    @Override
    public boolean can(Capability capability) {
        return capability == Capability.WRITE_TRACK;
    }

    @Override
    public int getTracksOnDevice() {
        return TRACKS_ON_DEVICE;
    }

    @Override
    public DataBuffer readTrackData(int track) {
        return source.readBytes(track * TRACK_SIZE, TRACK_SIZE);
    }

    @Override
    public void writeTrackData(int track, DataBuffer data) {
        assert(data.limit() == TRACK_SIZE);
        source.writeBytes(track * TRACK_SIZE, data);
    }
}
