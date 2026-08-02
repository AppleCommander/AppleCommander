package org.applecommander.os;

import java.util.List;
import java.util.Optional;

/**
 * Basic interface for the disk check (check disk, filesystem check, etc) for a disk. Any finding comes back in the list
 * and the CLI or GUI decides what to act upon.
 */
public interface DiskCheck {
    /** Perform the disk scan. Note that a generic Exception can be thrown simply because we're crossing between old an new interfaces! */
    List<Finding> scan() throws Exception;

    /** Encapsulates a single "finding" or problem. It may or may not be fixable via action. */
    record Finding(String description, Optional<Runnable> action, String classification, Coordinate coordinate) {}

    /** A simplistic coordinate indicator. The thought is to allow narrowing down of what is to be fixed. */
    record Coordinate(int... coordinates) {
        public Coordinate {
            assert coordinates.length == 1 || coordinates.length == 2;
        }
        @Override
        public String toString() {
            return switch (coordinates.length) {
                case 1 -> String.format("B%d", coordinates[0]);
                case 2 -> String.format("T%d,S%d", coordinates[0], coordinates[1]);
                default -> List.of(coordinates).toString();
            };
        }
    }
}
