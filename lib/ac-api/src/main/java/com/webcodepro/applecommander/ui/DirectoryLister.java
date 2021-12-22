package com.webcodepro.applecommander.ui;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskUnrecognizedException;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.FormattedDisk.FileColumnHeader;
import com.webcodepro.applecommander.util.TextBundle;
import com.webcodepro.applecommander.util.filestreamer.FileStreamer;
import com.webcodepro.applecommander.util.filestreamer.FileTuple;

public class DirectoryLister {
	private static TextBundle textBundle = UiBundle.getInstance();

	public static DirectoryLister text(int display) {
		return new DirectoryLister(new TextListingStrategy(display));
	}
	public static DirectoryLister csv(int display) {
		try {
			return new DirectoryLister(new CsvListingStrategy(display));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	public static DirectoryLister json(int display) {
		return new DirectoryLister(new JsonListingStrategy(display));
	}

	private ListingStrategy strategy;
	
	private DirectoryLister(ListingStrategy strategy) {
		this.strategy = strategy;
	}
	
	public void list(String filename) throws DiskUnrecognizedException, IOException {
		Disk disk = new Disk(filename);
		strategy.first(disk);

		FileStreamer.forDisk(disk)
			.recursive(true)
			.includeDeleted(false)
			.beforeDisk(strategy::beforeDisk)
			.afterDisk(strategy::afterDisk)
			.stream()
			.forEach(strategy::forEach);

		strategy.last(disk);
	}
	
	private static abstract class ListingStrategy {
		protected int display;
		protected ListingStrategy(int display) {
			this.display = display;
		}
		
		protected void first(Disk d) {};
		protected void beforeDisk(FormattedDisk d) {}
		protected void afterDisk(FormattedDisk d) {}
		protected void forEach(FileTuple f) {}
		protected void last(Disk d) {};
	}
	
	private static class TextListingStrategy extends ListingStrategy {
		protected TextListingStrategy(int display) {
			super(display);
		}
		protected void beforeDisk(FormattedDisk disk) {
			System.out.printf("%s %s\n", disk.getFilename(), disk.getDiskName());
		}
		protected void afterDisk(FormattedDisk disk) {
			System.out.printf("%s\n\n",
				textBundle.format("CommandLineStatus", 
					disk.getFormat(), disk.getFreeSpace(), disk.getUsedSpace()));
		}
		protected void forEach(FileTuple tuple) {
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
	
	private static class CsvListingStrategy extends ListingStrategy {
		private CSVPrinter printer;
		protected CsvListingStrategy(int display) throws IOException {
			super(display);
			this.printer = new CSVPrinter(System.out, CSVFormat.DEFAULT);
		}
		protected void beforeDisk(FormattedDisk disk) {
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
		protected void afterDisk(FormattedDisk disk) {
			try {
				printer.printRecord(disk.getFormat(), disk.getFreeSpace(), disk.getUsedSpace());
				printer.println();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		protected void forEach(FileTuple tuple) {
			try {
				printer.printRecord(tuple.fileEntry.getFileColumnData(display));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private static class JsonListingStrategy extends ListingStrategy {
		private JsonObject root;
		private JsonArray disks;
		private JsonObject currentDisk;
		private JsonArray files;
		private Gson gson = new Gson();
		protected JsonListingStrategy(int display) {
			super(display);
		}
		protected void first(Disk disk) {
			root = new JsonObject();
			root.addProperty("filename", disk.getFilename());
			root.addProperty("order", disk.getOrderName());
			root.addProperty("physicalSize", disk.getPhysicalSize());
			this.disks = new JsonArray();
			root.add("disks", disks);
		}
		protected void beforeDisk(FormattedDisk disk) {
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
		protected void afterDisk(FormattedDisk disk) {
			currentDisk = null;
		}
		protected void forEach(FileTuple tuple) {
			JsonObject file = new JsonObject();
			files.add(file);
			
			List<FileColumnHeader> headers = tuple.formattedDisk.getFileColumnHeaders(display);
			List<String> columns = tuple.fileEntry.getFileColumnData(display);
			for (int i=0; i<headers.size(); i++) {
				file.addProperty(headers.get(i).getKey(), columns.get(i));
			}
		}
		protected void last(Disk disk) {
			System.out.println(gson.toJson(root));			
		}
	}
}
