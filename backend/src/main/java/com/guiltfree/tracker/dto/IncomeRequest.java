package com.guiltfree.tracker.dto;

import com.guiltfree.tracker.model.PayFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

// Request body for POST /api/income/estimate and PUT /api/income.
public class IncomeRequest {

    // Must be a positive salary.
    @NotNull
    @DecimalMin(value = "0.01", message = "yearlySalary must be greater than zero")
    private BigDecimal yearlySalary;

    // Zero is valid - not every state taxes wages.
    @NotNull
    @DecimalMin(value = "0.00", message = "stateTaxRatePercent cannot be negative")
    private BigDecimal stateTaxRatePercent;

    // Required - drives the paycheck-counting logic.
    @NotNull
    private PayFrequency payFrequency;

    // Only meaningful (and only required by the frontend) for BIWEEKLY/WEEKLY.
    private LocalDate anchorPayDate;

    // Getters/setters used by Jackson to bind incoming JSON.
    public BigDecimal getYearlySalary() {
        return yearlySalary;
    }

    public void setYearlySalary(BigDecimal yearlySalary) {
        this.yearlySalary = yearlySalary;
    }

    public BigDecimal getStateTaxRatePercent() {
        return stateTaxRatePercent;
    }

    public void setStateTaxRatePercent(BigDecimal stateTaxRatePercent) {
        this.stateTaxRatePercent = stateTaxRatePercent;
    }

    public PayFrequency getPayFrequency() {
        return payFrequency;
    }

    public void setPayFrequency(PayFrequency payFrequency) {
        this.payFrequency = payFrequency;
    }

    public LocalDate getAnchorPayDate() {
        return anchorPayDate;
    }

    public void setAnchorPayDate(LocalDate anchorPayDate) {
        this.anchorPayDate = anchorPayDate;
    }
}
