package com.guiltfree.tracker.dto;

import java.math.BigDecimal;

// Result of a tax estimate. netMonthlyAverage is netAnnual / 12; netThisMonth is what actually
// lands in the target calendar month given pay frequency and paychecksThisMonth.
public record TaxBreakdown(BigDecimal grossAnnual, BigDecimal federalTax, BigDecimal ficaTax, BigDecimal stateTax,
                           BigDecimal netAnnual, BigDecimal netMonthlyAverage, BigDecimal netThisMonth,
                           int paychecksThisMonth) {
}
