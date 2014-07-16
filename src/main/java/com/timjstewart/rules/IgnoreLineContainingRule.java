package com.timjstewart.rules;

import java.util.Objects;
import java.util.regex.PatternSyntaxException;

public class IgnoreLineContainingRule implements BlockRule {

    private final String text;

    public IgnoreLineContainingRule(final String text) throws PatternSyntaxException {
        this.text = Objects.requireNonNull(text, "text cannot be null");
    }

    @Override
    public boolean isStartOfBlock(final String line) {
        return line.contains(text);
    }

    @Override
    public boolean isEndOfBlock(final String line) {
        return true;
    }

    @Override
    public String format(final String line) {
        return line;
    }

    @Override
    public boolean shouldIgnore(final String line) {
        return true;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public String getName() {
        return "IgnoreLineContainingRule";
    }
}
