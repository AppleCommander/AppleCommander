/// ShrinkIt archive format support.
///
/// ShrinkIt is a compression archive format for Apple II files, commonly used for
/// distributing software and disk images. Archives are typically stored with the
/// `.shk` or `.sdk` file extensions and are identified by a magic byte
/// sequence (NuFile ID) at the beginning of the file.
///
/// Each ShrinkIt archive contains a master header block with archive-level metadata
/// (creation/modification timestamps, version, total record count, and a CRC-16
/// integrity checksum), followed by per-file header blocks. Each archived file stores
/// full ProDOS metadata including file name, type, auxiliary type, access mode, storage
/// type, and creation and modification timestamps.
///
/// File content is stored as compressed threads, with separate support for both the
/// data fork and the resource fork. Each thread record specifies a compression format
/// (thread format), a kind indicator (e.g. disk image), a compressed size, and a
/// CRC-16 checksum for integrity verification. The archive preserves the original
/// uncompressed file size alongside the compressed data, enabling display of
/// compression ratios.
///
/// This package provides a [org.applecommander.filestore.FileStore] implementation
/// that parses ShrinkIt archives and exposes their contents through the standard AppleCommander
/// file store API, enabling browsing, metadata inspection, and extraction of archived files
/// with full ProDOS attributes and resource fork support.
///
/// @see <a href="https://github.com/AppleCommander/AppleCommander">AppleCommander</a>
package org.applecommander.archive.shrinkit;
