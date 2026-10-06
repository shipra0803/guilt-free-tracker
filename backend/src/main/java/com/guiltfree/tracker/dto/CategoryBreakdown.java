package com.guiltfree.tracker.dto;

import java.math.BigDecimal;

// One flexible category's budget vs. actual spend for the current month.
public class CategoryBreakdown {

    // The category's id, name, budget, amount spent, and amount remaining this month.
    private final Long id;
    private final String name;
    private final BigDecimal monthlyBudget;
    private final BigDecimal spentThisMonth;
    private final BigDecimal remaining;

    // Builds an immutable breakdown from already-computed values.
    public CategoryBreakdown(Long id, String name, BigDecimal monthlyBudget,
                              BigDecimal spentThisMonth, BigDecimal remaining) {
        this.id = id;
        this.name = name;
        this.monthlyBudget = monthlyBudget;
        this.spentThisMonth = spentThisMonth;
        this.remaining = remaining;
    }

    // Getters used by Jackson to serialize this object to JSON.
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }

    public BigDecimal getSpentThisMonth() {
        return spentThisMonth;
    }

    public BigDecimal getRemaining() {
        return remaining;
    }
}
