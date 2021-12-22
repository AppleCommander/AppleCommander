/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2004 by Robert Greene
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
package com.webcodepro.applecommander.ui.swt.filteradapter;

import java.io.ByteArrayInputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;

import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.FileViewerWindow;
import com.webcodepro.applecommander.ui.swt.util.ImageCanvas;
import com.webcodepro.applecommander.ui.swt.util.contentadapter.ImageCanvasAdapter;
import com.webcodepro.applecommander.ui.swt.util.contentadapter.NoActionContentTypeAdapter;

/**
 * Provides a view of an Apple graphic image.
 * 
 * @author Rob Greene
 */
public class GraphicsFilterAdapter extends FilterAdapter {
	private Image image;
	private boolean error = false;
	
	public GraphicsFilterAdapter(FileViewerWindow window, String text, String toolTipText, Image image) {
		super(window, text, toolTipText, image);
	}

	public void display() {
		getCopyToolItem().setEnabled(false);

		if (image == null && error == false) {
			try {
				byte[] imageBytes = getFileFilter().filter(getFileEntry());
				ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
				ImageLoader imageLoader = new ImageLoader();
				ImageData[] imageData = imageLoader.load(inputStream);
				image = new Image(getComposite().getDisplay(), imageData[0]);
			} catch (Throwable t) {
				error = true;
			}
		}

		if (!error) {
			GridLayout layout = new GridLayout();
			getComposite().setLayout(layout);
			GridData gridData = new GridData();
			gridData.widthHint = image.getImageData().width;
			gridData.heightHint = image.getImageData().height;
			ImageCanvas imageCanvas = new ImageCanvas(getComposite(), SWT.NONE, image, gridData);
			getComposite().setContent(imageCanvas);
			getComposite().setExpandHorizontal(true);
			getComposite().setExpandVertical(true);
			getComposite().setMinWidth(image.getImageData().width);
			getComposite().setMinHeight(image.getImageData().height);
			setContentTypeAdapter(new ImageCanvasAdapter(imageCanvas, getFileEntry().getFilename()));
		} else {
			Label label = new Label(getComposite(), SWT.NULL);
			label.setText(UiBundle.getInstance().get(
					"GraphicsFilterAdapter.BadImageMessage")); //$NON-NLS-1$
			getComposite().setContent(label);
			getComposite().setExpandHorizontal(true);
			getComposite().setExpandVertical(true);
			setContentTypeAdapter(new NoActionContentTypeAdapter());
		}
	}
	
	public void dispose() {
		if (image != null) image.dispose();
	}


}
