/// RDOS by Roland Gustafsson
///
/// Disks are identified by looking for the catalog header. It is consistent across versions,
/// except that 2.1 is all high-ASCII while 3.3 is low-ASCII. If the header is on track 0,
/// it's 3.3 while track 1 is 2.1. We know what sector the catalog exists on and from that
/// the sector skew can be interpolated.
///
/// | Version | Directory | `&CAT` | Sectors/Track |
/// | :--- | :---: | :---: | :---: |
/// | RDOS 2.1 | 13..23 | 25 | 13 |
/// | RDOS 3.3 | 16..31 | 1 | 16 |
///
/// References:
///
/// * [Rdos 3.3](http://underground2e.free.fr/Underground/Outils/Rdos/Rdos.html)
/// * [RDOS Stuff](https://gswv.apple2.org.za/a2zine/Sel/RDOSstuff.html)
/// * [RDOS Notes](https://ciderpress2.com/formatdoc/RDOS-notes.html)
package org.applecommander.os.rdos;