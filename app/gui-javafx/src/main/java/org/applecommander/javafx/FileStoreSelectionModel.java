package org.applecommander.javafx;

import javafx.scene.control.SingleSelectionModel;
import org.applecommander.filestore.FileStore;

import java.util.ArrayList;
import java.util.List;

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
