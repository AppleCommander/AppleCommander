/// Binary II archive format support.
///
/// Binary II is a binary archive format for Apple II files, identified by ProDOS file type
/// `$E0` (224) and auxiliary type `$8000`. It was designed as a successor to the
/// earlier Binary format, preserving ProDOS file metadata (type, subtype, access code, dates)
/// while also supporting files from other operating systems such as DOS 3.3, Apple Pascal,
/// Macintosh MFS/HFS, and MS-DOS.
///
/// Each file in a Binary II archive is stored as a 128-byte header block followed by the
/// file's data padded to 128-byte boundaries. The header begins with identification bytes
/// `0x0A 0x47 0x4C` at offset 0 and `0x02` at offset 18. The header encodes
/// both ProDOS 8 and GS/OS metadata, including file names (Pascal and native), sizes,
/// creation and modification timestamps, and data flags indicating compression, encryption,
/// or sparse content.
///
/// Common file extensions are `.bny`, `.bqy`, and `.bxy`.
///
/// This package provides a [org.applecommander.filestore.FileStore] implementation
/// that parses Binary II archives and exposes their contents through the standard AppleCommander
/// file store API, enabling browsing and extraction of archived files with full metadata.
///
/// @see <a href="https://github.com/AppleCommander/AppleCommander">AppleCommander</a>
package org.applecommander.archive.binaryii;
