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
package com.webcodepro.applecommander.ui.swt;

import java.io.InputStream;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * Manage image for the SWT-base AppleCommander.
 * <p>
 * Date created: Nov 17, 2002 6:53:08 PM
 * @author: Rob Greene
 */
public class ImageManager {
	private Image logoImage;
	private Image diskIcon;
	private Image standardFileViewIcon;
	private Image nativeFileViewIcon;
	private Image detailFileViewIcon;
	private Image importFileIcon;
	private Image exportFileIcon;
	private Image saveImageIcon;
	private Image deleteFileIcon;
	private Image deletedFilesIcon;
	private Image exportWizardLogo;
	private Image openDiskIcon;
	private Image newDiskIcon;
	private Image aboutIcon;
	private Image diskImageWizardLogo;
	private Image importWizardLogo;
	private Image compileIcon;
	/**
	 * Construct the ImageManager.
	 */
	protected ImageManager(Display display) {
		diskIcon = createImage(display, "diskicon.gif");
		standardFileViewIcon = createImage(display, "standardfileview.gif");
		nativeFileViewIcon = createImage(display, "nativefileview.gif");
		detailFileViewIcon = createImage(display, "detailfileview.gif");
		importFileIcon = createImage(display, "importfile.gif");
		exportFileIcon = createImage(display, "exportfile.gif");
		saveImageIcon = createImage(display, "saveimage.gif");
		deleteFileIcon = createImage(display, "deletefile.gif");
		deletedFilesIcon = createImage(display, "deletedfiles.gif");
		exportWizardLogo = createImage(display, "ExportWizardLogo.gif");
		logoImage = createImage(display, "AppleCommanderLogo.gif");
		openDiskIcon = createImage(display, "opendisk.gif");
		newDiskIcon = createImage(display, "newdisk.gif");
		aboutIcon = createImage(display, "about.gif");
		diskImageWizardLogo = createImage(display, "DiskImageWizardLogo.gif");
		importWizardLogo = createImage(display, "ImportWizardLogo.gif");
		compileIcon = createImage(display, "compile.gif");
	}
	/**
	 * Dispose of resources.
	 */
	public void dispose() {
		diskIcon.dispose();
		standardFileViewIcon.dispose();
		nativeFileViewIcon.dispose();
		detailFileViewIcon.dispose();
		importFileIcon.dispose();
		exportFileIcon.dispose();
		saveImageIcon.dispose();
		deleteFileIcon.dispose();
		deletedFilesIcon.dispose();
		logoImage.dispose();
		exportWizardLogo.dispose();
		openDiskIcon.dispose();
		newDiskIcon.dispose();
		aboutIcon.dispose();
		diskImageWizardLogo.dispose();
		importWizardLogo.dispose();
		compileIcon.dispose();
	}
	/**
	 * Creates an image.
	 */
	private Image createImage(Display display, String path) {
		try {
			InputStream stream = getClass().getResourceAsStream(
				"/com/webcodepro/applecommander/ui/images/" + path);
			if (stream != null) {
				Image image = new Image(display, stream);
				stream.close();
				return image;
			}
		} catch (Exception e) {
		}
		return null;
	}
	/**
	 * Returns the deletedFilesIcon.
	 * @return Image
	 */
	public Image getDeletedFilesIcon() {
		return deletedFilesIcon;
	}

	/**
	 * Returns the deleteFileIcon.
	 * @return Image
	 */
	public Image getDeleteFileIcon() {
		return deleteFileIcon;
	}

	/**
	 * Returns the detailFileViewIcon.
	 * @return Image
	 */
	public Image getDetailFileViewIcon() {
		return detailFileViewIcon;
	}

	/**
	 * Returns the diskIcon.
	 * @return Image
	 */
	public Image getDiskIcon() {
		return diskIcon;
	}

	/**
	 * Returns the exportFileIcon.
	 * @return Image
	 */
	public Image getExportFileIcon() {
		return exportFileIcon;
	}

	/**
	 * Returns the exportWizardLogo.
	 * @return Image
	 */
	public Image getExportWizardLogo() {
		return exportWizardLogo;
	}

	/**
	 * Returns the importFileIcon.
	 * @return Image
	 */
	public Image getImportFileIcon() {
		return importFileIcon;
	}

	/**
	 * Returns the logoImage.
	 * @return Image
	 */
	public Image getLogoImage() {
		return logoImage;
	}

	/**
	 * Returns the nativeFileViewIcon.
	 * @return Image
	 */
	public Image getNativeFileViewIcon() {
		return nativeFileViewIcon;
	}

	/**
	 * Returns the saveImageIcon.
	 * @return Image
	 */
	public Image getSaveImageIcon() {
		return saveImageIcon;
	}

	/**
	 * Returns the standardFileViewIcon.
	 * @return Image
	 */
	public Image getStandardFileViewIcon() {
		return standardFileViewIcon;
	}

	/**
	 * Returns the aboutIcon.
	 * @return Image
	 */
	public Image getAboutIcon() {
		return aboutIcon;
	}

	/**
	 * Returns the newDiskIcon.
	 * @return Image
	 */
	public Image getNewDiskIcon() {
		return newDiskIcon;
	}

	/**
	 * Returns the openDiskIcon.
	 * @return Image
	 */
	public Image getOpenDiskIcon() {
		return openDiskIcon;
	}

	/**
	 * Returns the diskImageWizardLogo.
	 * @return Image
	 */
	public Image getDiskImageWizardLogo() {
		return diskImageWizardLogo;
	}
	
	/**
	 * Returns the importWizardLogo.
	 * @return Image
	 */
	public Image getImportWizardLogo() {
		return importWizardLogo;
	}
	
	/**
	 * Returns the compileIcon.
	 * @return Image
	 */
	public Image getCompileIcon() {
		return compileIcon;
	}
}
