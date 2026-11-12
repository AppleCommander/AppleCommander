/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2026 by Robert Greene and others
 * robgreene at users.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.applecommander.filestore;

import org.applecommander.ui.CharacterMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/// Represents a single configuration option for file store creation. The intent is that this
/// class represents how the field is configured as well as the value.
public abstract class ConfigurationOption {
    private final String prompt;
    private final String notes;

    ConfigurationOption(String prompt, String notes) {
        this.prompt = prompt;
        this.notes = notes;
    }

    public String getPrompt() {
        return prompt;
    }
    public String getNotes() {
        return notes;
    }

    public static class StringConfigurationOption extends ConfigurationOption {
        private final CharacterMapper characterRestrictions;
        private final Predicate<String> validatorFn;
        private final int minLength;
        private final int maxLength;
        private String value;

        StringConfigurationOption(CharacterMapper characterRestrictions, Predicate<String> validatorFn,
                                  int minLength, int maxLength, String prompt, String notes, String defaultValue) {
            super(prompt, notes);
            this.characterRestrictions = characterRestrictions;
            this.validatorFn = validatorFn;
            this.minLength = minLength;
            this.maxLength = maxLength;
            this.value = defaultValue;
        }

        public CharacterMapper getCharacterRestrictions() {
            return characterRestrictions;
        }

        public Predicate<String> getValidatorFn() {
            return validatorFn;
        }

        public int getMinLength() {
            return minLength;
        }

        public int getMaxLength() {
            return maxLength;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            value = characterRestrictions.map(value);
            if (!validatorFn.test(value)) {
                throw new RuntimeException("field failed validation");
            }
            this.value = value;
        }
    }

    public static class FlagConfigurationOption extends ConfigurationOption {
        private boolean value;

        FlagConfigurationOption(String prompt, String notes, boolean defaultValue) {
            super(prompt, notes);
            this.value = defaultValue;
        }

        public boolean getValue() {
            return value;
        }
        public void setValue(boolean flag) {
            this.value = flag;
        }
    }

    /// Extract the given option value with optional default.
    public static String getValidatedValue(Map<String,ConfigurationOption> options, String key, String defaultValue) {
        if (options.get(key) instanceof StringConfigurationOption config) {
            return config.getValue();
        }
        return defaultValue;
    }

    /// Extract the given flag value.
    public static boolean getFlagValue(Map<String,ConfigurationOption> options, String key) {
        if (options.get(key) instanceof FlagConfigurationOption config) {
            return config.getValue();
        }
        return false;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String,ConfigurationOption> options = new HashMap<>();

        private Builder() {
        }

        public StringOptionBuilder stringValue(String id) {
            return new StringOptionBuilder(this, id);
        }
        public FlagOptionBuilder flagValue(String id) {
            return new FlagOptionBuilder(this, id);
        }
        public Map<String,ConfigurationOption> done() {
            return Collections.unmodifiableMap(options);
        }
    }
    public static class StringOptionBuilder {
        private final Builder parent;
        private final String id;
        private CharacterMapper characterRestrictions = CharacterMapper.ANYTHING;
        private Predicate<String> validatorFn;
        private int minLength = 1;
        private int maxLength = -1;
        private String prompt;
        private String notes;
        private String defaultValue;

        private StringOptionBuilder(Builder parent, String id) {
            Objects.requireNonNull(id);
            this.parent = parent;
            this.id = id;
        }

        public StringOptionBuilder enforceLength(int minLength, int maxLength) {
            assert minLength > 0 && (maxLength == -1 || maxLength >= minLength);
            this.minLength = minLength;
            this.maxLength = maxLength;
            return this;
        }
        public StringOptionBuilder restrictions(CharacterMapper characterRestrictions) {
            Objects.requireNonNull(characterRestrictions);
            this.characterRestrictions = characterRestrictions;
            return this;
        }
        public StringOptionBuilder prompt(String prompt) {
            Objects.requireNonNull(prompt);
            this.prompt = prompt;
            return this;
        }
        public StringOptionBuilder notes(String notes) {
            Objects.requireNonNull(notes);
            this.notes = notes;
            return this;
        }
        public StringOptionBuilder defaultValue(String defaultValue) {
            Objects.requireNonNull(defaultValue);
            this.defaultValue = defaultValue;
            return this;
        }
        /// Terminate builder and return the current string configuration option.
        public StringConfigurationOption single() {
            if (validatorFn == null) {
                validatorFn = Objects::nonNull;
                if (minLength > 0) validatorFn = validatorFn.and(s -> s.length() >= minLength);
                if (maxLength != -1) validatorFn = validatorFn.and(s -> s.length() <= maxLength);
            }
            return new StringConfigurationOption(characterRestrictions, validatorFn, minLength,
                    maxLength, prompt, notes, defaultValue);

        }
        /// Add this string configuration option to the list of options and continue with the builder.
        public Builder add() {
            parent.options.put(id, single());
            return parent;
        }
    }
    public static class FlagOptionBuilder {
        private final Builder parent;
        private final String id;

        private String prompt;
        private String notes;
        private boolean defaultValue;

        private FlagOptionBuilder(Builder parent, String id) {
            Objects.requireNonNull(id);
            this.parent = parent;
            this.id = id;
        }
        public FlagOptionBuilder prompt(String prompt) {
            Objects.requireNonNull(prompt);
            this.prompt = prompt;
            return this;
        }
        public FlagOptionBuilder notes(String notes) {
            Objects.requireNonNull(notes);
            this.notes = notes;
            return this;
        }
        public FlagOptionBuilder defaultValue(boolean defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }
        public Builder add() {
            parent.options.put(id, new FlagConfigurationOption(prompt, notes, defaultValue));
            return parent;
        }
    }
}
