package com.guiltfree.tracker.dto;

import java.math.BigDecimal;
import java.util.List;

// Payload behind GET /api/summary - everything the Home and Settings pages need, computed
// fresh from IncomeProfile + FixedExpense + FlexibleCategory + this month's Expenses.
public class SummaryResponse {

    // The top-level totals: income, fixed costs, and what's left after each stage.
    private final BigDecimal netThisMonth;
    private final BigDecimal fixedTotal;
    private final BigDecimal remainingAfterFixed;
    private final BigDecimal flexibleAllocated;
    private final BigDecimal flexibleSpentThisMonth;
    private final BigDecimal flexibleRemainingThisMonth;
    private final BigDecimal extraSavings;

    // Per-category budget/spend breakdown.
    private final List<CategoryBreakdown> categories;

    // Builds an immutable response from already-computed values.
    public SummaryResponse(BigDecimal netThisMonth, BigDecimal fixedTotal, BigDecimal remainingAfterFixed,
                            BigDecimal flexibleAllocated, BigDecimal flexibleSpentThisMonth,
                            BigDecimal flexibleRemainingThisMonth, BigDecimal extraSavings,
                            List<CategoryBreakdown> categories) {
        this.netThisMonth = netThisMonth;
        this.fixedTotal = fixedTotal;
        this.remainingAfterFixed = remainingAfterFixed;
        this.flexibleAllocated = flexibleAllocated;
        this.flexibleSpentThisMonth = flexibleSpentThisMonth;
        this.flexibleRemainingThisMonth = flexibleRemainingThisMonth;
        this.extraSavings = extraSavings;
        this.categories = categories;
    }

    // Getters used by Jackson to serialize this object to JSON.
    public BigDecimal getNetThisMonth() {
        return netThisMonth;
    }

    public BigDecimal getFixedTotal() {
        return fixedTotal;
    }

    public BigDecimal getRemainingAfterFixed() {
        return remainingAfterFixed;
    }

    public BigDecimal getFlexibleAllocated() {
        return flexibleAllocated;
    }

    public BigDecimal getFlexibleSpentThisMonth() {
        return flexibleSpentThisMonth;
    }

    public BigDecimal getFlexibleRemainingThisMonth() {
        return flexibleRemainingThisMonth;
    }

    public BigDecimal getExtraSavings() {
        return extraSavings;
    }

    public List<CategoryBreakdown> getCategories() {
        return categories;
    }
}
