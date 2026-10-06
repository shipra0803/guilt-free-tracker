package com.guiltfree.tracker.service;

import com.guiltfree.tracker.dto.HistoryResponse;
import com.guiltfree.tracker.dto.SpendingPoint;
import com.guiltfree.tracker.dto.TaxBreakdown;
import com.guiltfree.tracker.model.Expense;
import com.guiltfree.tracker.model.FixedExpense;
import com.guiltfree.tracker.model.HistoryRange;
import com.guiltfree.tracker.model.IncomeProfile;
import com.guiltfree.tracker.repository.ExpenseRepository;
import com.guiltfree.tracker.repository.FixedExpenseRepository;
import com.guiltfree.tracker.repository.IncomeProfileRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

// Builds the History page's spending-over-time chart and range totals for a preset range.
@Service
public class HistoryService {

    private final IncomeProfileRepository incomeProfileRepository;
    private final FixedExpenseRepository fixedExpenseRepository;
    private final ExpenseRepository expenseRepository;
    private final TaxCalculationService taxCalculationService;

    // Injects the repositories and tax service needed to build history data.
    public HistoryService(IncomeProfileRepository incomeProfileRepository,
                           FixedExpenseRepository fixedExpenseRepository,
                           ExpenseRepository expenseRepository,
                           TaxCalculationService taxCalculationService) {
        this.incomeProfileRepository = incomeProfileRepository;
        this.fixedExpenseRepository = fixedExpenseRepository;
        this.expenseRepository = expenseRepository;
        this.taxCalculationService = taxCalculationService;
    }

    // Routes to the right builder based on which preset range was requested.
    public HistoryResponse getHistory(HistoryRange range) {
        LocalDate today = LocalDate.now();
        switch (range) {
            case WEEK:
                return dailyHistory(today.minusDays(6), today, true);
            case THIRTY_DAYS:
                return dailyHistory(today.minusDays(29), today, true);
            case THIS_MONTH:
                return dailyHistory(YearMonth.from(today).atDay(1), today, false);
            case LAST_MONTH:
                YearMonth lastMonth = YearMonth.from(today).minusMonths(1);
                return dailyHistory(lastMonth.atDay(1), lastMonth.atEndOfMonth(), false);
            case YEAR:
                return yearlyHistory(today);
            default:
                throw new IllegalArgumentException("Unhandled range: " + range);
        }
    }

    // One point per calendar day in [start, end]; prorates income/fixed totals for rolling windows.
    private HistoryResponse dailyHistory(LocalDate start, LocalDate end, boolean rollingWindow) {
        List<Expense> expensesInRange = expenseRepository.findByDateBetweenOrderByDateAsc(start, end);

        List<SpendingPoint> points = new ArrayList<>();
        BigDecimal totalSpent = BigDecimal.ZERO;
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            BigDecimal spentThisDay = spentOn(expensesInRange, day);
            points.add(new SpendingPoint(day.toString(), spentThisDay));
            totalSpent = totalSpent.add(spentThisDay);
        }
        totalSpent = totalSpent.setScale(2, RoundingMode.HALF_UP);

        BigDecimal netIncome;
        BigDecimal fixedTotal;
        if (rollingWindow) {
            BigDecimal[] prorated = proratedNetIncomeAndFixed(start, end);
            netIncome = prorated[0];
            fixedTotal = prorated[1];
        } else {
            // start/end fall inside a single calendar month, so the exact figure applies.
            netIncome = netIncomeFor(YearMonth.from(start));
            fixedTotal = fixedTotal();
        }

        BigDecimal totalOut = fixedTotal.add(totalSpent).setScale(2, RoundingMode.HALF_UP);
        BigDecimal difference = netIncome.subtract(totalOut).setScale(2, RoundingMode.HALF_UP);

        return new HistoryResponse(start.toString(), end.toString(), netIncome, fixedTotal,
                totalSpent, totalOut, difference, rollingWindow, points);
    }

    // One point per trailing calendar month (12 months, oldest first).
    private HistoryResponse yearlyHistory(LocalDate today) {
        YearMonth current = YearMonth.from(today);
        YearMonth oldestMonth = current.minusMonths(11);
        BigDecimal monthlyFixed = fixedTotal();

        List<SpendingPoint> points = new ArrayList<>();
        BigDecimal totalSpent = BigDecimal.ZERO;
        BigDecimal netIncome = BigDecimal.ZERO;
        BigDecimal fixedTotal = BigDecimal.ZERO;

        for (int i = 11; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            BigDecimal spentThisMonth = expenseRepository
                    .findByDateBetweenOrderByDateAsc(month.atDay(1), month.atEndOfMonth()).stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            points.add(new SpendingPoint(month.toString(), spentThisMonth));
            totalSpent = totalSpent.add(spentThisMonth);
            netIncome = netIncome.add(netIncomeFor(month));
            fixedTotal = fixedTotal.add(monthlyFixed);
        }

        totalSpent = totalSpent.setScale(2, RoundingMode.HALF_UP);
        netIncome = netIncome.setScale(2, RoundingMode.HALF_UP);
        fixedTotal = fixedTotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalOut = fixedTotal.add(totalSpent).setScale(2, RoundingMode.HALF_UP);
        BigDecimal difference = netIncome.subtract(totalOut).setScale(2, RoundingMode.HALF_UP);

        return new HistoryResponse(oldestMonth.atDay(1).toString(), today.toString(), netIncome, fixedTotal,
                totalSpent, totalOut, difference, false, points);
    }

    // Sums expense amounts for one specific day.
    private BigDecimal spentOn(List<Expense> expenses, LocalDate day) {
        return expenses.stream()
                .filter(e -> e.getDate().equals(day))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // Prorates net income and fixed costs across every calendar month the range touches.
    private BigDecimal[] proratedNetIncomeAndFixed(LocalDate start, LocalDate end) {
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalFixed = BigDecimal.ZERO;
        BigDecimal monthlyFixed = fixedTotal();

        YearMonth month = YearMonth.from(start);
        YearMonth lastMonth = YearMonth.from(end);
        while (!month.isAfter(lastMonth)) {
            LocalDate monthStart = month.atDay(1).isBefore(start) ? start : month.atDay(1);
            LocalDate monthEnd = month.atEndOfMonth().isAfter(end) ? end : month.atEndOfMonth();
            long daysInRange = ChronoUnit.DAYS.between(monthStart, monthEnd) + 1;
            BigDecimal fraction = BigDecimal.valueOf(daysInRange)
                    .divide(BigDecimal.valueOf(month.lengthOfMonth()), 10, RoundingMode.HALF_UP);

            totalNet = totalNet.add(netIncomeFor(month).multiply(fraction));
            totalFixed = totalFixed.add(monthlyFixed.multiply(fraction));
            month = month.plusMonths(1);
        }

        return new BigDecimal[]{
                totalNet.setScale(2, RoundingMode.HALF_UP),
                totalFixed.setScale(2, RoundingMode.HALF_UP)
        };
    }

    // Looks up the current income profile and estimates net income for the given month.
    private BigDecimal netIncomeFor(YearMonth month) {
        return incomeProfileRepository.findAll().stream()
                .findFirst()
                .map(profile -> netIncomeFor(profile, month))
                .orElse(BigDecimal.ZERO);
    }

    // Runs the tax calculation for a given profile and month.
    private BigDecimal netIncomeFor(IncomeProfile profile, YearMonth month) {
        TaxBreakdown breakdown = taxCalculationService.estimateForMonth(
                profile.getYearlySalary(), profile.getStateTaxRatePercent(),
                profile.getPayFrequency(), profile.getAnchorPayDate(), month);
        return breakdown.getNetThisMonth();
    }

    // Sums monthlyAmount across all fixed expenses.
    private BigDecimal fixedTotal() {
        return fixedExpenseRepository.findAll().stream()
                .map(FixedExpense::getMonthlyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
