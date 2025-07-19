package com.webcodepro.applecommander.storage.physical;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.util.AppleUtil;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WozOrder extends NibbleOrder {
    public static final int INFO_CHUNK_ID = 0x4f464e49;
    public static final int TMAP_CHUNK_ID = 0x50414d54;
    public static final int TRKS_CHUNK_ID = 0x534b5254;
    public static final int FLUX_CHUNK_ID = 0x54495257;
    public static final int META_CHUNK_ID = 0x4154454d;

    private InfoChunk info;
    private Map<String,String> meta = new HashMap<>();
    private List<TmapChunk> tmap = new ArrayList<>();
    private List<TrkInfo> trks = new ArrayList<>();

    public WozOrder(ByteArrayImageLayout layout) {
        super(layout);

        ByteBuffer bb = ByteBuffer.wrap(layout.getDiskImage());
        bb.order(ByteOrder.LITTLE_ENDIAN);

        int sig = bb.getInt();
        int test = bb.getInt();
        if (sig != 0x325a4f57 || test != 0xa0d0aff) {
            throw new RuntimeException("Not a WOZ v2 format file.");
        }
        bb.getInt();    // ignoring CRC

        while (bb.hasRemaining()) {
            int chunkId = bb.getInt();
            int chunkSize = bb.getInt();
            byte[] data = new byte[chunkSize];
            bb.get(data);
            switch (chunkId) {
                case INFO_CHUNK_ID:
                    this.info = new InfoChunk(data);
                    if (this.info.getDiskType() != 1) {
                        throw new RuntimeException("WOZ support only supports 5.25\" disks at this time");
                    }
                    break;
                case TMAP_CHUNK_ID:
                    readTmapChunk(data);
                    break;
                case TRKS_CHUNK_ID:
                    readTrksChunk(data);
                case META_CHUNK_ID:
                    readMetaChunk(data);
                    break;
            }
        }
    }

    @Override
    public int getPhysicalSize() {
        return Disk.APPLE_140KB_NIBBLE_DISK;
    }

    private void readMetaChunk(byte[] data) {
        if (data.length > 0) {
            String meta = new String(data);
            for (String line : meta.split("\n")) {
                String[] parts = line.split("\t");
                if (parts.length == 2) {
                    this.meta.put(parts[0], parts[1]);
                }
            }
        }
    }

    private void readTmapChunk(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        while (bb.hasRemaining()) {
            byte[] chunk = new byte[4];
            bb.get(chunk);
            tmap.add(new TmapChunk(chunk));
        }
    }

    private void readTrksChunk(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (int i=0; i<160; i++) {
            trks.add(new TrkInfo(bb));
        }
    }

    @Override
    protected byte[] readTrackData(int track) {
        TmapChunk map = tmap.get(track);
        int trkInfo = map.getOffset00();
        if (trkInfo == 255) trkInfo = map.getOffset25();
        if (trkInfo == 255) trkInfo = map.getOffset50();
        if (trkInfo == 255) trkInfo = map.getOffset75();
        TrkInfo trk = trks.get(trkInfo);
        byte[] rawData = readBytes(trk.getStartingBlock()*512, trk.getBlockCount()*512);
        return transformBitstream(rawData, trk.getBitCount());
    }

    protected byte[] transformBitstream(byte[] rawData, int bitCount) {
        // NOTE: Uncertain if we need to track 0's (only 2 in a row) or not.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int bitNo = 7;
        int byteNo = 0;
        int byteVal = 0;
        while (bitCount > 0) {
            bitCount--;
            byteVal <<= 1;
            if (AppleUtil.isBitSet(rawData[byteNo], bitNo)) {
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
        return baos.toByteArray();

    }

    @Override
    protected void writeTrackData(int track, byte[] trackData) {
        throw new RuntimeException("WOZ disks do not support writing");
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

        public InfoChunk(byte[] data) {
            ByteBuffer bb = ByteBuffer.wrap(data);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            this.version = bb.get();
            this.diskType = bb.get();
            this.writeProtected = bb.get();
            this.synchrinized = bb.get();
            this.cleaned = bb.get();
            byte[] creator = new byte[32];
            bb.get(creator);
            this.creator = new String(creator);
            this.diskSides = bb.get();
            this.bootSectorFormat = bb.get();
            this.optimalBitTiming = bb.get();
            this.compatibleHardware = bb.getShort();
            this.requiredRAM = bb.getShort();
            this.largestTrack = bb.getShort();
            this.fluxBlock = bb.getShort();
            this.largestFluxTrack = bb.getShort();
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
        private byte[] tmap;

        public TmapChunk(byte[] data) {
            if (data.length != 4) {
                throw new RuntimeException("Unexpected TMAP chunk size of " + data.length);
            }
            tmap = data;
        }

        public int getOffset00() {
            return Byte.toUnsignedInt(tmap[0]);
        }
        public int getOffset25() {
            return Byte.toUnsignedInt(tmap[1]);
        }
        public int getOffset50() {
            return Byte.toUnsignedInt(tmap[2]);
        }
        public int getOffset75() {
            return Byte.toUnsignedInt(tmap[3]);
        }
    }

    public static class TrkInfo {
        private int startingBlock;
        private int blockCount;
        private int bitCount;

        public TrkInfo(ByteBuffer bb) {
            this.startingBlock = bb.getShort();
            this.blockCount = bb.getShort();
            this.bitCount = bb.getInt();
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
