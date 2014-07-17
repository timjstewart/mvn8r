package com.timjstewart.rules;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.fusesource.jansi.Ansi;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * determines which color to display text in and what part of the text to actually display.
 */
public class SingleLineRegexRule extends BaseBlockRule {

    private final Ansi.Color color;
    private final Pattern pattern;

    public SingleLineRegexRule(
            final Ansi.Color color,
            final String regex
    )  throws PatternSyntaxException {
        this.color = Objects.requireNonNull(color, "color cannot be null");
        this.pattern = Pattern.compile(regex);
    }

    @Override public boolean isNull() {
        return false;
    }

    @Override public boolean shouldIgnore(final String line) {
        return false;
    }

    @Override
    public boolean isStartOfBlock(final String line) {
        return pattern.matcher(line).find();
    }

    @Override
    public boolean isEndOfBlock(final String line) {
        return true;
    }

    public String format(final String line) {
        final Matcher matcher = pattern.matcher(line);
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
        return "SingleLineRegexRule";
    }
}
