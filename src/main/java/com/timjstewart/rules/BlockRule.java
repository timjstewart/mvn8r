package com.timjstewart.rules;
 
public interface BlockRule {
    boolean isStartOfBlock(final String line);
    boolean isEndOfBlock(final String line);
    String  format(final String line);
    boolean shouldIgnore(final String line);
    boolean isNull();
    String getName();
    void addListener(final BlockRuleListener listener);
    void onRuleMatched(final String line);
}
