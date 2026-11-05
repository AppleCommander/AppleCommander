/// BinSCII archive format support.
///
/// BinSCII is a text-based binary encoding format for Apple II files, designed to allow
/// binary files to be transmitted over text-only channels such as email or Usenet. It is
/// similar in concept to `uuencode`, but uses a 64-character alphabet and encodes
/// data three bytes at a time into four 6-bit ASCII characters.
///
/// Files in BinSCII format typically use the `.bsc` extension. The archive is
/// divided into 12 KB segments, each beginning with a sentinel line
/// `FiLeStArTfIlEsTaRt`, followed by an alphabet line defining the 64-character
/// encoding alphabet, and a metadata line containing the file name and a 29-byte binary
/// header structure. This header encodes ProDOS metadata including file size, segment
/// offset, access mode, file type, auxiliary type, storage type, block size, creation
/// and modification timestamps, segment length, and a CRC-16 checksum.
///
/// Data lines are exactly 64 characters long, each representing 48 bytes of the
/// original file. Each segment ends with a CRC-16 checksum line for integrity
/// verification using the polynomial `X^16 + X^12 + X^5 + 1`.
///
/// This package provides a [org.applecommander.filestore.FileStore] implementation
/// that parses BinSCII archives and exposes their contents through the standard AppleCommander
/// file store API, supporting decoding of the custom alphabet encoding, metadata parsing,
/// CRC-16 integrity verification, and extraction of archived files with full ProDOS metadata.
///
/// @see <a href="https://github.com/AppleCommander/AppleCommander">AppleCommander</a>
package org.applecommander.archive.binscii;
