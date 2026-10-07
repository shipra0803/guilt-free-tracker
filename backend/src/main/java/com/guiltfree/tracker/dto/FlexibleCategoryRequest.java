package com.guiltfree.tracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// Request body for POST/PUT /api/flexible-categories.
public record FlexibleCategoryRequest(
        @NotBlank String name,
        @NotNull @DecimalMin(value = "0.01", message = "monthlyBudget must be greater than zero") BigDecimal monthlyBudget) {
}
