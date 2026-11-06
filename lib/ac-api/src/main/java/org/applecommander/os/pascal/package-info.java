/// Apple Pascal is a disk operating system and programming environment released by Apple Computer
/// in 1980 for the Apple II series. It shipped with the Apple Pascal System (part numbers
/// 680-0282 through 680-0290) and provided a full Pascal compiler, linker, and runtime
/// alongside its own file system.
///
/// The Pascal file system uses a flat directory with up to 78 entries (including the volume
/// header), each 26 bytes long, stored contiguously across blocks 2 through 5. Files are
/// allocated contiguously on disk with no block map — free space is found by scanning the
/// directory for gaps between entries. There is no concept of deleted entries; deletion
/// "squeezes out" the removed entry by shifting subsequent entries forward.
///
/// The volume header (first directory entry) stores the volume name (up to 7 characters),
/// block count, file count, and a date stamp. File entries store a name (up to 15 characters),
/// type, first/last block numbers, bytes used in the last block, and a modification date.
///
/// File types supported:
///
/// | Type     | Description |
/// | :---     | :--- |
/// | CODE     | Compiled p-code (executable) |
/// | TEXT     | Source or text data |
/// | DATA     | Arbitrary binary data |
/// | INFO     | Information file |
/// | GRAF     | Graphics data |
/// | FOTO     | Photo data |
///
/// Dates are encoded in a single word: bits 0-3 for month (1-12), bits 4-8 for day (1-31),
/// and bits 9-15 for year (0-99).
///
/// Pascal CODE files contain p-code segments that can be disassembled. Each segment has a
/// name, kind (LINKED, HOSTSEG, SEGPROC, etc.), machine type (p-code, 6502, etc.), and a
/// dictionary of procedures. The p-code instruction set is interpreted by the Pascal Virtual
/// Machine (PVM) and supports stack-based operations, procedure calls, and I/O.
///
/// References:
///
/// * [Apple II Pascal 1.3 Workbench Manual](https://archive.org/details/apple_ii_pascal_1.3_workbench)
/// * [Apple Pascal System Technical Reference](https://archive.org/details/apple-pascal-system-technical-reference)
/// * [Apple Pascal file system layout](https://ciderpress2.com/formatdoc/Pascal-notes.html)
/// * [Apple Pascal discussions](https://github.com/AppleCommander/AppleCommander/blob/main/documentation/apple.pascal.txt)
package org.applecommander.os.pascal;
