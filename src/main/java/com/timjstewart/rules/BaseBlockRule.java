package com.timjstewart.rules;

import java.util.Objects;
import java.util.List;
import java.util.ArrayList;

public abstract class BaseBlockRule implements BlockRule {

    private final List<BlockRuleListener> listeners = new ArrayList<>();
    
    public void addListener(final BlockRuleListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener cannot be null"));
    }

    public void onRuleMatched(final String line) {
        for (final BlockRuleListener listener : listeners) {
            listener.onRuleMatched(line);
        }
    }

}
