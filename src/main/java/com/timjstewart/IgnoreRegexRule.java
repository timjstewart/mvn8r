package com.timjstewart;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class IgnoreRegexRule implements BlockRule {

    private final Pattern pattern;

    public IgnoreRegexRule(final String regex) throws PatternSyntaxException {
        pattern = Pattern.compile(regex);
    }

    public boolean isStartOfBlock(final String line) {
        return pattern.matcher(line).find();
    }

    public boolean isEndOfBlock(final String line) {
        return true;
    }

    public String  format(final String line) {
        return line;
    }

    public boolean shouldIgnore(final String line) {
        return true;
    }

    @Override public boolean isNull() {
        return false;
    }
}
