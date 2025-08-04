package org.applecommander.device;

import org.applecommander.capability.Capability;
import org.applecommander.source.Source;
import org.applecommander.util.DataBuffer;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class WozImage implements NibbleTrackReaderWriter {
    // The magic bytes are read in little-endian order, so they do appear reversed here.
    public static final int WOZ1_MAGIC = 0x315a4f57;
    public static final int WOZ2_MAGIC = 0x325a4f57;
    public static final int INFO_CHUNK_ID = 0x4f464e49;
    public static final int TMAP_CHUNK_ID = 0x50414d54;
    public static final int TRKS_CHUNK_ID = 0x534b5254;
    public static final int FLUX_CHUNK_ID = 0x54495257;
    public static final int META_CHUNK_ID = 0x4154454d;

    private final Source source;

    // WOZ image representation
    private InfoChunk info;
    private Map<String,String> meta = new HashMap<>();
    private List<TmapChunk> tmap = new ArrayList<>();
    private List<TrkInfo> trks = new ArrayList<>();

    // Internal configuration settings
    private boolean blockDevice;
    private boolean trackAndSectorDevice;
    private int blocksOnDevice;
    private Function<Integer,DataBuffer> trackReader = null;

    public WozImage(Source source) {
        this.source = source;

        DataBuffer bb = source.readAllBytes();

        int sig = bb.readInt();
        int test = bb.readInt();
        final int testExpected = 0xa0d0aff;
        if (sig == WOZ1_MAGIC && test == testExpected) {
            this.trackReader = this::readTrackDataWOZ1;
        }
        else if (sig == WOZ2_MAGIC && test == testExpected) {
            this.trackReader = this::readTrackDataWOZ2;
        }
        else {
            throw new RuntimeException("Not a WOZ1 or WOZ2 format file.");
        }
        bb.readInt();    // ignoring CRC

        Consumer<DataBuffer> tmapReader = this::readTmapChunk525;
        while (bb.hasRemaining()) {
            int chunkId = bb.readInt();
            int chunkSize = bb.readInt();
            DataBuffer data = bb.readBuffer(chunkSize);
            switch (chunkId) {
                case INFO_CHUNK_ID:
                    this.info = new InfoChunk(data);
                    if (this.info.getDiskType() == 1) { // DISK II - 5.25"
                        this.blockDevice = false;
                        this.trackAndSectorDevice = true;
                        this.blocksOnDevice = 280;
                        tmapReader = this::readTmapChunk525;
                    }
                    else {  // 400K or 800K 3.5" disk
                        this.blockDevice = true;
                        this.trackAndSectorDevice = false;
                        this.blocksOnDevice = this.info.getDiskSides() * 800;
                        tmapReader = this::readTmapChunk35;
                    }
                    break;
                case TMAP_CHUNK_ID:
                    tmapReader.accept(data);
                    // clean out the invalid chunks -- otherwise disk looks larger than it is
                    for (int i=tmap.size()-1; i>=0; i--) {
                        if (tmap.get(i).isValid()) break;
                        tmap.remove(i);
                    }
                    break;
                case TRKS_CHUNK_ID:
                    readTrksChunk(data);
                    break;
                case META_CHUNK_ID:
                    readMetaChunk(data);
                    break;
            }
        }
    }

    @Override
    public boolean can(Capability capability) {
        // No track writing with WOZ images
        return false;
    }

    @Override
    public DataBuffer readTrackData(int track) {
        return this.trackReader.apply(track);
    }

    @Override
    public void writeTrackData(int track, DataBuffer data) {

    }

    public DataBuffer readTrackDataWOZ2(int track) {
        TmapChunk map = tmap.get(track);
        int trkInfo = 255;
        for (int i=0; i<4 && trkInfo == 255; i++) {
            trkInfo = map.getOffset(i);
        }
        TrkInfo trk = trks.get(trkInfo);
        DataBuffer rawData = source.readBytes(trk.getStartingBlock()*512, trk.getBlockCount()*512);
        return transformBitstream(rawData, trk.getBitCount());
    }

    public DataBuffer readTrackDataWOZ1(int track) {
        final int trackLength = 6656;
        int start = 256 + (track * trackLength);
        DataBuffer details = source.readBytes(start + 6646, 10);
        int bytesUsed = details.getUnsignedShort(0);
        int bitCount = details.getUnsignedShort(2);
        DataBuffer rawData = source.readBytes(start, bytesUsed);
        return transformBitstream(rawData, bitCount);
    }

    public void readMetaChunk(DataBuffer data) {
        if (data.limit() > 0) {
            String meta = data.readFixedLengthString(data.limit());
            for (String line : meta.split("\n")) {
                String[] parts = line.split("\t");
                if (parts.length == 2) {
                    this.meta.put(parts[0], parts[1]);
                }
            }
        }
    }

    public void readTmapChunk525(DataBuffer data) {
        while (data.hasRemaining()) {
            DataBuffer chunk = data.readBuffer(4);
            tmap.add(new TmapChunk(chunk));
        }
    }

    public void readTmapChunk35(DataBuffer data) {
        while (data.hasRemaining()) {
            DataBuffer chunk = data.readBuffer(1);
            tmap.add(new TmapChunk(chunk));
        }
    }

    public void readTrksChunk(DataBuffer data) {
        // Note: WOZ2 only
        for (int i=0; i<160; i++) {
            trks.add(new TrkInfo(data));
        }
    }

    public DataBuffer transformBitstream(DataBuffer rawData, int bitCount) {
        // NOTE: Uncertain if we need to track 0's (only 2 allowed by hardware) or not.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int bitNo = 7;
        int byteNo = 0;
        int byteVal = 0;
        while (bitCount > 0) {
            bitCount--;
            byteVal <<= 1;
            if (rawData.isBitSet(byteNo, bitNo)) {
                byteVal |= 1;
            }
            bitNo--;
            if (bitNo < 0) {
                bitNo = 7;
                byteNo++;
            }
            if (byteVal >= 128) {
                baos.write(byteVal);
                byteVal = 0;
            }
        }
        return DataBuffer.wrap(baos.toByteArray());

    }

    public static class InfoChunk {
        private int version;
        private int diskType;
        private int writeProtected;
        private int synchrinized;
        private int cleaned;
        private String creator;
        private int diskSides;
        private int bootSectorFormat;
        private int optimalBitTiming;
        private int compatibleHardware;
        private int requiredRAM;
        private int largestTrack;
        private int fluxBlock;
        private int largestFluxTrack;

        public InfoChunk(DataBuffer data) {
            this.version = data.readUnsignedByte();
            this.diskType = data.readUnsignedByte();
            this.writeProtected = data.readUnsignedByte();
            this.synchrinized = data.readUnsignedByte();
            this.cleaned = data.readUnsignedByte();
            this.creator = data.readFixedLengthString(32);
            if (this.version >= 2) {
                this.diskSides = data.readUnsignedByte();
                this.bootSectorFormat = data.readUnsignedByte();
                this.optimalBitTiming = data.readUnsignedByte();
                this.compatibleHardware = data.readUnsignedShort();
                this.requiredRAM = data.readUnsignedShort();
                this.largestTrack = data.readUnsignedShort();
            }
            if (this.version >= 3) {
                this.fluxBlock = data.readUnsignedShort();
                this.largestFluxTrack = data.readUnsignedShort();
            }
        }

        public int getVersion() {
            return version;
        }
        public int getDiskType() {
            return diskType;
        }
        public int getWriteProtected() {
            return writeProtected;
        }
        public int getSynchrinized() {
            return synchrinized;
        }
        public int getCleaned() {
            return cleaned;
        }
        public String getCreator() {
            return creator;
        }
        public int getDiskSides() {
            return diskSides;
        }
        public int getBootSectorFormat() {
            return bootSectorFormat;
        }
        public int getOptimalBitTiming() {
            return optimalBitTiming;
        }
        public int getCompatibleHardware() {
            return compatibleHardware;
        }
        public int getRequiredRAM() {
            return requiredRAM;
        }
        public int getLargestTrack() {
            return largestTrack;
        }
        public int getFluxBlock() {
            return fluxBlock;
        }
        public int getLargestFluxTrack() {
            return largestFluxTrack;
        }
    }

    public static class TmapChunk {
        private int[] tmap;

        /**
         * Create TMAP entry. If length is 1, this is a 3.5" TMAP entry of just one
         * value; if length is 4, this is a 5.25" TMAP entry of every quarter track.
         */
        public TmapChunk(DataBuffer data) {
            assert(data.limit() == 1 || data.limit() == 4);
            tmap = new int[4];
            for (int i=0; i<tmap.length; i++) {
                tmap[i] = (i < data.limit()) ? tmap[i] = data.readUnsignedByte() : 255;
            }
        }

        public boolean isValid() {
            byte flag = (byte)255;
            return tmap[0] != flag || tmap[1] != flag || tmap[2] != flag || tmap[3] != flag;
        }

        public int getOffset(int index) {
            assert(index < tmap.length);
            return tmap[index];
        }
    }

    public static class TrkInfo {
        private int startingBlock;
        private int blockCount;
        private int bitCount;

        public TrkInfo(DataBuffer data) {
            this.startingBlock = data.readUnsignedShort();
            this.blockCount = data.readUnsignedShort();
            this.bitCount = data.readInt();
        }
        public int getStartingBlock() {
            return startingBlock;
        }
        public int getBlockCount() {
            return blockCount;
        }
        public int getBitCount() {
            return bitCount;
        }
    }
}
