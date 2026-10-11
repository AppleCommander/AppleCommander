/// GameDOS, Written by usotsuki.
///
/// This is a single-load read-only program launcher that does not require DOS.
/// It only resides on track 0, freeing up the rest of the disk for ... games.
/// There dos not appear to be any presence on the internet, but the source and
/// some example disks do exist in the Asimov archives.
///
/// GameDOS requires a DOS-ordered 140K disk.
///
/// There is a readme that gives information on the disk format and (expected)
/// file types:
///
/// ```c
/// struct gd_dirent {
///   char type;                /* 'A' = Applesoft, 'B' = Binary */
///   unsigned char track1;
///   unsigned char sector1;
///   unsigned short int meta;  /* load addr for Binary,
///                                end of code addr for Applesoft */
///   char filename[26];        /* space-padded */
///   unsigned char len;        /* in sectors; file must be contiguous */
/// };
/// ```
///
/// There are 16 directory entries, stored from $BE00 up (track 00 sectors E and F).
///
/// The following types are defined but not yet implemented:
///
/// * T - Text
/// * S - System, loads at $0800 (NOT $2000 as ProDOS!)
/// * P - Picture, loads at $2000
/// * I - Integer Basic
///
/// The AppleCommander implementation assumes all types are currently valid.
package org.applecommander.os.gamedos;