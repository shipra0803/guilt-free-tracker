package com.guiltfree.tracker.service;

import com.guiltfree.tracker.dto.TaxBreakdown;
import com.guiltfree.tracker.model.PayFrequency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Expected values are worked by hand from the 2026 single-filer figures in TaxCalculationService:
// standard deduction 16,100; brackets 10% to 12,400, 12% to 50,400, 22% to 105,700, ...;
// Social Security 6.2% up to 184,500; Medicare 1.45% uncapped.
class TaxCalculationServiceTest {

    final TaxCalculationService tax = new TaxCalculationService();

    TaxBreakdown estimate(String gross, String statePercent, PayFrequency frequency, LocalDate anchor, YearMonth month) {
        return tax.estimateForMonth(new BigDecimal(gross), new BigDecimal(statePercent), frequency, anchor, month);
    }

    TaxBreakdown monthly(String gross) {
        return estimate(gross, "0", PayFrequency.MONTHLY, null, YearMonth.of(2026, 10));
    }

    @Test
    void biweeklyEightyFiveK() {
        TaxBreakdown t = estimate("85000", "4.5", PayFrequency.BIWEEKLY, LocalDate.of(2026, 1, 2), YearMonth.of(2026, 10));
        assertEquals(new BigDecimal("9870.00"), t.federalTax());
        assertEquals(new BigDecimal("6502.50"), t.ficaTax());
        assertEquals(new BigDecimal("3825.00"), t.stateTax());
        assertEquals(new BigDecimal("64802.50"), t.netAnnual());
        assertEquals(2, t.paychecksThisMonth());
        assertEquals(new BigDecimal("4984.80"), t.netThisMonth());
    }

    // How many paydays land in a month. Anchor 2026-01-02 is a Friday.
    @ParameterizedTest(name = "{0} from {1} in {2}: {3} paychecks")
    @CsvSource({
            "BIWEEKLY, 2026-01-02, 2026-01, 3",  // Jan 2, 16, 30 - a "bonus paycheck" month
            "BIWEEKLY, 2026-01-02, 2026-02, 2",  // Feb 13, 27
            "WEEKLY,   2026-01-02, 2026-01, 5",  // every Friday in January
            "WEEKLY,   2026-01-02, 2026-02, 4",
            "BIWEEKLY, 2026-12-25, 2026-01, 2",  // anchor after the month still lines up: Jan 9, 23
            "MONTHLY,  2026-01-02, 2026-01, 1",
            "SEMI_MONTHLY, 2026-01-02, 2026-01, 2",
    })
    void paychecksInMonth(PayFrequency frequency, LocalDate anchor, YearMonth month, int expected) {
        assertEquals(expected, estimate("52000", "0", frequency, anchor, month).paychecksThisMonth());
    }

    @Test
    void bonusMonthPaysThreeTimesThePaycheck() {
        TaxBreakdown jan = estimate("85000", "4.5", PayFrequency.BIWEEKLY, LocalDate.of(2026, 1, 2), YearMonth.of(2026, 1));
        // net 64,802.50 / 26 paychecks = 2,492.40 each
        assertEquals(new BigDecimal("7477.20"), jan.netThisMonth());
    }

    @Test
    void semiMonthlyIsTwoHalfPaychecks() {
        TaxBreakdown t = estimate("60000", "0", PayFrequency.SEMI_MONTHLY, null, YearMonth.of(2026, 10));
        // net 50,390 / 24 = 2,099.58 (rounded per paycheck) * 2
        assertEquals(new BigDecimal("4199.16"), t.netThisMonth());
        assertEquals(new BigDecimal("4199.17"), monthly("60000").netThisMonth()); // 50,390 / 12
    }

    @Test
    void bracketBoundaries() {
        // Taxable income exactly at the top of the 10% bracket: 28,500 - 16,100 = 12,400.
        assertEquals(new BigDecimal("1240.00"), monthly("28500").federalTax());
        // Exactly at the top of the 12% bracket: 66,500 - 16,100 = 50,400 -> 1,240 + 38,000 * 12%.
        assertEquals(new BigDecimal("5800.00"), monthly("66500").federalTax());
        // One dollar into the 22% bracket.
        assertEquals(new BigDecimal("5800.22"), monthly("66501").federalTax());
    }

    @Test
    void incomeBelowTheStandardDeductionHasNoFederalTax() {
        TaxBreakdown t = estimate("10000", "5", PayFrequency.MONTHLY, null, YearMonth.of(2026, 10));
        assertEquals(new BigDecimal("0.00"), t.federalTax());
        assertEquals(new BigDecimal("765.00"), t.ficaTax());   // 7.65% of 10,000
        assertEquals(new BigDecimal("500.00"), t.stateTax());  // state tax has no deduction
    }

    @Test
    void socialSecurityStopsAtTheWageBase() {
        // Above 184,500 only Medicare grows: 50,000 more salary -> 1.45% = 725 more FICA.
        BigDecimal fica200k = monthly("200000").ficaTax();
        BigDecimal fica250k = monthly("250000").ficaTax();
        assertEquals(new BigDecimal("725.00"), fica250k.subtract(fica200k));
        // 184,500 * 6.2% + 200,000 * 1.45% = 11,439 + 2,900
        assertEquals(new BigDecimal("14339.00"), fica200k);
    }
}
