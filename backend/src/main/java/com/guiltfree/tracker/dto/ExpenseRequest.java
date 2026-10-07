package com.guiltfree.tracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

// Request body for POST /api/expenses. date is optional and defaults to today.
public record ExpenseRequest(
        @NotNull @DecimalMin(value = "0.01", message = "amount must be greater than zero") BigDecimal amount,
        @NotBlank String description,
        LocalDate date,
        @NotNull(message = "categoryId is required - every logged expense belongs to a Flexible category") Long categoryId) {
}
