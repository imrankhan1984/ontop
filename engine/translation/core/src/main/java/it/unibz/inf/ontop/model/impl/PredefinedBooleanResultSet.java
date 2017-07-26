package it.unibz.inf.ontop.model.impl;

import it.unibz.inf.ontop.model.BooleanResultSet;

/**
 * Predefined value
 */
public class PredefinedBooleanResultSet implements BooleanResultSet {

    private final boolean value;

    public PredefinedBooleanResultSet(boolean value) {
        this.value = value;
    }

    @Override
    public boolean getValue() {
        return value;
    }

    @Override
    public void close() {
    }
}
