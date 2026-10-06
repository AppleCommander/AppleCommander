package org.applecommander.os;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SequentialAllocatorTest {
    @Test
    public void test() {
        SequentialAllocator allocator = new SequentialAllocator(10);
        allocator.addUsed(2,4);
        allocator.addUsed(5,6);

        // Allocation:
        //    0123456789
        //    FFUUUUUFFF
        assertEquals(0, allocator.find(2));
        assertEquals(7, allocator.find(3));
        assertEquals(-1, allocator.find(4));
    }
}
