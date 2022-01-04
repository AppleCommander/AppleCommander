package io.github.applecommander.filters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.os.dos33.DosFileEntry;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFileEntry;

import io.github.applecommander.applesingle.AppleSingle;

/**
 *	A FileFilter to write each file to an independent AppleSingle file. 
 */
public class AppleSingleFileFilter implements FileFilter {
	@Override
	public byte[] filter(FileEntry fileEntry) {
		try {
			AppleSingle.Builder builder = AppleSingle.builder()
					.dataFork(fileEntry.getFileData())
					.realName(fileEntry.getFilename());
			if (fileEntry instanceof ProdosFileEntry) {
				handleProDOS(builder, (ProdosFileEntry)fileEntry);
			}
			else if (fileEntry instanceof DosFileEntry) {
				handleDOS(builder, (DosFileEntry)fileEntry);
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			builder.build().save(baos);
			return baos.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	protected void handleProDOS(AppleSingle.Builder builder, ProdosFileEntry prodos) {
		// We can't get the "access" byte so reconstructing it...
		int access = (prodos.canDestroy() ? 0x80 : 0x00)
				   | (prodos.canRename() ? 0x40 : 0x00)
				   | (prodos.canWrite() ? 0x02 : 0x00)
				   | (prodos.canRead() ? 0x01 : 0x00);
		builder.access(access);
		builder.auxType(prodos.getAuxiliaryType());
		builder.creationDate(prodos.getCreationDate().toInstant());
		builder.fileType(prodos.getFiletypeByte());
		builder.modificationDate(prodos.getLastModificationDate().toInstant());
	}
	protected void handleDOS(AppleSingle.Builder builder, DosFileEntry dos) {
		switch (dos.getFiletype()) {
		// 0x00 T
		case "T":
			builder.fileType(0x04);		// TXT
			break;
		// 0x01 I
		case "I":
			builder.fileType(0xfa);		// INT
			break;
		// 0x02 A
		case "A":
			builder.fileType(0xfc);		// BAS
			break;
		// 0x04 B
		case "B":
			builder.fileType(0x06);		// BIN
			//builder.auxType(???)		// FIXME address is not exposed?
			break;
		// The rest we just default
		default:
			builder.fileType(0xf1);		// $F1 (???)
			break;
		}
		builder.access(dos.isLocked() ? 0x01 : 0xc3);
	}

	@Override
	public String getSuggestedFileName(FileEntry fileEntry) {
		String fileName = fileEntry.getFilename().trim();
		if (!fileName.toLowerCase().endsWith(".as")) {
			fileName = fileName + ".as";
		}
		return fileName;
	}
}
