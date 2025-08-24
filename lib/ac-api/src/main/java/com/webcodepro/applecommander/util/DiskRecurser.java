/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2022 by Robert Greene and others
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
package com.webcodepro.applecommander.util;

import java.util.function.Consumer;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;

public class DiskRecurser {
	private boolean recursive;
	private Consumer<FormattedDisk> diskHeader = DiskRecurser::doNothing;
	private Consumer<FormattedDisk> diskFooter = DiskRecurser::doNothing;
	private Consumer<FormattedDisk> formattedDiskHeader = DiskRecurser::doNothing;
	private Consumer<FormattedDisk> formattedDiskFooter = DiskRecurser::doNothing;
	private Consumer<FileEntry> fileEntryConsumer = DiskRecurser::doNothing;
	private Consumer<DirectoryEntry> directoryEntryConsumer = DiskRecurser::doNothing;
	
	private DiskRecurser() {}
	
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
		public Builder diskHeader(Consumer<FormattedDisk> consumer) {
			recurser.diskHeader = consumer;
			return this;
		}
		public Builder diskFooter(Consumer<FormattedDisk> consumer) {
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
