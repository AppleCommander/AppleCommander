/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2022 by Robert Greene
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
package com.webcodepro.applecommander.ui;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;

import com.webcodepro.applecommander.storage.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.webcodepro.applecommander.storage.FormattedDisk.FileColumnHeader;
import com.webcodepro.applecommander.util.TextBundle;
import com.webcodepro.applecommander.util.filestreamer.FileStreamer;
import com.webcodepro.applecommander.util.filestreamer.FileTuple;
import org.applecommander.device.BlockDevice;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;
import org.applecommander.util.Container;

public class DirectoryLister {
	private static TextBundle textBundle = UiBundle.getInstance();

	public static DirectoryLister text(int display) {
		return new DirectoryLister(new TextListingStrategy(display));
	}
	public static DirectoryLister csv(int display) {
		return new DirectoryLister(new CsvListingStrategy(display));
	}
	public static DirectoryLister json(int display) {
		return new DirectoryLister(new JsonListingStrategy(display));
	}

	private ListingStrategy strategy;
	
	private DirectoryLister(ListingStrategy strategy) {
		this.strategy = strategy;
	}
	
	public void list(String filename) throws DiskUnrecognizedException, IOException {
        Source source = Sources.create(filename).orElseThrow();
        DiskFactory.Context ctx = Disks.inspect(source);
        // Pulling ImageOrder from a FormattedDisk to ensure it's one we chose
		strategy.first(ctx.disks.getFirst());

		FileStreamer.forDisks(ctx.disks)
			.recursive(true)
			.includeDeleted(false)
			.beforeDisk(strategy::beforeDisk)
			.afterDisk(strategy::afterDisk)
			.stream()
			.forEach(strategy::forEach);

		strategy.last();
	}
	
	public static abstract class ListingStrategy {
		protected int display;
		protected ListingStrategy(int display) {
			this.display = display;
		}
		
		public void first(FormattedDisk d) {};
		public void beforeDisk(FormattedDisk d) {}
		public void afterDisk(FormattedDisk d) {}
		public void forEach(FileTuple f) {}
		public void last() {};
	}
	
	public static class TextListingStrategy extends ListingStrategy {
		protected TextListingStrategy(int display) {
			super(display);
		}
        @Override
		public void beforeDisk(FormattedDisk disk) {
			System.out.printf("%s %s\n", disk.getFilename(), disk.getDiskName());
		}
        @Override
		public void afterDisk(FormattedDisk disk) {
			System.out.printf("%s\n\n",
				textBundle.format("CommandLineStatus", 
					disk.getFormat(), disk.getFreeSpace(), disk.getUsedSpace()));
		}
        @Override
		public void forEach(FileTuple tuple) {
			System.out.printf("%s%s\n",
				repeat(" ", tuple.paths.size()),
				String.join(" ", tuple.fileEntry.getFileColumnData(display)));
		}
		
		private String repeat(String s, int n) {
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<n; i++) sb.append(s);
			return sb.toString();
		}

	}
	
	public static class CsvListingStrategy extends ListingStrategy {
		private CSVPrinter printer;
		public CsvListingStrategy(int display) {
			super(display);
			try {
                this.printer = new CSVPrinter(System.out, CSVFormat.DEFAULT);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
		}
        @Override
		public void beforeDisk(FormattedDisk disk) {
			try {
				printer.printRecord(disk.getFilename(), disk.getDiskName());
				printer.printRecord(disk
							.getFileColumnHeaders(display)
							.stream()
							.map(FileColumnHeader::getTitle)
							.collect(Collectors.toList()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
        @Override
		public void afterDisk(FormattedDisk disk) {
			try {
				printer.printRecord(disk.getFormat(), disk.getFreeSpace(), disk.getUsedSpace());
				printer.println();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
        @Override
		public void forEach(FileTuple tuple) {
			try {
				printer.printRecord(tuple.fileEntry.getFileColumnData(display));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public static class JsonListingStrategy extends ListingStrategy {
		private JsonObject root;
		private JsonArray disks;
		private JsonObject currentDisk;
		private JsonArray files;
		private Gson gson = new Gson();
		public JsonListingStrategy(int display) {
			super(display);
		}
        @Override
		public void first(FormattedDisk disk) {
			root = new JsonObject();
			root.addProperty("filename", disk.getFilename());
            root.addProperty("size", disk.getSource().getSize());
            if (disk.get(BlockDevice.class).isPresent()) {
                root.addProperty("device", "block device");
            }
            else if (disk.get(TrackSectorDevice.class).isPresent()) {
                root.addProperty("device", "track/sector device");
            }
            this.disks = new JsonArray();
			root.add("disks", disks);
		}
        @Override
		public void beforeDisk(FormattedDisk disk) {
			currentDisk = new JsonObject();
			disks.add(currentDisk);
			currentDisk.addProperty("diskName", disk.getDiskName());
			currentDisk.addProperty("format", disk.getFormat());
			currentDisk.addProperty("freeSpace", disk.getFreeSpace());
			currentDisk.addProperty("usedSpace", disk.getUsedSpace());
			currentDisk.addProperty("logicalDiskNumber", disk.getLogicalDiskNumber());
			files = new JsonArray();
			currentDisk.add("files", files);

		}
        @Override
		public void afterDisk(FormattedDisk disk) {
			currentDisk = null;
		}
        @Override
		public void forEach(FileTuple tuple) {
			JsonObject file = new JsonObject();
			files.add(file);
			
			List<FileColumnHeader> headers = tuple.formattedDisk.getFileColumnHeaders(display);
			List<String> columns = tuple.fileEntry.getFileColumnData(display);
			for (int i=0; i<headers.size(); i++) {
				file.addProperty(headers.get(i).getKey(), columns.get(i));
			}
		}
        @Override
		public void last() {
			System.out.println(gson.toJson(root));			
		}
	}
}
