package com.guiltfree.tracker.dto;

import java.math.BigDecimal;

// Result of a tax estimate - either a live preview or the saved income profile's breakdown.
public class TaxBreakdown {

    // Gross salary and each deduction taken out of it.
    private final BigDecimal grossAnnual;
    private final BigDecimal federalTax;
    private final BigDecimal ficaTax;
    private final BigDecimal stateTax;
    private final BigDecimal netAnnual;

    // netAnnual / 12 - a frequency-independent, evenly-spread reference figure.
    private final BigDecimal netMonthlyAverage;

    // What actually lands in the current calendar month, given pay frequency and paycheck count.
    private final BigDecimal netThisMonth;

    // How many paychecks fall in the current calendar month.
    private final int paychecksThisMonth;

    // Builds an immutable breakdown from already-computed values.
    public TaxBreakdown(BigDecimal grossAnnual, BigDecimal federalTax, BigDecimal ficaTax, BigDecimal stateTax,
                         BigDecimal netAnnual, BigDecimal netMonthlyAverage, BigDecimal netThisMonth,
                         int paychecksThisMonth) {
        this.grossAnnual = grossAnnual;
        this.federalTax = federalTax;
        this.ficaTax = ficaTax;
        this.stateTax = stateTax;
        this.netAnnual = netAnnual;
        this.netMonthlyAverage = netMonthlyAverage;
        this.netThisMonth = netThisMonth;
        this.paychecksThisMonth = paychecksThisMonth;
    }

    // Getters used by Jackson to serialize this object to JSON.
    public BigDecimal getGrossAnnual() {
        return grossAnnual;
    }

    public BigDecimal getFederalTax() {
        return federalTax;
    }

    public BigDecimal getFicaTax() {
        return ficaTax;
    }

    public BigDecimal getStateTax() {
        return stateTax;
    }

    public BigDecimal getNetAnnual() {
        return netAnnual;
    }

    public BigDecimal getNetMonthlyAverage() {
        return netMonthlyAverage;
    }

    public BigDecimal getNetThisMonth() {
        return netThisMonth;
    }

    public int getPaychecksThisMonth() {
        return paychecksThisMonth;
    }
}
