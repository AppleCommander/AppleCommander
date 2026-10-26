/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2026 by Robert Greene and others
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
package org.applecommander.filestore;

import com.webcodepro.applecommander.storage.DiskConstants;
import org.applecommander.device.*;
import org.applecommander.device.nibble.NibbleTrackReaderWriter;
import org.applecommander.hint.Hint;
import org.applecommander.image.NibbleImage;
import org.applecommander.image.WozImage;
import org.applecommander.source.Source;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/// The FileStoreFactory inspects a given Source to see if it matches any known file store(s).
/// Invoke via [FileStores#inspect(Source)] which will return a Context. The Context _can be empty_.
/// If this is the case, devices can be created via the [FileStoreFactory.Context#blockDevice()] and
/// [Context#trackSectorDevice()] builders.
///
/// The builders follow the observation about what each filesystem currently uses:
///
/// | OS        | 13-sector | 16-sector | 800K   | HDV    |
/// | :---      | :---      | :---      | :---   | :---   |
/// | CP/M      | -         | CP/M      | -      | -      |
/// | DOS       | Physical  | DOS       | Pascal | -      |
/// | GameDOS   | -         | DOS       | -      | -      |
/// | Gutenberg | -         | DOS       | -      | -      |
/// | NakedOS   | -         | Physical  | -      | -      |
/// | Pascal    | -         | Pascal    | Pascal | Pascal |
/// | RDOS      | Physical  | DOS       | -      | -      |
///
/// * 13-sector disks are only track/sector devices. (And RDOS has a unique mapping to 256 byte blocks).
/// * 16-sector disks have a variety of sector mappings. It can also double as a "standard" 512 byte block,
///   as well as a unique CP/M 1024 block device.
/// * 800K disks are only standard 512 byte block (Prodos or Pascal). OzDOS and UniDOS do have unique
///   mappings to track and sector, but logically, they are block devices.
/// * Anything else (larger than 800K) are "hard-disk" images and only blocks.
///
/// Note that much of this logic is deferred to the "old" AppleCommander APIs for the time being.
public interface FileStoreFactory {
    void inspect(Context ctx);

    class Context {
        public final Source source;
        public final NibbleTrackReaderWriter nibbleTrackReaderWriter;
        public final List<FileStore> fileStores = new ArrayList<>();
        public final List<Message> messages = new ArrayList<>();

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
            private final Context ctx;
            private final List<BlockDevice> devices = new ArrayList<>();
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
                        TrackSectorDevice doDevice = new OrderedTrackSectorDevice(ctx.source, SectorOrder.DOS);
                        TrackSectorDevice poDevice = SkewedTrackSectorDevice.dosToPascalSkew(doDevice);
                        devices.add(new TrackSectorToBlockAdapter(poDevice, TrackSectorToBlockAdapter.BlockStyle.PRODOS));
                    }
                    else if (ctx.source.is(Hint.PRODOS_BLOCK_ORDER) || ctx.source.extensionLike("po")) {
                        devices.add(new ProdosOrderedBlockDevice(ctx.source, BlockDevice.STANDARD_BLOCK_SIZE));
                    }
                    else {
                        devices.add(new ProdosOrderedBlockDevice(ctx.source, BlockDevice.STANDARD_BLOCK_SIZE));
                        TrackSectorDevice doDevice = new OrderedTrackSectorDevice(ctx.source, SectorOrder.DOS);
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
            private final Context ctx;
            private final List<TrackSectorDevice> devices = new ArrayList<>();
            private TrackSectorDeviceBuilder(Context ctx) {
                this.ctx = ctx;
            }
            /// An alternate mechanism to test disks. It builds the device and lets the callback function perform the check.
            /// There is no additional mapping, that is expected to occur with the callback. Note that the device has
            /// a hint regarding sector ordering.
            public TrackSectorDeviceBuilder check13Sector(Consumer<TrackSectorDevice> check) {
                if (ctx.nibbleTrackReaderWriter != null) {
                    Optional<TrackSectorDevice> nibble = TrackSectorNibbleDevice.identify(ctx.nibbleTrackReaderWriter);
                    nibble.ifPresent(device -> {
                        if (device.getGeometry().sectorsPerTrack() == 13) {
                            check.accept(device);
                        }
                    });
                }
                else if (ctx.source.isApproxEQ(DiskConstants.APPLE_13SECTOR_DISK)) {
                    TrackSectorDevice device = new OrderedTrackSectorDevice(ctx.source, SectorOrder.NIBBLE);
                    check.accept(device);
                }
                return this;
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
                    devices.add(new OrderedTrackSectorDevice(ctx.source, SectorOrder.NIBBLE));
                }
                return this;
            }
            /// An alternate mechanism to test disks. It builds the device and lets the callback function perform the check.
            /// There is no additional mapping, that is expected to occur with the callback. Note that the device has
            /// a hint regarding sector ordering, if applicable. (`*.dsk` image are _usually_ DOS ordered but are also
            /// sometimes ProDOS ordered.)
            public TrackSectorDeviceBuilder check16Sector(Consumer<TrackSectorDevice> check) {
                if (ctx.nibbleTrackReaderWriter != null) {
                    Optional<TrackSectorDevice> nibble = TrackSectorNibbleDevice.identify(ctx.nibbleTrackReaderWriter);
                    nibble.ifPresent(device -> {
                        if (device.getGeometry().sectorsPerTrack() == 16) {
                            check.accept(device);
                        }
                    });
                }
                else if (ctx.source.isApproxBetween(DiskConstants.APPLE_140KB_DISK, DiskConstants.APPLE_160KB_DISK)) {
                    if (ctx.source.is(Hint.DOS_SECTOR_ORDER) || ctx.source.extensionLike("do")) {
                        TrackSectorDevice device = new OrderedTrackSectorDevice(ctx.source, SectorOrder.DOS);
                        check.accept(device);
                    } else if (ctx.source.is(Hint.PRODOS_BLOCK_ORDER) || ctx.source.extensionLike("po")) {
                        TrackSectorDevice device = new OrderedTrackSectorDevice(ctx.source, SectorOrder.PRODOS);
                        check.accept(device);
                    } else {
                        TrackSectorDevice doDevice = new OrderedTrackSectorDevice(ctx.source, SectorOrder.DOS);
                        check.accept(doDevice);
                        TrackSectorDevice poDevice = new OrderedTrackSectorDevice(ctx.source, SectorOrder.PRODOS);
                        check.accept(poDevice);
                    }
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
                        doDevice = new OrderedTrackSectorDevice(ctx.source, SectorOrder.DOS);
                    }
                    else if (ctx.source.is(Hint.PRODOS_BLOCK_ORDER) || ctx.source.extensionLike("po")) {
                        BlockDevice blockDevice = new ProdosOrderedBlockDevice(ctx.source, BlockDevice.STANDARD_BLOCK_SIZE);
                        poDevice = new BlockToTrackSectorAdapter(blockDevice, new ProdosBlockToTrackSectorAdapterStrategy());
                    }
                    else {
                        doDevice = new OrderedTrackSectorDevice(ctx.source, SectorOrder.DOS);
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

    record Message(Level level, String message) {
        public static Message info(String fmt, Object... args) {
            return new Message(Level.INFO, String.format(fmt, args));
        }
        public static Message warning(String fmt, Object... args) {
            return new Message(Level.WARNING, String.format(fmt, args));
        }
        public static Message error(String fmt, Object... args) {
            return new Message(Level.ERROR, String.format(fmt, args));
        }
    }

    enum Level {
        INFO,
        WARNING,
        ERROR
    }
}