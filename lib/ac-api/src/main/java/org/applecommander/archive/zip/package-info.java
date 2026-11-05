/// ZIP archive format support.
///
/// ZIP is a widely-used archive and compression format identified by the signature
/// `0x504B0304` (`"PK\\x03\\x04"`) at the beginning of the file. It supports
/// multiple compression methods, per-entry CRC-32 integrity checksums, and file metadata
/// including creation, last access, and last modification timestamps.
///
/// This package provides a [org.applecommander.filestore.FileStore] implementation
/// that reads and writes standard ZIP archives using Java's built-in
/// [java.util.zip.ZipInputStream] and [java.util.zip.ZipOutputStream]. It exposes
/// archived files through the standard AppleCommander file store API with support for browsing,
/// extracting, creating, and deleting entries. Each entry preserves its compression method,
/// compressed and uncompressed sizes, compression ratio, CRC-32 checksum, and file timestamps.
///
/// Unlike Apple II-specific archive formats, ZIP does not natively store ProDOS metadata
/// such as file type, auxiliary type, or access mode. Files are stored with their data fork
/// content and standard ZIP metadata (name, timestamps, compression attributes).
///
/// @see <a href="https://github.com/applecommander/AppleCommander">AppleCommander</a>
package org.applecommander.archive.zip;
