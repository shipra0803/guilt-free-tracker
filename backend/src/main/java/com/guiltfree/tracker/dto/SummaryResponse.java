package com.guiltfree.tracker.dto;

import java.math.BigDecimal;
import java.util.List;

// Payload behind GET /api/summary - everything the Home and Settings pages need, computed
// fresh from IncomeProfile + FixedExpense + FlexibleCategory + this month's Expenses.
public record SummaryResponse(BigDecimal netThisMonth, BigDecimal fixedTotal, BigDecimal remainingAfterFixed,
                              BigDecimal flexibleAllocated, BigDecimal flexibleSpentThisMonth,
                              BigDecimal flexibleRemainingThisMonth, BigDecimal extraSavings,
                              List<CategoryBreakdown> categories) {

    // One flexible category's budget vs. actual spend for the current month.
    public record CategoryBreakdown(Long id, String name, BigDecimal monthlyBudget,
                                    BigDecimal spentThisMonth, BigDecimal remaining) {
    }
}
