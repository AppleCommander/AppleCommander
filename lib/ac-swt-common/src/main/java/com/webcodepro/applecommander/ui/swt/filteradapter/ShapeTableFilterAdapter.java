package com.webcodepro.applecommander.ui.swt.filteradapter;

import org.eclipse.swt.graphics.Image;

import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.filters.ShapeTableFileFilter;
import com.webcodepro.applecommander.ui.swt.FileViewerWindow;

public class ShapeTableFilterAdapter extends GraphicsFilterAdapter {
    public ShapeTableFilterAdapter(FileViewerWindow window, String text, String toolTipText, Image image) {
        super(window, text, toolTipText, image);
    }

    @Override
    protected FileFilter getFileFilter() {
        return new ShapeTableFileFilter();
    }
}
