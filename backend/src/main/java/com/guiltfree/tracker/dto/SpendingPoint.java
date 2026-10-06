package com.guiltfree.tracker.dto;

import java.math.BigDecimal;

// One point on the History page's spending chart - a day or a month label, plus amount spent.
public class SpendingPoint {

    private final String label;
    private final BigDecimal spent;

    // Builds an immutable point from an already-computed label and amount.
    public SpendingPoint(String label, BigDecimal spent) {
        this.label = label;
        this.spent = spent;
    }

    // Getters used by Jackson to serialize this object to JSON.
    public String getLabel() {
        return label;
    }

    public BigDecimal getSpent() {
        return spent;
    }
}
