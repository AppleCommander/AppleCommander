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
package io.github.applecommander.acx;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.DiskConstants;
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.os.dos33.DosFormatDisk;

import io.github.applecommander.acx.fileutil.FileUtils;

public enum SystemType {
	DOS(OrderType.DOS, SystemType::enforce140KbDisk, 
	        SystemType::copyDosSystemTracks),
	// OzdosFormatDisk is structured on top of ProDOS blocks in the implementation.
	OZDOS(OrderType.PRODOS, SystemType::enforce800KbDisk, 
	        SystemType::copyDosSystemTracks),
	// UnidosFormatDisk is structured on top of DOS track/sectors in the implementation.
	UNIDOS(OrderType.DOS, SystemType::enforce800KbDisk, 
	        SystemType::copyDosSystemTracks),
	PRODOS(OrderType.PRODOS, SystemType::enforce140KbOr800KbUpTo32MbDisk, 
	        SystemType::copyProdosSystemFiles),
	PASCAL(OrderType.PRODOS, SystemType::enforce140KbDisk, 
	        SystemType::copyPascalSystemFiles);
	
    static Logger LOG = Logger.getLogger(SystemType.class.getName());

    private OrderType defaultOrderType;
    private Function<Integer,Integer> enforceDiskSizeFn;
	private BiConsumer<FormattedDisk,FormattedDisk> copySystemFn;
	
	private SystemType(OrderType defaultOrderType,
	        Function<Integer,Integer> enforceDiskSizeFn,
	        BiConsumer<FormattedDisk,FormattedDisk> copySystemFn) {
	    this.defaultOrderType = defaultOrderType;
	    this.enforceDiskSizeFn = enforceDiskSizeFn;
		this.copySystemFn = copySystemFn;
	}

	public OrderType defaultOrderType() {
	    return defaultOrderType;
	}
	public int validateSize(int size) {
	    return enforceDiskSizeFn.apply(size);
	}
	public void copySystem(FormattedDisk target, FormattedDisk source) {
		copySystemFn.accept(target, source);
	}
	
	static int enforce140KbDisk(int size) {
        if (size != DiskConstants.APPLE_140KB_DISK) {
            LOG.warning("Setting image size to 140KB");
        }
        return DiskConstants.APPLE_140KB_DISK;
	}
	static int enforce800KbDisk(int size) {
        if (size != DiskConstants.APPLE_800KB_DISK) {
            LOG.warning("Setting image size to 800KB.");
        }
        return DiskConstants.APPLE_800KB_DISK;
	}
	static int enforce140KbOr800KbUpTo32MbDisk(int size) {
	    if (size <= DiskConstants.APPLE_140KB_DISK) {
	        return enforce140KbDisk(size);
	    }
	    if (size <= DiskConstants.APPLE_800KB_DISK) {
	        return enforce800KbDisk(size);
	    }
	    if (size > DiskConstants.APPLE_32MB_HARDDISK) {
	        LOG.warning("Setting image size to 32MB.");
	        return DiskConstants.APPLE_32MB_HARDDISK;
	    }
	    return size;
	}

	static void copyDosSystemTracks(FormattedDisk targetDisk, FormattedDisk source) {
		DosFormatDisk target = (DosFormatDisk)targetDisk;
		byte[] vtoc = target.readVtoc();
		int sectorsPerTrack = vtoc[0x35];
		// Note that this also patches T0 S0 for BOOT0
		for (int t=0; t<3; t++) {
			for (int s=0; s<sectorsPerTrack; s++) {
				target.writeSector(t, s, source.readSector(t, s));
				target.setSectorUsed(t, s, vtoc);
			}
		}
		target.writeVtoc(vtoc);
	}
	static void copyProdosSystemFiles(FormattedDisk target, FormattedDisk source) {
		// We need to explicitly fix the boot block
		target.writeBlock(0, source.readBlock(0));
		target.writeBlock(1, source.readBlock(1));
		
		try {
            FileUtils copier = new FileUtils(false);
			for (String filename : Arrays.asList("PRODOS", "BASIC.SYSTEM")) {
                FileEntry sourceFile = source.getFile(filename);
			    copier.copy(target, sourceFile);
			}
		} catch (DiskException e) {
			throw new RuntimeException(e);
		}
	}
	static void copyPascalSystemFiles(FormattedDisk target, FormattedDisk source) {
		// We need to explicitly fix the boot block
		target.writeBlock(0, source.readBlock(0));
		target.writeBlock(1, source.readBlock(1));

		// TODO; uncertain what files Pascal disks require for booting
	}
}