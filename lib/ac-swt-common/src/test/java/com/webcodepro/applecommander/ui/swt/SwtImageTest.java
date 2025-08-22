/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2003-2022 by Robert Greene and others
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
package com.webcodepro.applecommander.ui.swt;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Exercise the SwtImage class for all known types.
 * @author Rob
 */
public class SwtImageTest {
	@BeforeEach
	public void beforeMethod() {
		assumeTrue(System.getenv("AC_SKIP_SWT_TESTS") == null);
	}

	@Test
	public void testPNG() throws Exception {
		// SwtImage unit test fails on Mac, appears to be the thread deal. Disabling for now.
		assumeFalse(SystemUtils.IS_OS_MAC);
		// ... and on Rasbian 32-bit; disabling.
		assumeFalse(SystemUtils.IS_OS_LINUX && "arm".equals(SystemUtils.OS_ARCH));

		performTest("PNG"); //$NON-NLS-1$
	}
	
	@Test
	public void testJPEG() throws Exception {
		// SwtImage unit test fails on Mac, appears to be the thread deal. Disabling for now.
		assumeFalse(SystemUtils.IS_OS_MAC);
		// ... and on Rasbian 32-bit; disabling.
		assumeFalse(SystemUtils.IS_OS_LINUX && "arm".equals(SystemUtils.OS_ARCH));

		performTest("JPEG"); //$NON-NLS-1$
	}
	
	@Test
	public void testBMP() throws Exception {
		// SwtImage unit test fails on Mac, appears to be the thread deal. Disabling for now.
		assumeFalse(SystemUtils.IS_OS_MAC);
		// ... and on Rasbian 32-bit; disabling.
		assumeFalse(SystemUtils.IS_OS_LINUX && "arm".equals(SystemUtils.OS_ARCH));

		performTest("BMP"); //$NON-NLS-1$
	}
	
	@Test
	public void testBMP_RLE() throws Exception {
		// SwtImage unit test fails on Mac, appears to be the thread deal. Disabling for now.
		assumeFalse(SystemUtils.IS_OS_MAC);
		// ... and on Rasbian 32-bit; disabling.
		assumeFalse(SystemUtils.IS_OS_LINUX && "arm".equals(SystemUtils.OS_ARCH));

		performTest("RLE"); //$NON-NLS-1$
	}
	
	@Test
	public void testGIF() throws Exception {
		// SwtImage unit test fails on Mac, appears to be the thread deal. Disabling for now.
		assumeFalse(SystemUtils.IS_OS_MAC);

		performTest("GIF"); //$NON-NLS-1$
	}
	
	@Test
	public void testICO() throws Exception {
		// SwtImage unit test fails on Mac, appears to be the thread deal. Disabling for now.
		assumeFalse(SystemUtils.IS_OS_MAC);
		// ... and on Rasbian 32-bit; disabling.
		assumeFalse(SystemUtils.IS_OS_LINUX && "arm".equals(SystemUtils.OS_ARCH));

		performTest("ICO"); //$NON-NLS-1$
	}
	
	protected void performTest(String imageType) throws Exception {
		int height = 100;
		int width = 100;
		SwtImage image = new SwtImage(width, height);
		image.setFileExtension(imageType);
		int[] colors = { 
			0xff0000, 0xff0000, 0xff0000, 0xff0000, 0xff0000,	// red
			0x00ff00, 0x00ff00, 0x00ff00, 0x00ff00, 0x00ff00,	// green
			0x0000ff, 0x0000ff, 0x0000ff, 0x0000ff, 0x0000ff,	// blue
			0xffff00, 0xffff00, 0xffff00, 0xffff00, 0xffff00,	// red+green
			0xff00ff, 0xff00ff, 0xff00ff, 0xff00ff, 0xff00ff,	// purple
			0x00ffff, 0x00ffff, 0x00ffff, 0x00ffff, 0x00ffff,	// green+blue
			0x000000, 0x000000, 0x000000, 0x000000, 0x000000,	// black
			0xffffff, 0xffffff, 0xffffff, 0xffffff, 0xffffff	// white
		};
		for (int y=0; y<height; y++) {
			int color= colors[y % colors.length];
			for (int x=0; x<width; x++) {
				image.setPoint(x, y, color);
			}
		}
		File tempImageFile = File.createTempFile("TestImage-", "." + imageType);
		tempImageFile.deleteOnExit();
		image.save(new FileOutputStream(tempImageFile));
	}
}
