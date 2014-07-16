package com.timjstewart.rules;

public class NullRule implements BlockRule {

    private static final NullRule instance = new NullRule();

    private NullRule() {
    }

    public static NullRule getInstance() {
        return instance;
    }

    @Override
    public boolean isStartOfBlock(final String line) {
        return false;
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

    @Override public boolean isNull() {
        return true;
    }

    @Override
    public String getName() {
        return "NullRule";
    }
}
