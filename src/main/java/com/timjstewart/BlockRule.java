package com.timjstewart;

public interface BlockRule {
    public boolean isStartOfBlock(final String line);
    public boolean isEndOfBlock(final String line);
    public String  format(final String line);
    public boolean shouldIgnore(final String line);
    public boolean isNull();
}

