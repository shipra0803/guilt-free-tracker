package com.guiltfree.tracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

// Request body for POST /api/expenses. date is optional and defaults to today.
public class ExpenseRequest {

    // Must be a positive amount.
    @NotNull
    @DecimalMin(value = "0.01", message = "amount must be greater than zero")
    private BigDecimal amount;

    // Must not be blank.
    @NotBlank
    private String description;

    // Optional - the controller defaults this to today when missing.
    private LocalDate date;

    // Required - every expense belongs to exactly one flexible category.
    @NotNull(message = "categoryId is required - every logged expense belongs to a Flexible category")
    private Long categoryId;

    // Getters/setters used by Jackson to bind incoming JSON.
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
}
