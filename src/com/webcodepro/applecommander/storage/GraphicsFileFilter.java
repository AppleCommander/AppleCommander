/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002 by Robert Greene
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

import com.webcodepro.applecommander.storage.filters.imagehandlers.AppleImage;
import com.webcodepro.applecommander.util.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Filter the given file as if it were a graphics image.
 * <p>
 * Address for Apple2 HGR/DHR address is calculated from an observation of a pattern:<br>
 * line number bits:  87654321<br>
 * 87 are multipled by 0x0028<br>
 * 65 are multipled by 0x0100<br>
 * 4 is multiplied by 0x0080<br>
 * 321 are multipled by 0x0400
 * <p>
 * HGR bit values ignore the high bit, as that switches the "palette", and for B&W mode,
 * the bit does nothing.  The other 7 bits simply toggle the pixel on or off.  Double hires
 * does not follow this - it uses a real 4 bit value, but the high bit is still ignored for
 * graphics (hence, the 560 instead of 640 resolution).
 * <p>
 * Date created: Nov 3, 2002 12:06:36 PM
 * @author: Rob Greene
 */
public class GraphicsFileFilter implements FileFilter {
	public static final int MODE_HGR_BLACK_AND_WHITE = 1;
	public static final int MODE_HGR_COLOR = 2;
	public static final int MODE_DHR_BLACK_AND_WHITE = 3;
	public static final int MODE_DHR_COLOR = 4;
	public static final int MODE_SHR = 5;
	
	private int mode = MODE_HGR_COLOR;
	private AppleImage appleImage;	
	
	/**
	 * Constructor for GraphicsFileFilter.
	 */
	public GraphicsFileFilter() {
		super();
		appleImage = AppleImage.create(1,1);	// used by isCodecAvailable
	}
	
	/**
	 * Indicate if a codec is available (assist with interface requirements).
	 */
	public boolean isCodecAvailable() {
		return appleImage != null;
	}

	/**
	 * Filter the file data and produce an image.
	 * @see com.webcodepro.applecommander.storage.FileFilter#filter(FileEntry)
	 */
	public byte[] filter(FileEntry fileEntry) {
		byte[] fileData = fileEntry.getFileData();
		AppleImage image = null;
		if (isHiresColorMode()) {
			image = AppleImage.create(280, 192);
		} else if (isDoubleHiresMode()) {
			image = AppleImage.create(560, 192*2);
		} else if (isSuperHiresMode()) {
			image = AppleImage.create(640, 400);
		} else {
			return new byte[0];
		}
		image.setFileExtension(appleImage.getFileExtension());
		if (isSuperHiresMode()) {
			if (fileData.length < 32767) {	// leaves 1 byte of leeway
				fileData = AppleUtil.unpackBytes(fileData);
				if (fileData.length == 32767) {
					byte[] data = new byte[32768];
					System.arraycopy(fileData, 0, data, 0, fileData.length);
					fileData = data;
				}
			}
			int base = 0;
			byte[] pallettes = new byte[0x200];
			System.arraycopy(fileData, 0x7e00, pallettes, 0, pallettes.length);
			for (int y=0; y<200; y++) {
				byte[] lineData = new byte[160];
				System.arraycopy(fileData, base, lineData, 0, lineData.length);
				processSuperHiresLine(lineData, image, y, 
					fileData[0x7d00+y], pallettes);
				base+= lineData.length;
			}
		} else {
			for (int y=0; y<192; y++) {
				int base = (			// odd notation - bit value shifted right * hex value
					((y & 0x7) << 10)			// 00000111 * 0x0400
					| (y & 0x8) << 4			// 00001000 * 0x0080
					| (y & 0x30) << 4			// 00110000 * 0x0100
					| ((y & 0xc0) >> 6) * 0x028	// 11000000 * 0x0028
					) & 0x1fff;
				byte[] lineData = new byte[40];
				System.arraycopy(fileData, base, lineData, 0, 40);
				if (isHiresBlackAndWhiteMode()) {
					processHiresBlackAndWhiteLine(lineData, image, y);
				} else if (isHiresColorMode()) {
					processHiresColorLine(lineData, image, y);
				} else if (isDoubleHiresMode()) {
					byte[] lineData2 = new byte[40];
					System.arraycopy(fileData, base + 0x2000, lineData2, 0, 40);
					if (isDoubleHiresBlackAndWhiteMode()) {
						processDoubleHiresBlackAndWhiteLine(lineData, lineData2, image, y);
					} else if (isDoubleHiresColorMode()) {
						processDoubleHiresColorLine(lineData, lineData2, image, y);
					}
				} else {
					// oops...
				}
			}
		}
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			image.save(outputStream);
			return outputStream.toByteArray();
		} catch (IOException ex) {
			return null;
		}
	}

	/**
	 * Given a specific line in the image, process it in hires black and white
	 * mode.
	 */
	protected void processHiresBlackAndWhiteLine(byte[] lineData, AppleImage image, int y) {
		for (int x=0; x<280; x++) {
			int offset = x / 7;	// byte across row
			int bit = x % 7;		// bit to test
			byte byt = lineData[offset];
			if (AppleUtil.isBitSet(byt, bit)) {
				image.setPoint(x, y, 0xffffff);
			} else {
				image.setPoint(x, y, 0x0);
			}
		}
	}

	/**
	 * Given a specific line in the image, process it in hires color mode.
	 * HGR color is two bits to determine color - essentially resolution is
	 * 140 horizontally, but it indicates the color for two pixels.
	 * <p>
	 * The names of pixles is a bit confusion - pixel0 is really the left-most
	 * pixel (not the low-value bit).
	 * To alleviate my bad naming, here is a color table to assist:<br>
	 * <pre>
	 * Color   Bits      RGB
	 * ======= ==== ========
	 * Black1   000 0x000000
	 * Green    001 0x00ff00
	 * Violet   010 0xff00ff
	 * White1   011 0xffffff
	 * Black2   100 0x000000
	 * Orange   101 0xff8000
	 * Blue     110 0x0000ff
	 * White2   111 0xffffff
	 * </pre>
	 * Remember: bits are listed as "highbit", "pixel0", "pixel1"!
	 */
	protected void processHiresColorLine(byte[] lineData, AppleImage image, int y) {
		for (int x=0; x<140; x++) {
			int x0 = x*2;
			int x1 = x0+1;
			int offset0 = x0 / 7;	// byte across row
			int bit0 = x0 % 7;		// bit to test
			boolean pixel0 = AppleUtil.isBitSet(lineData[offset0], bit0);
			int offset1 = x1 / 7;	// byte across row
			int bit1 = x1 % 7;		// bit to test
			boolean pixel1 = AppleUtil.isBitSet(lineData[offset1], bit1);
			int color;
			if (pixel0 && pixel1) {
				color = 0xffffff;	// white
			} else if (!pixel0 && !pixel1) {
				color = 0;			// black
			} else {
				boolean highbit = pixel0 ? AppleUtil.isBitSet(lineData[offset0], 7) :
					AppleUtil.isBitSet(lineData[offset1], 7);
				if (pixel0 && highbit) {
					color = 0x0000ff;	// blue
				} else if (pixel0 && !highbit) {
					color = 0xff00ff;	// voilet
				} else if (pixel1 && !highbit) {
					color = 0x00ff00;	// green
				} else {	// pixel1 && highbit
					color = 0xff8000;	// orange
				}
			}
			if (pixel0) image.setPoint(x0, y, color);
			if (pixel1) image.setPoint(x1, y, color);
		}
	}

	/**
	 * Given a specific line in the image, process it in double hires black and white
	 * mode.
	 */
	protected void processDoubleHiresBlackAndWhiteLine(byte[] lineData1, byte[] lineData2, 
		AppleImage image, int y) {
			
		for (int x=0; x<560; x++) {
				// alternate bytes - switching memory banks
			byte[] lineData = (x % 14 < 7) ? lineData1 : lineData2;
			int rowOffset = x / 14;	// byte across row
			int bit = x % 7;			// bit to test
			byte byt = lineData[rowOffset];
			if (AppleUtil.isBitSet(byt, bit)) {
				image.setPoint(x, y*2, 0xffffff);
				image.setPoint(x, y*2+1, 0xffffff);
			} else {
				image.setPoint(x, y*2, 0x0);
				image.setPoint(x, y*2+1, 0x0);
			}
		}
	}

	/**
	 * Given a specific line in the image, process it in double hires color
	 * mode.  Treat image as 140x192 mode.
	 * <p>
	 * From the <a href='http://web.pdx.edu/~heiss/technotes/aiie/tn.aiie.03.html'>Apple2 
	 * technical note:
	 * <pre>
	 *                                          Repeated<br>
     *                                          Binary<br>
     *    Color         aux1  main1 aux2  main2 Pattern<br>
     *    Black          00    00    00    00    0000<br>
     *    Magenta        08    11    22    44    0001<br>
     *    Brown          44    08    11    22    0010<br>
     *    Orange         4C    19    33    66    0011<br>
     *    Dark Green     22    44    08    11    0100<br>
     *    Grey1          2A    55    2A    55    0101<br>
     *    Green          66    4C    19    33    0110<br>
     *    Yellow         6E    5D    3B    77    0111<br>
     *    Dark Blue      11    22    44    08    1000<br>
     *    Violet         19    33    66    4C    1001<br>
     *    Grey2          55    2A    55    2A    1010<br>
     *    Pink           5D    3B    77    6E    1011<br>
     *    Medium Blue    33    66    4C    19    1100<br>
     *    Light Blue     3B    77    6E    5D    1101<br>
     *    Aqua           77    6E    5D    3B    1110<br>
     *    White          7F    7F    7F    7F    1111
     * </pre>
	 */
	protected void processDoubleHiresColorLine(byte[] lineData1, byte[] lineData2, 
		AppleImage image, int y) {
		
		int[] bitValues = { 8,4,2,1 };
		int[] colorValues = {
				0x000000, 0xff0000, 0x800000, 0xff8000,	// black, magenta, brown, orange
				0x008000, 0x808080, 0x00ff00, 0xffff00,	// dark green, grey1, green, yellow
				0x000080, 0xff00ff, 0x808080, 0xff80c0,	// dark blue, voilet, grey2, pink
				0x0000a0, 0x0000ff, 0x00c080, 0xffffff	// medium blue, light blue, aqua, white
		};
		for (int x=0; x<560; x+=4) {
			int colorValue = 0;
			for (int b = 0; b < 4; b++) {
				int xb = x+b;
				// alternate bytes - switching memory banks
				byte[] lineData = (xb % 14 < 7) ? lineData1 : lineData2;
				int rowOffset = xb / 14;	// byte across row
				int bit = xb % 7;			// bit to test
				byte byt = lineData[rowOffset];
				if (AppleUtil.isBitSet(byt, bit)) {
					colorValue+= bitValues[b];
				}
			}
			for (int b = 0; b < 4; b++) {
				image.setPoint(x+b, y*2, colorValues[colorValue]);
				image.setPoint(x+b, y*2+1, colorValues[colorValue]);
			}
		}
	}

	/**
	 * Given a specific line in the image, process it in super hires color
	 * mode.
	 * <p>
	 * The color map varies depending upon the SCB value(s) and the pallettes
	 * stored with the image.
     * </pre>
	 */
	protected void processSuperHiresLine(byte[] lineData, 
		AppleImage image, int y, byte scb, byte[] pallettes) {
		
		int palletteNumber = (scb & 0x0f);
		boolean fillMode = (scb & 0x20) != 0;
		boolean mode320 = (scb & 0x80) == 0;
		int width = mode320 ? 320 : 640;
		int yPosition = y*2;
		int lastColorValue = 0;

		for (int x=0; x<width; x++) {
			int colorNumber;
			if (mode320) {
				int offset = (x / 2);
				int colorBits = (x % 2);
				byte byt = lineData[offset];
				if (colorBits == 1) {
					colorNumber = (byt & 0x0f);
				} else {
					colorNumber = (byt & 0xf0) >> 4;
				}
			} else {
				int offset = (x / 4);
				int colorBits = (x % 4);
				byte byt = lineData[offset];
				switch (colorBits) {
					case 0:	colorNumber = (byt & 0xc0) >> 6;
								break;
					case 1:	colorNumber = (byt & 0x30) >> 4;
								break;
					case 2:	colorNumber = (byt & 0x0c) >> 2;
								break;
					default:
					case 3:	colorNumber = (byt & 0x03);
								break;
				}
				colorNumber += 12 - (colorBits * 4);
			}
	
			int colorValue;		
			if (colorNumber == 0 && fillMode) {
				colorValue = lastColorValue;
			} else {
				int colorWord = AppleUtil.getWordValue(pallettes, 
					palletteNumber * 0x20 + colorNumber * 0x02);
				colorValue = 
					(colorWord & 0x0f00) << 12
					| (colorWord & 0x00f0) << 8
					| (colorWord & 0x000f) << 4;
			}

			int xPosition = mode320 ? x*2 : x;
			image.setPoint(xPosition, yPosition, colorValue);
			image.setPoint(xPosition, yPosition+1, colorValue);
			if (mode320) {
				image.setPoint(xPosition+1, yPosition, colorValue);
				image.setPoint(xPosition+1, yPosition+1, colorValue);
			}
		}
	}

	/**
	 * Give file extensions.
	 */
	public String[] getFileExtensions() {
		return appleImage.getAvailableExtensions();
	}

	/**
	 * Give suggested file name.
	 */
	public String getSuggestedFileName(FileEntry fileEntry) {
		String fileName = fileEntry.getFilename().trim();
		if (!fileName.toLowerCase().endsWith("." + getExtension())) {
			fileName = fileName + "." + getExtension();
		}
		return fileName;
	}
	
	/**
	 * Set the format name.
	 */
	public void setExtension(String extension) {
		appleImage.setFileExtension(extension);
	}
	
	/**
	 * Get the format name.
	 */
	public String getExtension() {
		return appleImage.getFileExtension();
	}
	
	/**
	 * Set the color mode.
	 */
	public void setMode(int mode) {
		this.mode = mode;
	}

	/**
	 * Indicates if this is configured for hires black & white mode.
	 */
	public boolean isHiresBlackAndWhiteMode() {
		return mode == MODE_HGR_BLACK_AND_WHITE;
	}
	
	/**
	 * Indicates if this is configured for hires color mode.
	 */
	public boolean isHiresColorMode() {
		return mode == MODE_HGR_COLOR;
	}
	
	/**
	 * Indicates if this is configured for double hires black & white mode.
	 */
	public boolean isDoubleHiresBlackAndWhiteMode() {
		return mode == MODE_DHR_BLACK_AND_WHITE;
	}
	
	/**
	 * Indicates if this is configured for double hires color mode.
	 */
	public boolean isDoubleHiresColorMode() {
		return mode == MODE_DHR_COLOR;
	}
	
	/**
	 * Indicates if this is configured for super hires 16 color mode.
	 */
	public boolean isSuperHiresMode() {
		return mode == MODE_SHR;
	}
	
	/**
	 * Indicates if this is a hires mode.
	 */
	public boolean isHiresMode() {
		return isHiresBlackAndWhiteMode() || isHiresColorMode();
	}
	
	/**
	 * Indicates if this is a double hires mode.
	 */
	public boolean isDoubleHiresMode() {
		return isDoubleHiresBlackAndWhiteMode() || isDoubleHiresColorMode();
	}
}
