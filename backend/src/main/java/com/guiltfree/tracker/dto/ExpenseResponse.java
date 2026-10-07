package com.guiltfree.tracker.dto;

import com.guiltfree.tracker.model.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;

// Response shape for an expense - flattens in the category's name so the frontend doesn't
// need a second lookup.
public record ExpenseResponse(Long id, BigDecimal amount, String description, LocalDate date,
                              Long categoryId, String categoryName) {

    public static ExpenseResponse from(Expense e) {
        return new ExpenseResponse(e.getId(), e.getAmount(), e.getDescription(), e.getDate(),
                e.getCategory().getId(), e.getCategory().getName());
    }
}
