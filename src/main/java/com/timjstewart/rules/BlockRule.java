package com.timjstewart.rules;
 
public interface BlockRule {
    /**
     * @return the name of the BlockRule
     */
    String getName();

    /**
     * @return true iff this BlockRule interprets the supplied line as
     * starting a Block.
     */
    boolean isStartOfBlock(final String line);

    /**
     * @return true iff this BlockRule interprets the supplied line as
     * ending a Block.  For single line BlockRules the same line
     * matches both the beginning of the BlockRule and the end of the
     * BlockRule.
     */
    boolean isEndOfBlock(final String line);

    /**
     * Once a BlockRule is matched, the BlockRule can specify whether
     * or not the block should be ignored (i.e. not printed)
     */
    boolean shouldIgnore(final String line);

    /**
     * gives the Block rule a chance to format the current line
     * (e.g. remove parts, colorize parts, etc.).
     */
    String format(final String line);

    /**
     * @return true iff this BlockRule is a "null" BlockRule (e.g. one
     * that represents the absence of a rule).
     */
    boolean isNull();

    /**
     * some rules capture interesting events in the build's life
     * (e.g. a compile error or a unit test failure).  This method
     * allows interested clients to be alerted when a BlockRule is
     * triggered.
     */
    void addListener(final BlockRuleListener listener);

    /**
     * When a BlockRule is matched, this method is called and any
     * registered BlockRuleListeners will be alerted.
     */
    void onRuleMatched(final String line);
}
