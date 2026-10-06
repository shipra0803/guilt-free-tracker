package com.guiltfree.tracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// Request body for POST/PUT /api/fixed-expenses.
public class FixedExpenseRequest {

    // Must not be blank.
    @NotBlank
    private String name;

    // Must be a positive amount.
    @NotNull
    @DecimalMin(value = "0.01", message = "monthlyAmount must be greater than zero")
    private BigDecimal monthlyAmount;

    // Getters/setters used by Jackson to bind incoming JSON.
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getMonthlyAmount() {
        return monthlyAmount;
    }

    public void setMonthlyAmount(BigDecimal monthlyAmount) {
        this.monthlyAmount = monthlyAmount;
    }
}
