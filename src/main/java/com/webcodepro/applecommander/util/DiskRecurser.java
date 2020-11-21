package com.webcodepro.applecommander.util;

import java.util.function.Consumer;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;

public class DiskRecurser {
	private boolean recursive;
	private Consumer<Disk> diskHeader = DiskRecurser::doNothing;
	private Consumer<Disk> diskFooter = DiskRecurser::doNothing;
	private Consumer<FormattedDisk> formattedDiskHeader = DiskRecurser::doNothing;
	private Consumer<FormattedDisk> formattedDiskFooter = DiskRecurser::doNothing;
	private Consumer<FileEntry> fileEntryConsumer = DiskRecurser::doNothing;
	private Consumer<DirectoryEntry> directoryEntryConsumer = DiskRecurser::doNothing;
	
	private DiskRecurser() {}
	
	public void recurse(Disk disk) throws DiskException {
		diskHeader.accept(disk);
		for (FormattedDisk formattedDisk : disk.getFormattedDisks()) {
			recurse(formattedDisk);
		}
		diskFooter.accept(disk);
	}
	
	public void recurse(FormattedDisk formattedDisk) throws DiskException {
		formattedDiskHeader.accept(formattedDisk);
		for (FileEntry fileEntry : formattedDisk.getFiles()) {
			fileEntryConsumer.accept(fileEntry);
			if (fileEntry.isDirectory() && recursive) {
				directoryEntryConsumer.accept((DirectoryEntry)fileEntry);
			}
		}
		formattedDiskFooter.accept(formattedDisk);
	}

	public void recurse(DirectoryEntry directoryEntry) throws DiskException {
		for (FileEntry fileEntry : directoryEntry.getFiles()) {
			if (fileEntry.isDirectory() && recursive) {
				directoryEntryConsumer.accept((DirectoryEntry)fileEntry);
				recurse((DirectoryEntry)fileEntry);
			} else {
				fileEntryConsumer.accept(fileEntry);
			}
		}
	}

	public static void doNothing(Disk disk) {}
	public static void doNothing(FormattedDisk formattedDisk) {}
	public static void doNothing(FileEntry fileEntry) {}
	public static void doNothing(DirectoryEntry directoryEntry) {}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		private DiskRecurser recurser = new DiskRecurser();
		
		public Builder recursive() {
			recurser.recursive = true;
			return this;
		}
		public Builder diskHeader(Consumer<Disk> consumer) {
			recurser.diskHeader = consumer;
			return this;
		}
		public Builder diskFooter(Consumer<Disk> consumer) {
			recurser.diskFooter = consumer;
			return this;
		}
		public Builder formattedDiskHeader(Consumer<FormattedDisk> consumer) {
			recurser.formattedDiskHeader = consumer;
			return this;
		}
		public Builder formattedDiskFooter(Consumer<FormattedDisk> consumer) {
			recurser.formattedDiskFooter = consumer;
			return this;
		}
		public Builder fileEntry(Consumer<FileEntry> consumer) {
			recurser.fileEntryConsumer = consumer;
			return this;
		}
		public Builder directoryEntry(Consumer<DirectoryEntry> consumer) {
			recurser.directoryEntryConsumer = consumer;
			return this;
		}
		public DiskRecurser build() {
			return recurser;
		}
	}
}
