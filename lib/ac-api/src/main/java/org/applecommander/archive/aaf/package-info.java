/// Apple Archive Format (AAF) support.
///
/// AAF is a simple text-based archive format used for distributing Apple II files.
/// Each archived file is represented by a header line starting with `=` followed by the
/// file name, subsequent lines starting with `-` containing the file's text content,
/// and the entire archive is terminated by a line starting with `+`.
///
/// This package provides a [org.applecommander.filestore.FileStore] implementation
/// that parses AAF files and exposes their contents through the standard AppleCommander
/// file store API, enabling browsing and extraction of archived files.
///
/// Example AAF structure:
///
/// ```
/// =HELLO.TXT
/// -Hello, World!
/// +
/// ```
package org.applecommander.archive.aaf;
