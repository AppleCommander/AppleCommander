package io.github.applecommander.acx;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.os.dos33.DosFormatDisk;
import com.webcodepro.applecommander.storage.physical.ByteArrayImageLayout;
import com.webcodepro.applecommander.storage.physical.DosOrder;
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import com.webcodepro.applecommander.storage.physical.ProdosOrder;

import io.github.applecommander.acx.converter.DataSizeConverter;
import io.github.applecommander.acx.fileutil.FileUtils;

public enum SystemType {
	DOS(SystemType::createDosImageOrder, SystemType::copyDosSystemTracks),
	OZDOS(SystemType::create800kDosImageOrder, SystemType::copyDosSystemTracks),
	UNIDOS(SystemType::create800kDosImageOrder, SystemType::copyDosSystemTracks),
	PRODOS(SystemType::createProdosImageOrder, SystemType::copyProdosSystemFiles),
	PASCAL(SystemType::createProdosImageOrder, SystemType::copyPascalSystemFiles);
	
    private static Logger LOG = Logger.getLogger(SystemType.class.getName());

	private Function<Integer,ImageOrder> createImageOrderFn;
	private BiConsumer<FormattedDisk,FormattedDisk> copySystemFn;
	
	private SystemType(Function<Integer,ImageOrder> createImageOrderFn,
			BiConsumer<FormattedDisk,FormattedDisk> copySystemFn) {
		this.createImageOrderFn = createImageOrderFn;
		this.copySystemFn = copySystemFn;
	}

	public ImageOrder createImageOrder(int size) {
		return createImageOrderFn.apply(size);
	}
	public void copySystem(FormattedDisk target, FormattedDisk source) {
		copySystemFn.accept(target, source);
	}

	private static ImageOrder createDosImageOrder(int size) {
		ByteArrayImageLayout layout = new ByteArrayImageLayout(new byte[size]);
		return new DosOrder(layout);
	}
	private static ImageOrder create800kDosImageOrder(int size) {
		if (size != 800 * DataSizeConverter.KB) {
			LOG.warning("Setting image size to 800KB.");
		}
		ByteArrayImageLayout layout = new ByteArrayImageLayout(new byte[800 * DataSizeConverter.KB]);
		return new DosOrder(layout);
	}
	private static ImageOrder createProdosImageOrder(int size) {
		ByteArrayImageLayout layout = new ByteArrayImageLayout(size);
		return new ProdosOrder(layout);
	}
	
	private static void copyDosSystemTracks(FormattedDisk targetDisk, FormattedDisk source) {
		DosFormatDisk target = (DosFormatDisk)targetDisk;
		// FIXME messing with the VTOC should be handled elsewhere 
		byte[] vtoc = source.readSector(DosFormatDisk.CATALOG_TRACK, DosFormatDisk.VTOC_SECTOR);
		int sectorsPerTrack = vtoc[0x35];
		// Note that this also patches T0 S0 for BOOT0
		for (int t=0; t<3; t++) {
			for (int s=0; s<sectorsPerTrack; s++) {
				target.writeSector(t, s, source.readSector(t, s));
				target.setSectorUsed(t, s, vtoc);
			}
		}
	}
	private static void copyProdosSystemFiles(FormattedDisk target, FormattedDisk source) {
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
	private static void copyPascalSystemFiles(FormattedDisk target, FormattedDisk source) {
		// We need to explicitly fix the boot block
		target.writeBlock(0, source.readBlock(0));
		target.writeBlock(1, source.readBlock(1));

		// TODO; uncertain what files Pascal disks require for booting
	}
}