package com.guiltfree.tracker.controller;

import com.guiltfree.tracker.dto.HistoryResponse;
import com.guiltfree.tracker.dto.HistoryResponse.SpendingPoint;
import com.guiltfree.tracker.model.Expense;
import com.guiltfree.tracker.repository.ExpenseRepository;
import com.guiltfree.tracker.repository.FixedExpenseRepository;
import com.guiltfree.tracker.repository.IncomeProfileRepository;
import com.guiltfree.tracker.service.TaxCalculationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

// Builds the History page's spending-over-time chart and range totals for a preset range.
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    // The date-range presets the History page can be viewed at (?range=...).
    public enum HistoryRange {
        WEEK, THIRTY_DAYS, THIS_MONTH, LAST_MONTH, YEAR
    }

    private final IncomeProfileRepository incomeProfileRepository;
    private final FixedExpenseRepository fixedExpenseRepository;
    private final ExpenseRepository expenseRepository;
    private final TaxCalculationService taxCalculationService;

    public HistoryController(IncomeProfileRepository incomeProfileRepository,
                           FixedExpenseRepository fixedExpenseRepository,
                           ExpenseRepository expenseRepository,
                           TaxCalculationService taxCalculationService) {
        this.incomeProfileRepository = incomeProfileRepository;
        this.fixedExpenseRepository = fixedExpenseRepository;
        this.expenseRepository = expenseRepository;
        this.taxCalculationService = taxCalculationService;
    }

    // GET /api/history?range=... (defaults to the current month); routes to the right builder.
    @GetMapping
    public HistoryResponse getHistory(@RequestParam(name = "range", defaultValue = "THIS_MONTH") HistoryRange range) {
        LocalDate today = LocalDate.now();
        YearMonth lastMonth = YearMonth.from(today).minusMonths(1);
        return switch (range) {
            case WEEK -> dailyHistory(today.minusDays(6), today, true);
            case THIRTY_DAYS -> dailyHistory(today.minusDays(29), today, true);
            case THIS_MONTH -> dailyHistory(YearMonth.from(today).atDay(1), today, false);
            case LAST_MONTH -> dailyHistory(lastMonth.atDay(1), lastMonth.atEndOfMonth(), false);
            case YEAR -> yearlyHistory(today);
        };
    }

    // One point per calendar day in [start, end]; prorates income/fixed totals for rolling windows.
    private HistoryResponse dailyHistory(LocalDate start, LocalDate end, boolean rollingWindow) {
        List<Expense> expensesInRange = expenseRepository.findByDateBetween(start, end);

        List<SpendingPoint> points = new ArrayList<>();
        BigDecimal totalSpent = BigDecimal.ZERO;
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            LocalDate d = day;
            BigDecimal spentThisDay = sum(expensesInRange.stream().filter(e -> e.getDate().equals(d)).toList());
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
            fixedTotal = fixedExpenseRepository.monthlyTotal();
        }

        BigDecimal totalOut = fixedTotal.add(totalSpent);
        return new HistoryResponse(start.toString(), end.toString(), netIncome, fixedTotal,
                totalSpent, totalOut, netIncome.subtract(totalOut), rollingWindow, points);
    }

    // One point per trailing calendar month (12 months, oldest first).
    private HistoryResponse yearlyHistory(LocalDate today) {
        YearMonth current = YearMonth.from(today);
        BigDecimal monthlyFixed = fixedExpenseRepository.monthlyTotal();

        List<SpendingPoint> points = new ArrayList<>();
        BigDecimal totalSpent = BigDecimal.ZERO;
        BigDecimal netIncome = BigDecimal.ZERO;

        for (int i = 11; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            BigDecimal spentThisMonth = sum(expenseRepository.findByDateBetween(month.atDay(1), month.atEndOfMonth()));
            points.add(new SpendingPoint(month.toString(), spentThisMonth));
            totalSpent = totalSpent.add(spentThisMonth);
            netIncome = netIncome.add(netIncomeFor(month));
        }

        totalSpent = totalSpent.setScale(2, RoundingMode.HALF_UP);
        netIncome = netIncome.setScale(2, RoundingMode.HALF_UP);
        BigDecimal fixedTotal = monthlyFixed.multiply(BigDecimal.valueOf(12));
        BigDecimal totalOut = fixedTotal.add(totalSpent);

        return new HistoryResponse(current.minusMonths(11).atDay(1).toString(), today.toString(), netIncome, fixedTotal,
                totalSpent, totalOut, netIncome.subtract(totalOut), false, points);
    }

    // Prorates net income and fixed costs across every calendar month the range touches.
    private BigDecimal[] proratedNetIncomeAndFixed(LocalDate start, LocalDate end) {
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalFixed = BigDecimal.ZERO;
        BigDecimal monthlyFixed = fixedExpenseRepository.monthlyTotal();

        for (YearMonth month = YearMonth.from(start); !month.isAfter(YearMonth.from(end)); month = month.plusMonths(1)) {
            LocalDate monthStart = month.atDay(1).isBefore(start) ? start : month.atDay(1);
            LocalDate monthEnd = month.atEndOfMonth().isAfter(end) ? end : month.atEndOfMonth();
            long daysInRange = ChronoUnit.DAYS.between(monthStart, monthEnd) + 1;
            BigDecimal fraction = BigDecimal.valueOf(daysInRange)
                    .divide(BigDecimal.valueOf(month.lengthOfMonth()), 10, RoundingMode.HALF_UP);

            totalNet = totalNet.add(netIncomeFor(month).multiply(fraction));
            totalFixed = totalFixed.add(monthlyFixed.multiply(fraction));
        }

        return new BigDecimal[]{
                totalNet.setScale(2, RoundingMode.HALF_UP),
                totalFixed.setScale(2, RoundingMode.HALF_UP)
        };
    }

    // Estimates net income for the given month from the saved profile (zero if none).
    private BigDecimal netIncomeFor(YearMonth month) {
        return incomeProfileRepository.current()
                .map(p -> taxCalculationService.estimateForMonth(p.getYearlySalary(), p.getStateTaxRatePercent(),
                        p.getPayFrequency(), p.getAnchorPayDate(), month).netThisMonth())
                .orElse(BigDecimal.ZERO);
    }

    private static BigDecimal sum(List<Expense> expenses) {
        return expenses.stream().map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }
}
