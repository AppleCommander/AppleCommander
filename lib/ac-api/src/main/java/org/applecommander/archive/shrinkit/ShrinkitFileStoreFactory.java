package org.applecommander.archive.shrinkit;

import com.webcodepro.shrinkit.io.ByteConstants;
import org.applecommander.filestore.FileStoreFactory;
import org.applecommander.util.DataBuffer;

import java.util.Arrays;

public class ShrinkitFileStoreFactory implements FileStoreFactory {
    @Override
    public void inspect(Context ctx) {
        DataBuffer buffer = ctx.source.readBytes(0, 6);
        if (Arrays.equals(ByteConstants.NUFILE_ID, buffer.asBytes())) {
            ctx.fileStores.add(new ShrinkitFileStore(ctx.source));
        }
    }
}
