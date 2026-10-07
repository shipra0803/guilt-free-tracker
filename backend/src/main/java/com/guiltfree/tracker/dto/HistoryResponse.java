package com.guiltfree.tracker.dto;

import java.math.BigDecimal;
import java.util.List;

// Payload behind GET /api/history - a spending-over-time chart plus range-level totals.
// estimated is true when netIncome/fixedTotal are prorated rather than exact; points has one
// entry per day or per month depending on the range.
public record HistoryResponse(String startDate, String endDate, BigDecimal netIncome, BigDecimal fixedTotal,
                              BigDecimal totalSpent, BigDecimal totalOut, BigDecimal difference,
                              boolean estimated, List<SpendingPoint> points) {

    // One chart point - a day or a month label, plus the amount spent.
    public record SpendingPoint(String label, BigDecimal spent) {
    }
}
