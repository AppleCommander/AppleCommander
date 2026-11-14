/// # US-DOS (Universal Software DOS)
///
/// US-DOS is a Bulgarian MS-DOS-like operating system made in the early
/// 1990s for Pravetz Apple II clones.
///
/// ## Disk layout
///
/// Disks appear to have DOS ordered sectors.
///
/// * Boot: Track 0, sectors 0 and 1. Tries to load `COMMAND.COM`.
/// * Directory: Begins at Track 0, sector 14 and descends (likely to sector 2).
///   Each directory entry is 20 bytes and wraps into the next sector.
/// * Bitmap: Track 0, sector 15 contains the usage bitmap.
///
/// ## Directory
///
/// Each directory entry is 20 bytes:
///
/// | Offset | Length (bytes) | Notes |
/// | ---: | ---: | :--- |
/// | 0 | 8 | Name, low ASCII, space-padded. If the first byte is `$00` the file is deleted. |
/// | 8 | 3 | Extension, low ASCII; if offset +8 is `$DC` (`\\`), this is a directory and offset +9 is directory number. |
/// | 11 | 2 | Length, little-endian |
/// | 13 | 2 | Date. Format is `dddddmmm` `myyyyyyy`; year = +1977; day and month are zero based. |
/// | 15 | 1 | Directory number; 0 is root directory. |
/// | 16 | 2 | Address or load address. |
/// | 18 | 2 | Track/sector map location. |
///
/// ## Usage bitmap
///
/// * Begins at offset `$0E` through `$5D` in the sector.
/// * Blocks out 40 tracks, even when the disk is a typical 35 track device.
/// * Each track is two bytes, high byte is sectors 8..15 and low byte is sectors 0..7.
///   Values `FF 03` map to sectors 0-5 free and 6-15 used.
/// * Location `$00` contains the disk label. Maximum length is 11 characters and is
///   a zero terminated string.
///
/// ## File track/sector map
///
/// The file's track/sector map separates the tracks and sectors. The full range is a
/// bit of a guess. Full usage allows 170 tracks but 172 sectors.
///
/// * From `$00` to `$A9` is a list of tracks. When track `$FF` encountered, at end.
/// * From `$AA` to `$FF` are the sectors, encoded in a 4-bit nybble. Low-nybble first.
package org.applecommander.os.usdos;