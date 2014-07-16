package com.timjstewart.rules;

import org.fusesource.jansi.Ansi;

import static org.fusesource.jansi.Ansi.ansi;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * determines which color to display text in and what part of the text to actually display.
 */
public class MultiLineRule implements BlockRule {

    private final Ansi.Color color;
    private final Pattern startPattern;
    private final Pattern endPattern;

    public MultiLineRule(
            final Ansi.Color color,
            final String startRegex,
            final String endRegex
    ) throws PatternSyntaxException {
        this.color = Objects.requireNonNull(color, "color cannot be null");
        this.startPattern = Pattern.compile(startRegex);
        this.endPattern = Pattern.compile(endRegex);
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public boolean shouldIgnore(final String line) {
        return line.trim().isEmpty();
    }

    @Override
    public boolean isStartOfBlock(final String line) {
        return startPattern.matcher(line).find();
    }

    @Override
    public boolean isEndOfBlock(final String line) {
        return endPattern.matcher(line).find();
    }

    @Override
    public String format(final String line) {
        final Matcher matcher = startPattern.matcher(line);
        if (matcher.find()) {
            return ansi().fg(color).a(matcher.group(1)).reset().toString();
        } else {
            return line;
        }
    }

    /**
     * will allow color rules to be loaded from a config file
     */
    public static SingleLineRegexRule parse(final String text) throws RuleParseException {
        final String[] tokens = text.split(":");
        if (tokens.length == 2) {
            return new SingleLineRegexRule(Ansi.Color.valueOf(tokens[0]), tokens[1]);
        } else {
            throw new RuleParseException(String.format("%s could not be parsed into a ColorRule.", text));
        }
    }

    @Override
    public String getName() {
        return "MultiLineRule";
    }
}

