package org.applecommander.device;

import org.applecommander.capability.Capability;
import org.applecommander.os.DiskCheck;
import org.applecommander.util.DataBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.applecommander.device.TrackSectorDevice.SECTOR_SIZE;

/**
 * Provides a "low-level" scan of a nibble device. If any sectors cannot be found, adds a Finding
 * which allows the user to over-write the track (first, reading sectors from the track).
 */
public class TrackSectorNibbleDiskCheck implements DiskCheck {
    private final TrackSectorNibbleDevice device;

    public TrackSectorNibbleDiskCheck(TrackSectorNibbleDevice device) {
        this.device = device;
    }

    @Override
    public List<Finding> scan() {
        List<Finding> findings = new ArrayList<>();
        if (device.can(Capability.WRITE_SECTOR) && device.can(Capability.FORMAT_TRACK)) {
            TrackSectorDevice.Geometry geometry = device.getGeometry();
            for (int track = 0; track < geometry.tracksOnDisk(); track++) {
                int badSectors = 0;
                int trackSize = geometry.sectorsPerTrack() * SECTOR_SIZE;
                DataBuffer trackData = DataBuffer.create(trackSize);
                for (int sector = 0; sector < geometry.sectorsPerTrack(); sector++) {
                    try {
                        DataBuffer sectorData = device.readSector(track, sector);
                        trackData.put(sector * SECTOR_SIZE, sectorData);
                    } catch (Throwable t) {
                        badSectors++;
                    }
                }
                if (badSectors > 0) {
                    final int problemTrack = track;
                    Finding finding = new Finding(
                            String.format("Unable to read %d sectors on track %d.", badSectors, problemTrack),
                            Optional.of(() -> rewriteTrack(problemTrack, trackData)),
                            "nibble", new Coordinate(problemTrack, 0));
                    findings.add(finding);
                }
            }
        }
        return findings;
    }

    private void rewriteTrack(int track, DataBuffer trackData) {
        device.formatTrack(track);
        for (int sector = 0; sector < device.getGeometry().sectorsPerTrack(); sector++) {
            device.writeSector(track, sector, trackData.slice(sector*SECTOR_SIZE, SECTOR_SIZE));
        }
    }
}
