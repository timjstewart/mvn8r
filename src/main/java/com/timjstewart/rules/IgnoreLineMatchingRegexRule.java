package com.timjstewart.rules;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class IgnoreLineMatchingRegexRule extends BaseBlockRule {

    private final Pattern pattern;

    public IgnoreLineMatchingRegexRule(final String regex) throws PatternSyntaxException {
        this.pattern = Pattern.compile(Objects.requireNonNull(regex, "text cannot be null"));
    }

    @Override
    public boolean isStartOfBlock(final String line) {
        return pattern.matcher(line).find();
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
        return "IgnoreLineMatchingRegexRule";
    }
}
