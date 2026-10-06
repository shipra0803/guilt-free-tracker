package com.guiltfree.tracker.dto;

import com.guiltfree.tracker.model.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;

// Response shape for an expense - flattens in the category's name so the frontend doesn't
// need a second lookup.
public class ExpenseResponse {

    private final Long id;
    private final BigDecimal amount;
    private final String description;
    private final LocalDate date;
    private final Long categoryId;
    private final String categoryName;

    // Builds the response directly from an Expense entity.
    public ExpenseResponse(Expense expense) {
        this.id = expense.getId();
        this.amount = expense.getAmount();
        this.description = expense.getDescription();
        this.date = expense.getDate();
        this.categoryId = expense.getCategory().getId();
        this.categoryName = expense.getCategory().getName();
    }

    // Getters used by Jackson to serialize this object to JSON.
    public Long getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDate() {
        return date;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }
}
