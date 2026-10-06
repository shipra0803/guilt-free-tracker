package com.guiltfree.tracker.service;

import com.guiltfree.tracker.dto.CategoryBreakdown;
import com.guiltfree.tracker.dto.SummaryResponse;
import com.guiltfree.tracker.dto.TaxBreakdown;
import com.guiltfree.tracker.model.Expense;
import com.guiltfree.tracker.model.FixedExpense;
import com.guiltfree.tracker.model.FlexibleCategory;
import com.guiltfree.tracker.model.IncomeProfile;
import com.guiltfree.tracker.repository.ExpenseRepository;
import com.guiltfree.tracker.repository.FixedExpenseRepository;
import com.guiltfree.tracker.repository.FlexibleCategoryRepository;
import com.guiltfree.tracker.repository.IncomeProfileRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

// Computes the Fixed/Flexible/Extra-savings breakdown fresh on every call - nothing is stored.
@Service
public class CategoryBudgetService {

    private final IncomeProfileRepository incomeProfileRepository;
    private final FixedExpenseRepository fixedExpenseRepository;
    private final FlexibleCategoryRepository flexibleCategoryRepository;
    private final ExpenseRepository expenseRepository;
    private final TaxCalculationService taxCalculationService;

    // Injects the repositories and tax service needed to build the summary.
    public CategoryBudgetService(IncomeProfileRepository incomeProfileRepository,
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

    // Builds the full summary: income, fixed total, per-category breakdowns, and extra savings.
    public SummaryResponse getSummary() {
        BigDecimal netThisMonth = netThisMonth();
        BigDecimal fixedTotal = fixedTotal();
        BigDecimal remainingAfterFixed = netThisMonth.subtract(fixedTotal).setScale(2, RoundingMode.HALF_UP);

        YearMonth thisMonth = YearMonth.now();
        LocalDate start = thisMonth.atDay(1);
        LocalDate end = thisMonth.atEndOfMonth();

        List<FlexibleCategory> categories = flexibleCategoryRepository.findAll();
        List<CategoryBreakdown> breakdowns = categories.stream()
                .map(category -> toBreakdown(category, start, end))
                .collect(Collectors.toList());

        BigDecimal flexibleAllocated = categories.stream()
                .map(FlexibleCategory::getMonthlyBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal flexibleSpentThisMonth = breakdowns.stream()
                .map(CategoryBreakdown::getSpentThisMonth)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal flexibleRemainingThisMonth = flexibleAllocated.subtract(flexibleSpentThisMonth)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal extraSavings = remainingAfterFixed.subtract(flexibleAllocated).setScale(2, RoundingMode.HALF_UP);

        return new SummaryResponse(netThisMonth, fixedTotal, remainingAfterFixed, flexibleAllocated,
                flexibleSpentThisMonth, flexibleRemainingThisMonth, extraSavings, breakdowns);
    }

    // Whether any expense still references this category - used to block deletion.
    public boolean categoryHasExpenses(FlexibleCategory category) {
        return expenseRepository.existsByCategory(category);
    }

    // Computes one category's spent/remaining for the given date range.
    private CategoryBreakdown toBreakdown(FlexibleCategory category, LocalDate start, LocalDate end) {
        BigDecimal spent = expenseRepository.findByCategoryAndDateBetween(category, start, end).stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal remaining = category.getMonthlyBudget().subtract(spent).setScale(2, RoundingMode.HALF_UP);
        return new CategoryBreakdown(category.getId(), category.getName(), category.getMonthlyBudget(), spent, remaining);
    }

    // Looks up the current income profile and estimates this month's net income.
    private BigDecimal netThisMonth() {
        return incomeProfileRepository.findAll().stream()
                .findFirst()
                .map(this::netThisMonthFor)
                .orElse(BigDecimal.ZERO);
    }

    // Runs the tax calculation for a given profile and pulls out this month's net figure.
    private BigDecimal netThisMonthFor(IncomeProfile profile) {
        TaxBreakdown breakdown = taxCalculationService.estimate(
                profile.getYearlySalary(), profile.getStateTaxRatePercent(),
                profile.getPayFrequency(), profile.getAnchorPayDate());
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
