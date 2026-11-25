/// Conversion support for data formats found in Apple II files.
///
/// The package provides APIs for translating native file data into forms that applications
/// can inspect or present.
///
/// * [TextConverter] handles text data as strings and byte buffers;
/// * [BASICConverter] exposes the structure of BASIC programs through events for lines,
///   tokens, literals, identifiers, and other program elements;
/// * [WordProcessorConverter] defines an event-based representation of document text and
///   formatting controls;
/// * The package also includes format-specific support for structured data such as saved BASIC
///   variables.
///
/// Access to the converters is done through the `ConverterRepository` API.
/// Available conversion coverage varies by format.
package org.applecommander.converter;