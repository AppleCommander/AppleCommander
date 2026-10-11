/// Contains most of the device interfaces and implementation.
///
/// Some creational patterns for `TrackSectorDevice`:
///
/// * 140K DOS-ordered sectors:
///   ```
///   Source source = DataBufferSource.create(DiskConstants.APPLE_140KB_DISK, "filename.do").get();
///   TrackSectorDevice device = new DosOrderedTrackSectorDevice(source);
///   ```
///
/// * 140K ProDOS-ordered sectors:
///   ```
///   Source source = DataBufferSource.create(DiskConstants.APPLE_140KB_DISK, "filename.po").get();
///   TrackSectorDevice device = new BlockToTrackSectorAdapter(new ProdosOrderedBlockDevice(source,
///                       BlockDevice.STANDARD_BLOCK_SIZE), new ProdosBlockToTrackSectorAdapterStrategy());
///   ```
///
/// * 140K Nibble-ordered sectors:
///   ```
///   Source source = DataBufferSource.create(DiskConstants.APPLE_140KB_NIBBLE_DISK, "filename.nib").get();
///   TrackSectorDevice device = SkewedTrackSectorDevice.physicalToDosSkew(
///                       TrackSectorNibbleDevice.create(new NibbleImage(source), 16));
///   ```
///
package org.applecommander.device;