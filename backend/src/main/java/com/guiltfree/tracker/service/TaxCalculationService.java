package com.guiltfree.tracker.service;

import com.guiltfree.tracker.dto.TaxBreakdown;
import com.guiltfree.tracker.model.PayFrequency;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

// Estimates take-home pay from a gross yearly salary (federal brackets, FICA, flat state rate).
@Service
public class TaxCalculationService {

    // 2026 standard deduction, single filer (IRS Revenue Procedure 2025-32).
    private static final BigDecimal STANDARD_DEDUCTION_SINGLE_2026 = new BigDecimal("16100.00");

    // 2026 federal marginal tax brackets, single filer (same source as above).
    private static final TaxBracket[] FEDERAL_BRACKETS_SINGLE_2026 = {
            new TaxBracket(new BigDecimal("0"), new BigDecimal("0.10")),
            new TaxBracket(new BigDecimal("12400"), new BigDecimal("0.12")),
            new TaxBracket(new BigDecimal("50400"), new BigDecimal("0.22")),
            new TaxBracket(new BigDecimal("105700"), new BigDecimal("0.24")),
            new TaxBracket(new BigDecimal("201775"), new BigDecimal("0.32")),
            new TaxBracket(new BigDecimal("256225"), new BigDecimal("0.35")),
            new TaxBracket(new BigDecimal("640600"), new BigDecimal("0.37")),
    };

    // Social Security wage base and rate (ssa.gov), and Medicare's uncapped rate.
    private static final BigDecimal SOCIAL_SECURITY_WAGE_BASE_2026 = new BigDecimal("184500.00");
    private static final BigDecimal SOCIAL_SECURITY_RATE = new BigDecimal("0.062");

    private static final BigDecimal MEDICARE_RATE = new BigDecimal("0.0145");

    // Estimates the tax breakdown for the current calendar month.
    public TaxBreakdown estimate(BigDecimal grossAnnual, BigDecimal stateTaxRatePercent,
                                  PayFrequency payFrequency, LocalDate anchorPayDate) {
        return estimateForMonth(grossAnnual, stateTaxRatePercent, payFrequency, anchorPayDate, YearMonth.now());
    }

    // Estimates the tax breakdown for an arbitrary target month.
    public TaxBreakdown estimateForMonth(BigDecimal grossAnnual, BigDecimal stateTaxRatePercent,
                                          PayFrequency payFrequency, LocalDate anchorPayDate, YearMonth targetMonth) {
        BigDecimal taxableIncome = grossAnnual.subtract(STANDARD_DEDUCTION_SINGLE_2026).max(BigDecimal.ZERO);
        BigDecimal federalTax = calculateMarginalTax(taxableIncome, FEDERAL_BRACKETS_SINGLE_2026);
        BigDecimal ficaTax = calculateFica(grossAnnual);
        BigDecimal stateTax = grossAnnual.multiply(stateTaxRatePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal netAnnual = grossAnnual.subtract(federalTax).subtract(ficaTax).subtract(stateTax);
        BigDecimal netMonthlyAverage = netAnnual.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        BigDecimal netPerPaycheck = netAnnual.divide(BigDecimal.valueOf(paychecksPerYear(payFrequency)), 2, RoundingMode.HALF_UP);

        // Falls back to today if no anchor was given; safe since MONTHLY/SEMI_MONTHLY never use it.
        LocalDate effectiveAnchor = anchorPayDate != null ? anchorPayDate : LocalDate.now();
        int paychecksThisMonth = paychecksInMonth(payFrequency, effectiveAnchor, targetMonth);
        BigDecimal netThisMonth = netPerPaycheck.multiply(BigDecimal.valueOf(paychecksThisMonth))
                .setScale(2, RoundingMode.HALF_UP);

        return new TaxBreakdown(
                round(grossAnnual), round(federalTax), round(ficaTax), round(stateTax),
                round(netAnnual), round(netMonthlyAverage), netThisMonth, paychecksThisMonth);
    }

    // How many paychecks a year for this frequency.
    private static int paychecksPerYear(PayFrequency frequency) {
        return switch (frequency) {
            case MONTHLY -> 12;
            case SEMI_MONTHLY -> 24;
            case BIWEEKLY -> 26;
            case WEEKLY -> 52;
        };
    }

    // How many paychecks land within the given calendar month.
    private static int paychecksInMonth(PayFrequency frequency, LocalDate anchorPayDate, YearMonth month) {
        return switch (frequency) {
            case MONTHLY -> 1;
            case SEMI_MONTHLY -> 2;
            case BIWEEKLY -> countPaydaysInMonth(anchorPayDate, month, 14);
            case WEEKLY -> countPaydaysInMonth(anchorPayDate, month, 7);
        };
    }

    // Walks every day in the month and counts days that land exactly on a pay cycle.
    private static int countPaydaysInMonth(LocalDate anchorPayDate, YearMonth month, int periodDays) {
        int count = 0;
        for (LocalDate day = month.atDay(1); !day.isAfter(month.atEndOfMonth()); day = day.plusDays(1)) {
            if (Math.floorMod(ChronoUnit.DAYS.between(anchorPayDate, day), periodDays) == 0) {
                count++;
            }
        }
        return count;
    }

    // Standard marginal bracket calculation - each dollar taxed at its own bracket's rate.
    private BigDecimal calculateMarginalTax(BigDecimal taxableIncome, TaxBracket[] brackets) {
        BigDecimal tax = BigDecimal.ZERO;

        for (int i = 0; i < brackets.length; i++) {
            BigDecimal bracketStart = brackets[i].start();
            if (taxableIncome.compareTo(bracketStart) <= 0) {
                break;
            }

            boolean isTopBracket = i == brackets.length - 1;
            BigDecimal bracketEnd = isTopBracket ? taxableIncome : brackets[i + 1].start();
            BigDecimal amountTaxedAtThisRate = taxableIncome.min(bracketEnd).subtract(bracketStart);

            tax = tax.add(amountTaxedAtThisRate.multiply(brackets[i].rate()));
        }

        return tax;
    }

    // Social Security (capped at the wage base) plus uncapped Medicare.
    private BigDecimal calculateFica(BigDecimal grossAnnual) {
        BigDecimal socialSecurityTax = grossAnnual.min(SOCIAL_SECURITY_WAGE_BASE_2026).multiply(SOCIAL_SECURITY_RATE);
        BigDecimal medicareTax = grossAnnual.multiply(MEDICARE_RATE);
        return socialSecurityTax.add(medicareTax);
    }

    // Rounds to 2 decimal places for currency display.
    private BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    // One bracket: income above `start` (up to the next bracket) is taxed at `rate`.
    private record TaxBracket(BigDecimal start, BigDecimal rate) {
    }
}
