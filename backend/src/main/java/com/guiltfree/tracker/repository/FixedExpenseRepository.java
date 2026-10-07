package com.guiltfree.tracker.repository;

import com.guiltfree.tracker.model.FixedExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Data access for fixed expenses - standard CRUD plus the monthly total.
public interface FixedExpenseRepository extends JpaRepository<FixedExpense, Long> {

    // Sums monthlyAmount across all fixed expenses.
    default BigDecimal monthlyTotal() {
        return findAll().stream()
                .map(FixedExpense::getMonthlyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
