/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-3 by Robert Greene
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
package com.webcodepro.applecommander.ui.swt.util;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * Manage image for the SWT-base AppleCommander.
 * <p>
 * As of Dec 9 2003, the design has changed.  Instead of "programming" for
 * each image, there will be a series of public-scoped constants available
 * to retrieve an image.  This will make adding an image as simple as adding
 * the constant and adding that constant to the list of images to load, instead
 * of writing two methods, adding a private method, and adding code to the
 * constructor. 
 * <p>
 * Date created: Nov 17, 2002 6:53:08 PM
 * @author Rob Greene
 */
public class ImageManager {
	public static final String ICON_DISK = "diskicon.gif"; //$NON-NLS-1$
	public static final String ICON_STANDARD_FILE_VIEW = "standardfileview.gif"; //$NON-NLS-1$
	public static final String ICON_NATIVE_FILE_VIEW = "nativefileview.gif"; //$NON-NLS-1$
	public static final String ICON_DETAIL_FILE_VIEW = "detailfileview.gif"; //$NON-NLS-1$
	public static final String ICON_IMPORT_FILE = "importfile.gif"; //$NON-NLS-1$
	public static final String ICON_EXPORT_FILE = "exportfile.gif"; //$NON-NLS-1$
	public static final String ICON_SAVE_DISK_IMAGE = "saveimage.gif"; //$NON-NLS-1$
	public static final String ICON_DELETE_FILE = "deletefile.gif"; //$NON-NLS-1$
	public static final String ICON_SHOW_DELETED_FILES = "deletedfiles.gif"; //$NON-NLS-1$
	public static final String ICON_OPEN_DISK_IMAGE = "opendisk.gif"; //$NON-NLS-1$
	public static final String ICON_NEW_DISK_IMAGE = "newdisk.gif"; //$NON-NLS-1$
	public static final String ICON_ABOUT_APPLECOMMANDER = "about.gif"; //$NON-NLS-1$
	public static final String ICON_COMPILE_FILE = "compile.gif"; //$NON-NLS-1$
	public static final String ICON_VIEW_FILE = "viewfile.gif"; //$NON-NLS-1$
	public static final String ICON_SAVE_DISK_IMAGE_AS = "saveas.gif"; //$NON-NLS-1$
	public static final String ICON_VIEW_AS_DATABASE = "database.gif"; //$NON-NLS-1$
	public static final String ICON_VIEW_IN_HEX = "hex.gif"; //$NON-NLS-1$
	public static final String ICON_VIEW_AS_IMAGE = "image.gif"; //$NON-NLS-1$
	public static final String ICON_PRINT_FILE = "print.gif"; //$NON-NLS-1$
	public static final String ICON_VIEW_IN_RAW_HEX = "raw.gif"; //$NON-NLS-1$
	public static final String ICON_VIEW_AS_SPREADSHEET = "spreadsheet.gif"; //$NON-NLS-1$
	public static final String ICON_VIEW_AS_TEXTFILE = "text.gif"; //$NON-NLS-1$
	public static final String ICON_VIEW_AS_WORDPROCESSOR = "wordprocessor.gif"; //$NON-NLS-1$
	public static final String ICON_VIEW_AS_BASIC_PROGRAM = "appleicon.gif"; //$NON-NLS-1$
	public static final String ICON_COPY = "copy.gif"; //$NON-NLS-1$
	public static final String ICON_COMPARE_DISKS = "comparedisks.gif"; //$NON-NLS-1$
	public static final String ICON_CHANGE_IMAGE_ORDER = "changeorder.gif"; //$NON-NLS-1$

	public static final String LOGO_EXPORT_WIZARD = "ExportWizardLogo.gif"; //$NON-NLS-1$
	public static final String LOGO_APPLECOMMANDER = "AppleCommanderLogo.gif"; //$NON-NLS-1$
	public static final String LOGO_DISK_IMAGE_WIZARD = "DiskImageWizardLogo.gif"; //$NON-NLS-1$
	public static final String LOGO_IMPORT_WIZARD = "ImportWizardLogo.gif"; //$NON-NLS-1$
	public static final String LOGO_COMPILE_WIZARD = "CompileWizardLogo.gif"; //$NON-NLS-1$
	public static final String LOGO_COMPARE_IMAGE_WIZARD = "CompareImageWizardLogo.gif"; //$NON-NLS-1$
	
	private Map images = new HashMap();
	private String[] imageNames = {
		// Icons:
		ICON_DISK,					ICON_STANDARD_FILE_VIEW,
		ICON_NATIVE_FILE_VIEW,		ICON_DETAIL_FILE_VIEW,
		ICON_IMPORT_FILE,			ICON_EXPORT_FILE,
		ICON_SAVE_DISK_IMAGE,		ICON_DELETE_FILE,
		ICON_SHOW_DELETED_FILES,	ICON_OPEN_DISK_IMAGE,
		ICON_NEW_DISK_IMAGE,		ICON_ABOUT_APPLECOMMANDER,
		ICON_COMPILE_FILE,			ICON_VIEW_FILE,
		ICON_SAVE_DISK_IMAGE_AS,	ICON_VIEW_AS_DATABASE,
		ICON_VIEW_IN_HEX,			ICON_VIEW_AS_IMAGE,
		ICON_PRINT_FILE,			ICON_VIEW_IN_RAW_HEX,
		ICON_VIEW_AS_SPREADSHEET,	ICON_VIEW_AS_TEXTFILE,
		ICON_VIEW_AS_WORDPROCESSOR,	ICON_VIEW_AS_BASIC_PROGRAM,
		ICON_COPY,					ICON_COMPARE_DISKS,
		ICON_CHANGE_IMAGE_ORDER,
		// Logos:
		LOGO_EXPORT_WIZARD,			LOGO_APPLECOMMANDER,
		LOGO_DISK_IMAGE_WIZARD,		LOGO_IMPORT_WIZARD,
		LOGO_COMPILE_WIZARD,		LOGO_COMPARE_IMAGE_WIZARD
	};
	/**
	 * Construct the ImageManager and load all images.
	 */
	public ImageManager(Display display) {
		for (int i=0; i<imageNames.length; i++) {
			String imageName = imageNames[i];
			Image image = createImage(display, imageName);
			images.put(imageName, image);
		}
	}
	/**
	 * Dispose of resources.
	 */
	public void dispose() {
		for (int i=0; i<imageNames.length; i++) {
			String imageName = imageNames[i];
			Image image = (Image) images.get(imageName);
			image.dispose();
			images.remove(imageName);
		}
	}
	/**
	 * Creates an image.
	 */
	private Image createImage(Display display, String path) {
		try {
			InputStream stream = getClass().getResourceAsStream(
				"/com/webcodepro/applecommander/ui/images/" + path); //$NON-NLS-1$
			if (stream != null) {
				Image image = new Image(display, stream);
				stream.close();
				return image;
			}
		} catch (Exception e) {
			// Ignored
		}
		return null;
	}
	/**
	 * Get an image.
	 */
	public Image get(String imageName) {
		return (Image) images.get(imageName);
	}
}
