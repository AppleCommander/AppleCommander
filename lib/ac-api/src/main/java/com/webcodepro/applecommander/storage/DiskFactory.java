/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2025 by Robert Greene and others
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
package com.webcodepro.applecommander.storage;

import org.applecommander.device.*;
import org.applecommander.device.nibble.NibbleTrackReaderWriter;
import org.applecommander.hint.Hint;
import org.applecommander.image.NibbleImage;
import org.applecommander.image.WozImage;
import org.applecommander.source.Source;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The DiskFactory inspects a given Source inspect it to see if it matches filesystem structure(s).
 * Invoke via {@link Disks#inspect(Source)} which will return a Context. The Context _can be empty_.
 * If this is the case, devices can be created via the {@link Context#blockDevice()} and
 * {@link Context#trackSectorDevice()} builders.
 * <br/>
 * The builders follow the observation about what each filesystem currently uses:
 * <pre>
 * OS         13-sector  16-sector   800K    HDV
 * =========  =========  ==========  ======  ========
 * CP/M       -          CP/M        -       -
 * DOS        Physical   DOS         Pascal  -
 * Gutenberg  -          DOS         -       -
 * NakeDOS    -          Physical    -       -
 * Pascal     -          Pascal      Pascal  -
 * ProDOS     -          Pascal      Pascal  Pascal
 * RDOS       Physical   DOS         -       -
 * </pre>
 * <ul>
 * <li>13-sector disks are only track/sector devices. (And RDOS has a unique mapping to 256 byte blocks).</li>
 * <li>16-sector disks have a variety of sector mappings. It can also double as a "standard" 512 byte block,
 *     as well as a unique CP/M 1024 block device.</li>
 * <li>800K disks are only standard 512 byte block (Prodos or Pascal). OzDOS and UniDOS do have unique
 *     mappings to track and sector, but logically, they are block devices.</li>
 * <li>Anything else (larger than 800K) are "hard-disk" images and only blocks.</li>
 * </ul>
 */
public interface DiskFactory {
    void inspect(Context ctx);

    class Context {
        public final Source source;
        public final NibbleTrackReaderWriter nibbleTrackReaderWriter;
        public final List<FormattedDisk> disks = new ArrayList<>();

        public Context(Source source) {
            this.source = source;

            /* Does it have the WOZ1 or WOZ2 header? */
            int signature = source.readBytes(0, 4).readInt();
            if (WozImage.WOZ1_MAGIC == signature || WozImage.WOZ2_MAGIC == signature) {
                nibbleTrackReaderWriter = new WozImage(source);
            } else if (source.is(Hint.NIBBLE_SECTOR_ORDER) || source.isApproxEQ(DiskConstants.APPLE_140KB_NIBBLE_DISK)) {
                nibbleTrackReaderWriter = new NibbleImage(source);
            } else {
                nibbleTrackReaderWriter = null;
            }
        }

        public BlockDeviceBuilder blockDevice() {
            return new BlockDeviceBuilder(this);
        }
        public static class BlockDeviceBuilder {
            private Context ctx;
            private List<BlockDevice> devices = new ArrayList<>();
            private BlockDeviceBuilder(Context ctx) {
                this.ctx = ctx;
            }
            public BlockDeviceBuilder include16Sector(Hint hint) {
                if (ctx.nibbleTrackReaderWriter != null) {
                    Optional<TrackSectorDevice> nibble = TrackSectorNibbleDevice.identify(ctx.nibbleTrackReaderWriter);
                    if (nibble.isPresent()) {
                        TrackSectorDevice converted = switch (hint) {
                            case DOS_SECTOR_ORDER -> SkewedTrackSectorDevice.physicalToDosSkew(nibble.get());
                            case PRODOS_BLOCK_ORDER -> SkewedTrackSectorDevice.physicalToPascalSkew(nibble.get());
                            case NIBBLE_SECTOR_ORDER -> nibble.get();
                            default -> throw new RuntimeException("wrong hint type: " + hint);
                        };
                        devices.add(new TrackSectorToBlockAdapter(converted, TrackSectorToBlockAdapter.BlockStyle.PRODOS));
                    }
                }
                else if (ctx.source.isApproxBetween(DiskConstants.APPLE_140KB_DISK, DiskConstants.APPLE_160KB_DISK)) {
                    if (ctx.source.is(Hint.DOS_SECTOR_ORDER) || ctx.source.extensionLike("do")) {
                        TrackSectorDevice doDevice = new DosOrderedTrackSectorDevice(ctx.source, Hint.DOS_SECTOR_ORDER);
                        TrackSectorDevice poDevice = SkewedTrackSectorDevice.dosToPascalSkew(doDevice);
                        devices.add(new TrackSectorToBlockAdapter(poDevice, TrackSectorToBlockAdapter.BlockStyle.PRODOS));
                    }
                    else if (ctx.source.is(Hint.PRODOS_BLOCK_ORDER) || ctx.source.extensionLike("po")) {
                        devices.add(new ProdosOrderedBlockDevice(ctx.source, BlockDevice.STANDARD_BLOCK_SIZE));
                    }
                    else {
                        devices.add(new ProdosOrderedBlockDevice(ctx.source, BlockDevice.STANDARD_BLOCK_SIZE));
                        TrackSectorDevice doDevice = new DosOrderedTrackSectorDevice(ctx.source, Hint.DOS_SECTOR_ORDER);
                        TrackSectorDevice poDevice = SkewedTrackSectorDevice.dosToPascalSkew(doDevice);
                        devices.add(new TrackSectorToBlockAdapter(poDevice, TrackSectorToBlockAdapter.BlockStyle.PRODOS));
                    }
                }
                return this;
            }
            public BlockDeviceBuilder include800K() {
                if (ctx.source.isApproxEQ(DiskConstants.APPLE_800KB_DISK)) {
                    devices.add(new ProdosOrderedBlockDevice(ctx.source, BlockDevice.STANDARD_BLOCK_SIZE));
                }
                return this;
            }
            public BlockDeviceBuilder includeHDV() {
                // Anything not a floppy is included -- but if we have a device, assume it was picked up elsewhere...
                if (ctx.source.getSize() > DiskConstants.APPLE_140KB_NIBBLE_DISK && devices.isEmpty()) {
                    devices.add(new ProdosOrderedBlockDevice(ctx.source, BlockDevice.STANDARD_BLOCK_SIZE));
                }
                return this;
            }
            public List<BlockDevice> get() {
                return devices;
            }
        }

        public TrackSectorDeviceBuilder trackSectorDevice() {
            return new TrackSectorDeviceBuilder(this);
        }
        public static class TrackSectorDeviceBuilder {
            private Context ctx;
            private List<TrackSectorDevice> devices = new ArrayList<>();
            private TrackSectorDeviceBuilder(Context ctx) {
                this.ctx = ctx;
            }
            public TrackSectorDeviceBuilder include13Sector() {
                if (ctx.nibbleTrackReaderWriter != null) {
                    Optional<TrackSectorDevice> nibble = TrackSectorNibbleDevice.identify(ctx.nibbleTrackReaderWriter);
                    nibble.ifPresent(device -> {
                        if (device.getGeometry().sectorsPerTrack() == 13) {
                            devices.add(device);
                        }
                    });
                }
                else if (ctx.source.isApproxEQ(DiskConstants.APPLE_13SECTOR_DISK)) {
                    devices.add(new DosOrderedTrackSectorDevice(ctx.source));
                }
                return this;
            }
            public TrackSectorDeviceBuilder include16Sector(Hint hint) {
                assert hint == Hint.DOS_SECTOR_ORDER || hint == Hint.NIBBLE_SECTOR_ORDER;
                if (ctx.nibbleTrackReaderWriter != null) {
                    Optional<TrackSectorDevice> nibble = TrackSectorNibbleDevice.identify(ctx.nibbleTrackReaderWriter);
                    nibble.ifPresent(device -> {
                        if (device.getGeometry().sectorsPerTrack() == 16) {
                            TrackSectorDevice converted = switch (hint) {
                                case DOS_SECTOR_ORDER -> SkewedTrackSectorDevice.physicalToDosSkew(nibble.get());
                                case NIBBLE_SECTOR_ORDER -> nibble.get();
                                default -> throw new RuntimeException("wrong hint type: " + hint);
                            };
                            devices.add(converted);
                        }
                    });
                }
                else if (ctx.source.isApproxBetween(DiskConstants.APPLE_140KB_DISK, DiskConstants.APPLE_160KB_DISK)) {
                    TrackSectorDevice doDevice = null;
                    TrackSectorDevice poDevice = null;
                    if (ctx.source.is(Hint.DOS_SECTOR_ORDER) || ctx.source.extensionLike("do")) {
                        doDevice = new DosOrderedTrackSectorDevice(ctx.source, Hint.DOS_SECTOR_ORDER);
                    }
                    else if (ctx.source.is(Hint.PRODOS_BLOCK_ORDER) || ctx.source.extensionLike("po")) {
                        BlockDevice blockDevice = new ProdosOrderedBlockDevice(ctx.source, BlockDevice.STANDARD_BLOCK_SIZE);
                        poDevice = new BlockToTrackSectorAdapter(blockDevice, new ProdosBlockToTrackSectorAdapterStrategy());
                    }
                    else {
                        doDevice = new DosOrderedTrackSectorDevice(ctx.source, Hint.DOS_SECTOR_ORDER);
                        BlockDevice blockDevice = new ProdosOrderedBlockDevice(ctx.source, BlockDevice.STANDARD_BLOCK_SIZE);
                        poDevice = new BlockToTrackSectorAdapter(blockDevice, new ProdosBlockToTrackSectorAdapterStrategy());
                    }
                    switch (hint) {
                        case DOS_SECTOR_ORDER -> {
                            if (doDevice != null) {
                                devices.add(doDevice);
                            }
                            if (poDevice != null) {
                                devices.add(poDevice);
                            }
                        }
                        case NIBBLE_SECTOR_ORDER -> {
                            if (doDevice != null) {
                                devices.add(SkewedTrackSectorDevice.dosToPhysicalSkew(doDevice));
                            }
                            if (poDevice != null) {
                                devices.add(SkewedTrackSectorDevice.dosToPhysicalSkew(poDevice));
                            }
                        }
                    }
                }
                return this;
            }
            public List<TrackSectorDevice> get() {
                return devices;
            }
        }
    }
}
