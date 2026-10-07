package com.guiltfree.tracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// Request body for POST/PUT /api/fixed-expenses.
public record FixedExpenseRequest(
        @NotBlank String name,
        @NotNull @DecimalMin(value = "0.01", message = "monthlyAmount must be greater than zero") BigDecimal monthlyAmount) {
}
