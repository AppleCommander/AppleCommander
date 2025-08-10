package org.applecommander.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This is a simple record to provide information for various user interfaces.
 */
public record Information(String label, String value) {
    public static Builder builder(String label) {
        return new Builder(label);
    }

    public static class Builder {
        private static final SimpleDateFormat dateFormatter = new SimpleDateFormat();
        private final String label;

        private Builder(String label) {
            this.label = label;
        }
        public Information value(String value) {
            return new Information(label, value);
        }
        public Information value(int value) {
            return value("%d", value);
        }
        public Information value(String fmt, Object... args) {
            return new Information(label, String.format(fmt, args));
        }
        public Information value(Date date) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            return new Information(label, date != null ? dateFormat.format(date) : "-None-");
        }
    }
}
