package org.applecommander.util;

import java.util.Optional;

/**
 * Container marks an object that contains other objects for retrieval.
 * Experimental - the idea is to no longer expose everything that *might*
 * apply, but instead test if it's there and react accordingly.
 * <p/>
 * The end goal is to be able to retrieve the various devices - than have to
 * be handled distinctly anyway - and work from that. A DOS disk won't be backed
 * by a BlockDevice (most of the time), so it's counterintuitive that the DOS
 * file system has block level access. Vice versa for ProDOS/Pascal/RDOS/CPM.
 * Not to mention that it doesn't apply to Shrinkit or Zip archives at all.
 */
public interface Container {
    <T> Optional<T> get(Class<T> iface);
}
