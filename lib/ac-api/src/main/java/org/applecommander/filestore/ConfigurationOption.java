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
public class ConfigurationOption {
    private final CharacterMapper characterRestrictions;
    private final Predicate<String> validatorFn;
    private final int minLength;
    private final int maxLength;
    private final String prompt;
    private final String notes;
    private String value;

    ConfigurationOption(CharacterMapper characterRestrictions, Predicate<String> validatorFn,
                               int minLength, int maxLength, String prompt, String notes, String defaultValue) {
        this.characterRestrictions = characterRestrictions;
        this.validatorFn = validatorFn;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.prompt = prompt;
        this.notes = notes;
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
    public String getPrompt() {
        return prompt;
    }
    public String getNotes() {
        return notes;
    }

    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        value = characterRestrictions.map(value);
        if (!validatorFn.test(value)) {
            throw new RuntimeException("field  failed validation");
        }
        this.value = value;
    }

    /// Extract the given option value with optional default.
    public static String getValidatedValue(Map<String,ConfigurationOption> options, String key, String defaultValue) {
        if (options.containsKey(key)) {
            return options.get(key).getValue();
        }
        return defaultValue;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<String,ConfigurationOption> options = new HashMap<>();

        private Builder() {
        }

        public OptionBuilder next(String id) {
            return new OptionBuilder(this, id);
        }
        public Map<String,ConfigurationOption> done() {
            return Collections.unmodifiableMap(options);
        }
    }
    public static class OptionBuilder {
        private final Builder parent;
        private final String id;
        private CharacterMapper characterRestrictions = CharacterMapper.ANYTHING;
        private Predicate<String> validatorFn;
        private int minLength = 1;
        private int maxLength = -1;
        private String prompt;
        private String notes;
        private String defaultValue;

        private OptionBuilder(Builder parent, String id) {
            Objects.requireNonNull(id);
            this.parent = parent;
            this.id = id;
        }

        public OptionBuilder enforceLength(int minLength, int maxLength) {
            assert minLength > 0 && (maxLength == -1 || maxLength >= minLength);
            this.minLength = minLength;
            this.maxLength = maxLength;
            return this;
        }
        public OptionBuilder restrictions(CharacterMapper characterRestrictions) {
            Objects.requireNonNull(characterRestrictions);
            this.characterRestrictions = characterRestrictions;
            return this;
        }
        public OptionBuilder prompt(String prompt) {
            Objects.requireNonNull(prompt);
            this.prompt = prompt;
            return this;
        }
        public OptionBuilder notes(String notes) {
            Objects.requireNonNull(notes);
            this.notes = notes;
            return this;
        }
        public OptionBuilder defaultValue(String defaultValue) {
            Objects.requireNonNull(defaultValue);
            this.defaultValue = defaultValue;
            return this;
        }
        public Builder add() {
            if (validatorFn == null) {
                validatorFn = Objects::nonNull;
                if (minLength > 0) validatorFn = validatorFn.and(s -> s.length() >= minLength);
                if (maxLength != -1) validatorFn = validatorFn.and(s -> s.length() <= maxLength);
            }
            parent.options.put(id, new ConfigurationOption(characterRestrictions, validatorFn, minLength, maxLength,
                    prompt, notes, defaultValue));
            return parent;
        }
    }
}
