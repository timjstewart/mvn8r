package com.timjstewart;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class IgnoreLineContainingRule implements BlockRule {

    private final String text;

    public IgnoreLineContainingRule(final String text) throws PatternSyntaxException {
        this.text = Objects.requireNonNull(text, "text cannot be null");
    }

    public boolean isStartOfBlock(final String line) {
        return line.contains(text);
    }

    public boolean isEndOfBlock(final String line) {
        return true;
    }

    public String format(final String line) {
        return line;
    }

    public boolean shouldIgnore(final String line) {
        return true;
    }

    @Override public boolean isNull() {
        return false;
    }
}
