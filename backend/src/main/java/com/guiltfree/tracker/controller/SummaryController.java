package com.guiltfree.tracker.controller;

import com.guiltfree.tracker.dto.SummaryResponse.CategoryBreakdown;
import com.guiltfree.tracker.dto.SummaryResponse;
import com.guiltfree.tracker.model.Expense;
import com.guiltfree.tracker.model.FlexibleCategory;
import com.guiltfree.tracker.repository.ExpenseRepository;
import com.guiltfree.tracker.repository.FixedExpenseRepository;
import com.guiltfree.tracker.repository.FlexibleCategoryRepository;
import com.guiltfree.tracker.repository.IncomeProfileRepository;
import com.guiltfree.tracker.service.TaxCalculationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

// Computes the Fixed/Flexible/Extra-savings breakdown fresh on every call - nothing is stored.
@RestController
@RequestMapping("/api/summary")
public class SummaryController {

    private final IncomeProfileRepository incomeProfileRepository;
    private final FixedExpenseRepository fixedExpenseRepository;
    private final FlexibleCategoryRepository flexibleCategoryRepository;
    private final ExpenseRepository expenseRepository;
    private final TaxCalculationService taxCalculationService;

    public SummaryController(IncomeProfileRepository incomeProfileRepository,
                                  FixedExpenseRepository fixedExpenseRepository,
                                  FlexibleCategoryRepository flexibleCategoryRepository,
                                  ExpenseRepository expenseRepository,
                                  TaxCalculationService taxCalculationService) {
        this.incomeProfileRepository = incomeProfileRepository;
        this.fixedExpenseRepository = fixedExpenseRepository;
        this.flexibleCategoryRepository = flexibleCategoryRepository;
        this.expenseRepository = expenseRepository;
        this.taxCalculationService = taxCalculationService;
    }

    // GET /api/summary: income, fixed total, per-category breakdowns, and extra savings - computed fresh.
    @GetMapping
    public SummaryResponse getSummary() {
        BigDecimal netThisMonth = incomeProfileRepository.current()
                .map(p -> taxCalculationService.estimate(p.getYearlySalary(), p.getStateTaxRatePercent(),
                        p.getPayFrequency(), p.getAnchorPayDate()).netThisMonth())
                .orElse(BigDecimal.ZERO);
        BigDecimal fixedTotal = fixedExpenseRepository.monthlyTotal();
        BigDecimal remainingAfterFixed = netThisMonth.subtract(fixedTotal);

        YearMonth thisMonth = YearMonth.now();
        LocalDate start = thisMonth.atDay(1);
        LocalDate end = thisMonth.atEndOfMonth();

        List<FlexibleCategory> categories = flexibleCategoryRepository.findAll();
        List<CategoryBreakdown> breakdowns = categories.stream()
                .map(category -> toBreakdown(category, start, end))
                .toList();

        BigDecimal flexibleAllocated = sum(categories.stream().map(FlexibleCategory::getMonthlyBudget).toList());
        BigDecimal flexibleSpentThisMonth = sum(breakdowns.stream().map(CategoryBreakdown::spentThisMonth).toList());
        BigDecimal flexibleRemainingThisMonth = flexibleAllocated.subtract(flexibleSpentThisMonth);
        BigDecimal extraSavings = remainingAfterFixed.subtract(flexibleAllocated);

        return new SummaryResponse(netThisMonth, fixedTotal, remainingAfterFixed, flexibleAllocated,
                flexibleSpentThisMonth, flexibleRemainingThisMonth, extraSavings, breakdowns);
    }

    // Computes one category's spent/remaining for the given date range.
    private CategoryBreakdown toBreakdown(FlexibleCategory category, LocalDate start, LocalDate end) {
        BigDecimal spent = sum(expenseRepository.findByCategoryAndDateBetween(category, start, end).stream()
                .map(Expense::getAmount).toList());
        BigDecimal remaining = category.getMonthlyBudget().subtract(spent);
        return new CategoryBreakdown(category.getId(), category.getName(), category.getMonthlyBudget(), spent, remaining);
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }
}
