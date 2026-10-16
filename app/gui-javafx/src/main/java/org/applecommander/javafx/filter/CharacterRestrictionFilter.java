package org.applecommander.javafx.filter;

import javafx.scene.control.TextFormatter;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class CharacterRestrictionFilter implements UnaryOperator<TextFormatter.Change> {
    private final Pattern pattern;

    public CharacterRestrictionFilter(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public TextFormatter.Change apply(TextFormatter.Change change) {
        if (pattern.matcher(change.getControlNewText()).matches()) {
            return change;
        }
        return null;
    }
}
