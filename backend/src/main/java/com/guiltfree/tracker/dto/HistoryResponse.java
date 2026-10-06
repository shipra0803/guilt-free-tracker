package com.guiltfree.tracker.dto;

import java.math.BigDecimal;
import java.util.List;

// Payload behind GET /api/history - a spending-over-time chart plus range-level totals.
public class HistoryResponse {

    // The range shown, plus its totals.
    private final String startDate;
    private final String endDate;
    private final BigDecimal netIncome;
    private final BigDecimal fixedTotal;
    private final BigDecimal totalSpent;
    private final BigDecimal totalOut;
    private final BigDecimal difference;

    // True when netIncome/fixedTotal are prorated estimates rather than exact figures.
    private final boolean estimated;

    // The chart data - one point per day or per month depending on the range.
    private final List<SpendingPoint> points;

    // Builds an immutable response from already-computed values.
    public HistoryResponse(String startDate, String endDate, BigDecimal netIncome, BigDecimal fixedTotal,
                            BigDecimal totalSpent, BigDecimal totalOut, BigDecimal difference,
                            boolean estimated, List<SpendingPoint> points) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.netIncome = netIncome;
        this.fixedTotal = fixedTotal;
        this.totalSpent = totalSpent;
        this.totalOut = totalOut;
        this.difference = difference;
        this.estimated = estimated;
        this.points = points;
    }

    // Getters used by Jackson to serialize this object to JSON.
    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public BigDecimal getNetIncome() {
        return netIncome;
    }

    public BigDecimal getFixedTotal() {
        return fixedTotal;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public BigDecimal getTotalOut() {
        return totalOut;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public boolean isEstimated() {
        return estimated;
    }

    public List<SpendingPoint> getPoints() {
        return points;
    }
}
