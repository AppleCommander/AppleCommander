package org.applecommander.javafx.filter;

import javafx.scene.control.TextFormatter;

import java.util.function.UnaryOperator;

// See: https://www.reddit.com/r/JavaFX/comments/qd5uds/how_do_you_limit_the_character_input_in_a/
public class FixedLengthFilter implements UnaryOperator<TextFormatter.Change> {
    private final int maxLength;

    public FixedLengthFilter(int maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public TextFormatter.Change apply(TextFormatter.Change change) {
        if (change.getControlNewText().length() > maxLength) {
            return null;
        }
        return change;
    }
}
