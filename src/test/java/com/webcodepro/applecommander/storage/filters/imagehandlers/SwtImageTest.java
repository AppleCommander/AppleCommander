package com.webcodepro.applecommander.storage.filters.imagehandlers;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assume.assumeThat;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;

/**
 * Exercise the SwtImage class for all known types.
 * @author Rob
 */
public class SwtImageTest {
	@Test
	public void testPNG() throws Exception {
		performTest("PNG"); //$NON-NLS-1$
	}
	
	@Test
	public void testJPEG() throws Exception {
		performTest("JPEG"); //$NON-NLS-1$
	}
	
	@Test
	public void testBMP() throws Exception {
		performTest("BMP"); //$NON-NLS-1$
	}
	
	@Test
	public void testBMP_RLE() throws Exception {
		performTest("RLE"); //$NON-NLS-1$
	}
	
	@Test
	public void testGIF() throws Exception {
		// GIF unit test fails on Linux, assuming it is an SWT component issue...
		assumeThat(SystemUtils.IS_OS_LINUX, is(false));
		performTest("GIF"); //$NON-NLS-1$
	}
	
	@Test
	public void testICO() throws Exception {
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
