/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2025 by Robert Greene
 * robgreene at users.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.webcodepro.applecommander.storage.physical;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.os.dos33.DosSectorAddress;
import com.webcodepro.applecommander.util.AppleUtil;
import org.applecommander.source.Source;
import org.applecommander.util.DataBuffer;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.webcodepro.applecommander.storage.physical.NibbleCodec.*;
import static com.webcodepro.applecommander.storage.physical.NibbleOrder.DOS_SECTOR_SKEW;
import static com.webcodepro.applecommander.storage.physical.ProdosOrder.blockInterleave;
import static com.webcodepro.applecommander.storage.physical.ProdosOrder.blockOffsets;

public class WozOrder extends ImageOrder {
    // The magic bytes are read in little-endian order, so they do appear reversed here.
    public static final int WOZ1_MAGIC = 0x315a4f57;
    public static final int WOZ2_MAGIC = 0x325a4f57;
    public static final int INFO_CHUNK_ID = 0x4f464e49;
    public static final int TMAP_CHUNK_ID = 0x50414d54;
    public static final int TRKS_CHUNK_ID = 0x534b5254;
    public static final int FLUX_CHUNK_ID = 0x54495257;
    public static final int META_CHUNK_ID = 0x4154454d;

    // WOZ image representation
    private InfoChunk info;
    private Map<String,String> meta = new HashMap<>();
    private List<TmapChunk> tmap = new ArrayList<>();
    private List<TrkInfo> trks = new ArrayList<>();

    // Internal configuration settings
    private boolean blockDevice;
    private boolean trackAndSectorDevice;
    private int blocksOnDevice;
    private int sectorsPerTrack;
    private Function<Integer,byte[]> trackReader = null;
    private Function<Integer,byte[]> blockReader = this::readBlock525;
    private BiFunction<Integer,Integer,byte[]> sectorReader = this::readSector525;

    public WozOrder(Source source) {
        super(source);

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

        Consumer<byte[]> tmapReader = this::readTmapChunk525;
        while (bb.hasRemaining()) {
            int chunkId = bb.readInt();
            int chunkSize = bb.readInt();
            byte[] data = new byte[chunkSize];
            bb.read(data);
            switch (chunkId) {
                case INFO_CHUNK_ID:
                    this.info = new InfoChunk(data);
                    if (this.info.getDiskType() == 1) { // DISK II - 5.25"
                        this.blockDevice = false;
                        this.trackAndSectorDevice = true;
                        this.blocksOnDevice = 280;
                        tmapReader = this::readTmapChunk525;
                        this.blockReader = this::readBlock525;
                        this.sectorReader = this::readSector525;
                    }
                    else {  // 400K or 800K 3.5" disk
                        this.blockDevice = true;
                        this.trackAndSectorDevice = false;
                        this.blocksOnDevice = this.info.getDiskSides() * 800;
                        tmapReader = this::readTmapChunk35;
                        //this.blockReader = this::readBlock35;
                        this.sectorReader = this::readSector35;
                    }
                    break;
                case TMAP_CHUNK_ID:
                    tmapReader.accept(data);
                    break;
                case TRKS_CHUNK_ID:
                    readTrksChunk(data);
                case META_CHUNK_ID:
                    readMetaChunk(data);
                    break;
            }
        }
        // Identify 13-sector vs 16-sector
        byte[] trackData = readTrackData(0);
        sectorsPerTrack = identifySectorsPerTrack(trackData);
    }

    @Override
    public String getName() {
        return "WOZ Disk Image";
    }

    @Override
    public int getPhysicalSize() {
        // We fake it. "Size" is sometimes used to determine OS type, so we give the decoded disk size.
        return this.blocksOnDevice * 512;
    }

    @Override
    public int getSectorsPerTrack() {
        return sectorsPerTrack;
    }

    @Override
    public byte[] readBlock(int block) {
        return this.blockReader.apply(block);
    }

    public byte[] readBlock525(int block) {
        if (sectorsPerTrack == 13) {
            // 13 sector disks don't map to blocks, but the interrogation routines don't know this; faking it.
            return new byte[512];
        }
        byte[] sector1;
        byte[] sector2;
        DosSectorAddress[] sectors;
        sectors = DosOrder.blockToSectors525(block);
        sector1 = readSector(sectors[0].track, sectors[0].sector);
        sector2 = readSector(sectors[1].track, sectors[1].sector);
        byte[] blockData = new byte[Disk.BLOCK_SIZE];
        System.arraycopy(sector1, 0, blockData, 0, Disk.SECTOR_SIZE);
        System.arraycopy(sector2, 0, blockData, Disk.SECTOR_SIZE, Disk.SECTOR_SIZE);
        return blockData;
    }

//    public byte[] readBlock35(int block) {
//        DosSectorAddress addr = blockToSector35(block);
//        byte[] trackData = readTrackData(addr.track);
//        return readSectorFromTrack35(trackData, addr.track, addr.sector, getSectorsPerTrack());
//    }

    public DosSectorAddress blockToSector35(int block) {
        // 12x8, 11x8, 10x8, 9x8, 8x8 = 96+88+80+72+64 = 400 sectors per side
        int sector = block;
        int maxSectorsOnTrack = 0;
        int track = 0;
    locateBlockLoop:
        for (int i=0; i<info.getDiskSides(); i++) {
            for (int s=12; s>=8; s--) {
                maxSectorsOnTrack = s;
                for (int c=0; c<8; c++) {
                    if (sector < maxSectorsOnTrack) {
                        break locateBlockLoop;
                    }
                    sector -= s;
                    track++;
                }
            }
        }
        return new DosSectorAddress(track, sector);
    }

    @Override
    public void writeBlock(int block, byte[] data) {
        throw new RuntimeException("WOZ Disk Image does not support writing at this time");
    }

    @Override
    public boolean isBlockDevice() {
        return blockDevice;
    }

    @Override
    public boolean isTrackAndSectorDevice() {
        return trackAndSectorDevice;
    }

    @Override
    public int getBlocksOnDevice() {
        return blocksOnDevice;
    }

    @Override
    public byte[] readSector(int track, int sector) throws IllegalArgumentException {
        return this.sectorReader.apply(track, sector);
    }

    public byte[] readSector525(int track, int sector) throws IllegalArgumentException {
        if (sectorsPerTrack == 16) {
            sector = DOS_SECTOR_SKEW[sector];
            byte[] trackData = readTrackData(track);
            return readSectorFromTrack62(trackData, track, sector, getSectorsPerTrack());
        } else {
            byte[] trackData = readTrackData(track);
            return readSectorFromTrack53(trackData, track, sector, getSectorsPerTrack());
        }
    }

    public byte[] readSector35(int track, int sector) throws IllegalArgumentException {
        int block = track * 8 + blockInterleave[sector];
        byte[] blockData = readBlock(block);
        int offset = blockOffsets[sector];
        byte[] sectorData = new byte[Disk.SECTOR_SIZE];
        System.arraycopy(blockData, offset * Disk.SECTOR_SIZE,
                sectorData, 0, Disk.SECTOR_SIZE);
        return sectorData;
    }

    @Override
    public void writeSector(int track, int sector, byte[] bytes) throws IllegalArgumentException {
        throw new RuntimeException("WOZ Disk Image does not support writing at this time");
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

    private void readTmapChunk525(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        while (bb.hasRemaining()) {
            byte[] chunk = new byte[4];
            bb.get(chunk);
            tmap.add(new TmapChunk(chunk));
        }
    }

    private void readTmapChunk35(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        while (bb.hasRemaining()) {
            byte chunk = bb.get();
            tmap.add(new TmapChunk(chunk));
        }
    }

    private void readTrksChunk(byte[] data) {
        // Note: WOZ2 only
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (int i=0; i<160; i++) {
            trks.add(new TrkInfo(bb));
        }
    }

    protected byte[] readTrackData(int track) {
        return this.trackReader.apply(track);
    }

    protected byte[] readTrackDataWOZ2(int track) {
        TmapChunk map = tmap.get(track);
        int trkInfo = map.getOffset00();
        if (trkInfo == 255) trkInfo = map.getOffset25();
        if (trkInfo == 255) trkInfo = map.getOffset50();
        if (trkInfo == 255) trkInfo = map.getOffset75();
        TrkInfo trk = trks.get(trkInfo);
        byte[] rawData = readBytes(trk.getStartingBlock()*512, trk.getBlockCount()*512);
        return transformBitstream(rawData, trk.getBitCount());
    }

    protected byte[] readTrackDataWOZ1(int track) {
        final int trackLength = 6656;
        int start = 256 + (track * trackLength);
        byte[] details = readBytes(start + 6646, 10);
        int bytesUsed = AppleUtil.getWordValue(details, 0);
        int bitCount = AppleUtil.getWordValue(details, 2);
        byte[] rawData = readBytes(start, bytesUsed);
        return transformBitstream(rawData, bitCount);
    }

    protected byte[] transformBitstream(byte[] rawData, int bitCount) {
        // NOTE: Uncertain if we need to track 0's (only 2 allowed by hardware) or not.
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
            if (this.version == 2) {
                this.diskSides = bb.get();
                this.bootSectorFormat = bb.get();
                this.optimalBitTiming = bb.get();
                this.compatibleHardware = bb.getShort();
                this.requiredRAM = bb.getShort();
                this.largestTrack = bb.getShort();
            }
            if (this.version == 3) {
                this.fluxBlock = bb.getShort();
                this.largestFluxTrack = bb.getShort();
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
        private byte[] tmap;

        /** Create a 5.25" TMAP entry; one for every quarter track. */
        public TmapChunk(byte[] data) {
            if (data.length != 4) {
                throw new RuntimeException("Unexpected TMAP chunk size of " + data.length);
            }
            tmap = data;
        }
        /** Create a 3.5" TMAP entry; just one for the track. */
        public TmapChunk(byte data) {
            tmap = new byte[4];
            tmap[0] = data;
            tmap[1] = (byte)255;
            tmap[2] = (byte)255;
            tmap[3] = (byte)255;
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
