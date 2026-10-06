package org.applecommander.os;

import org.applecommander.util.Range;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/// The SequentialAllocator is intended to handle what is, essentially, a
/// single dimension allocation. That is, if the disk is allocated by sequential
/// chunks, this allocator should support it. For images that use T/S geometry
/// instead of block geometry, the T/S will need to be transformed.
///
/// For instance, GameDOS allocates by T/S but allocates all in a chunk. So once
/// there is a starting place, the rest of the file follows in a very specific
/// order.
public class SequentialAllocator {
    private final int totalSize;
    private final List<Range> used = new ArrayList<>();

    public SequentialAllocator(int totalSize) {
        this.totalSize = totalSize;
    }

    public void addUsed(int first, int last) {
        assert first <= last;
        assert first < totalSize && last <= totalSize;
        used.add(new Range(first, last));
    }
    public void addUsedByLength(int first, int length) {
        addUsed(first, first+length-1);
    }

    /// Look for `requestSize` spaces between the used ranges.
    /// Returns either the first placement that the request will fit
    /// or a `-1` if there is no fit.
    public int find(int requestSize) {
        used.sort(Comparator.comparingInt(Range::first));
        int start = 0;
        for (Range range : used) {
            if (range.first() - start >= requestSize) {
                return start;
            }
            start = range.last() + 1;
        }
        if (totalSize - start >= requestSize) {
            return start;
        }
        return -1;
    }
}
