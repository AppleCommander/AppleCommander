package org.applecommander.device;

/// Represents the primary sector orders in the Apple world.
/// <pre>
/// From Beneath Apple DOS (2020).
/// -------- SECTOR ORDERING --------
/// Physical*  DOS 3.3  Pascal*  CP/M
///     0         0        0       0
///     1         D        2       3
///     2         B        4       6
///     3         9        6       9
///     4         7        8       C
///     5         5        A       F
///     6         3        C       2
///     7         1        E       5
///     8         E        1       8
///     9         C        3       B
///     A         A        5       E
///     B         8        7       1
///     C         6        9       4
///     D         4        B       7
///     E         2        D       A
///     F         F        F       D
/// </pre>
public enum SectorOrder {
    /// "nibble" or "physical" sector order. This is the physical sector as written on disk.
    /// Used by the 13-sector formats of DOS (3.0 through 3.2), and RDOS 2.1.
    NIBBLE(0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xa, 0xb, 0xc, 0xd, 0xe, 0xf),
    /// DOS 3.3 order.
    DOS(0x0, 0xd, 0xb, 0x9, 0x7, 0x5, 0x3, 0x1, 0xe, 0xc, 0xa, 0x8, 0x6, 0x4, 0x2, 0xf),
    /// ProDOS or Pascal sector order. Also used by RDOS 3.3.
    PRODOS(0x0, 0x2, 0x4, 0x6, 0x8, 0xa, 0xc, 0xe, 0x1, 0x3, 0x5, 0x7, 0x9, 0xb, 0xd, 0xf),
    /// CP/M sector order.
    CPM(0x0, 0x3, 0x6, 0x9, 0xc, 0xf, 0x2, 0x5, 0x8, 0xb, 0xe, 0x1, 0x4, 0x7, 0xa, 0xd);

    public final int[] physicalSectorOrder;

    SectorOrder(int... physicalSectorOrder) {
        assert physicalSectorOrder.length == 16;
        this.physicalSectorOrder = physicalSectorOrder;
    }
}
