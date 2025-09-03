/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2019-2022 by Robert Greene and others
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
package io.github.applecommander.acx.command;

import java.util.Optional;
import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.DiskFactory;
import com.webcodepro.applecommander.storage.Disks;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.os.dos33.DosFormatDisk;
import com.webcodepro.applecommander.storage.os.pascal.PascalFormatDisk;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;

import io.github.applecommander.acx.OrderType;
import io.github.applecommander.acx.SystemType;
import io.github.applecommander.acx.base.ReusableCommandOptions;
import io.github.applecommander.acx.converter.DataSizeConverter;
import org.applecommander.codec.Nibble62Disk525Codec;
import org.applecommander.device.*;
import org.applecommander.hint.Hint;
import org.applecommander.image.NibbleImage;
import org.applecommander.os.dos.OzdosAdapterStrategy;
import org.applecommander.os.dos.UnidosAdapterStrategy;
import org.applecommander.source.DataBufferSource;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "create", description = "Create a disk image.",
		 aliases = { "mkdisk" })
public class CreateDiskCommand extends ReusableCommandOptions {
    private static Logger LOG = Logger.getLogger(CreateDiskCommand.class.getName());

    @Option(names = { "-d", "--disk" }, description = "Image to create [$ACX_DISK_NAME].", required = true,
            defaultValue = "${ACX_DISK_NAME}")
    private String imageName;

    @ArgGroup(multiplicity = "1", heading = "%nOperating System Selection:%n")
    private SystemSelection systemSelection;
    
    @ArgGroup(heading = "%nDisk Sector Ordering Selection:%n")
    private OrderSelection orderSelection = new OrderSelection();
    
    @Option(names = { "-s", "--size" }, defaultValue = "140kb", converter = DataSizeConverter.class, 
            description = "Select disk size (examples: 140K, 800K, 10M).")
    private int size;
    
    @Option(names = { "-f", "--format" }, 
            description = "Disk to copy system files/tracks/boot sector from.")
    private String formatSource;
    
    @Option(names = { "-n", "--name" }, defaultValue = "NEW.DISK", 
            description = "Disk Volume name (ProDOS/Pascal).")
    private String diskName;

    @Override
    public int handleCommand() throws Exception {
        SystemType systemType = systemSelection.get();

        // This allows a defaulted OrderType to be adjusted based on SystemType.
        OrderType actualOrderType = orderSelection.get().orElse(systemType.defaultOrderType());

        // Size is constrained in DOS and Pascal
        int correctedSize = systemType.validateSize(size);
    	
        LOG.info(() -> String.format("Creating %s image of type %s (%s).", 
                DataSizeConverter.format(correctedSize), systemType, actualOrderType));
    	
        Source source = DataBufferSource.create(correctedSize, imageName).get();
        BlockDevice blockDevice = new ProdosOrderedBlockDevice(source, BlockDevice.STANDARD_BLOCK_SIZE);
    	FormattedDisk[] disks = switch (systemType) {
            case DOS -> {
                TrackSectorDevice sectorDevice = switch (actualOrderType) {
                    case DOS -> new DosOrderedTrackSectorDevice(source, Hint.DOS_SECTOR_ORDER);
                    case NIBBLE -> new TrackSectorNibbleDevice(new NibbleImage(source), DiskMarker.disk525sector16(),
                            new Nibble62Disk525Codec(), 16);
                    case PRODOS -> new BlockToTrackSectorAdapter(blockDevice, new ProdosBlockToTrackSectorAdapterStrategy());
                };
                yield DosFormatDisk.create(imageName, sectorDevice);
            }
            case OZDOS -> DosFormatDisk.create(imageName, blockDevice, OzdosAdapterStrategy.values());
            case UNIDOS -> DosFormatDisk.create(imageName, blockDevice, UnidosAdapterStrategy.values());
            case PRODOS -> ProdosFormatDisk.create(imageName, diskName, blockDevice);
            case PASCAL -> PascalFormatDisk.create(imageName, diskName, blockDevice);
        };
    	
    	if (formatSource != null) {
            Source fsource = Sources.create(formatSource).orElseThrow();
            DiskFactory.Context ctx = Disks.inspect(fsource);
    		systemType.copySystem(disks[0], ctx.disks.getFirst());
    	}
    	
    	saveDisk(disks[0]);
    	
        return 0;
    }
    
    private static class SystemSelection {
        private SystemType systemType;
        
        public SystemType get() {
            return systemType;
        }
        
        @Option(names = "--dos", description = "DOS formatted disk.")
        public void selectDos(boolean flag) {
            systemType = SystemType.DOS;
        }
        @Option(names = "--ozdos", description = "OzDOS 800K formatted disk.")
        public void selectOzdos(boolean flag) {
            systemType = SystemType.OZDOS;
        }
        @Option(names = "--unidos", description = "UniDOS 800K formatted disk.")
        public void selectUnidos(boolean flag) {
            systemType = SystemType.UNIDOS;
        }
        @Option(names = "--pascal", description = "Pascal formatted disk.")
        public void selectPascal(boolean flag) {
            systemType = SystemType.PASCAL;
        }
        @Option(names = "--prodos", description = "ProDOS formatted disk.")
        public void selectProdos(boolean flag) {
            systemType = SystemType.PRODOS;
        }
    }
    
    private static class OrderSelection {
        private Optional<OrderType> orderType = Optional.empty();
        
        public Optional<OrderType> get() {
            return orderType;
        }

        @Option(names = { "--dos-order" }, description = "DOS ordered sectors.")
        public void selectDosOrder(boolean flag) {
            orderType = Optional.of(OrderType.DOS);
        }
        @Option(names = { "--nibble-order" }, description = "DOS ordered, nibble encoded sectors.")
        public void selectNibbleOrder(boolean flag) {
            orderType = Optional.of(OrderType.NIBBLE);
        }
        @Option(names = { "--prodos-order" }, description = "ProDOS ordered sectors/blocks.")
        public void selectProdosOrder(boolean flag) {
            orderType = Optional.of(OrderType.PRODOS);
        }
    }
}
