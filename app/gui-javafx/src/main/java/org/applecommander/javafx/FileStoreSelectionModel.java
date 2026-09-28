/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2026 by Robert Greene and others
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
package org.applecommander.javafx;

import javafx.scene.control.SingleSelectionModel;
import org.applecommander.filestore.FileStore;

import java.util.ArrayList;
import java.util.List;

/// The FileStoreSelectionModel manages the FileStores that were discovered in the selected image.
/// It ensures that only one is selected, exposes various properties that can be utilized in the
/// reactive JavaFX application.
public class FileStoreSelectionModel extends SingleSelectionModel<FileStore> {
    private final List<FileStore> fileStores = new ArrayList<>();

    public void changeFileStores(List<FileStore> fileStores) {
        this.fileStores.clear();
        this.fileStores.addAll(fileStores);
        if (!this.fileStores.isEmpty()) {
            selectFirst();
        }
    }

    public void clearFileStores() {
        this.clearSelection();
        this.fileStores.clear();
    }

    @Override
    protected FileStore getModelItem(int index) {
        if (index >= 0 && index < fileStores.size()) {
            return fileStores.get(index);
        }
        return null;
    }

    @Override
    protected int getItemCount() {
        return fileStores.size();
    }
}
