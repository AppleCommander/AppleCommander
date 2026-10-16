package org.applecommander.javafx.filter;

import javafx.scene.control.TextFormatter;

import java.util.List;
import java.util.function.UnaryOperator;

public class CompositeFilter implements UnaryOperator<TextFormatter.Change> {
    private final List<UnaryOperator<TextFormatter.Change>> filters;

    public CompositeFilter(List<UnaryOperator<TextFormatter.Change>> filters) {
        this.filters = filters;
    }

    @Override
    public TextFormatter.Change apply(TextFormatter.Change change) {
        for (UnaryOperator<TextFormatter.Change> filter : filters) {
            change = filter.apply(change);
            if (change == null) {
                break;
            }
        }
        return change;
    }
}
