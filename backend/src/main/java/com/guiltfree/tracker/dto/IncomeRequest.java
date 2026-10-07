package com.guiltfree.tracker.dto;

import com.guiltfree.tracker.model.PayFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

// Request body for POST /api/income/estimate and PUT /api/income. A zero state rate is valid
// (not every state taxes wages); anchorPayDate only matters for BIWEEKLY/WEEKLY.
public record IncomeRequest(
        @NotNull @DecimalMin(value = "0.01", message = "yearlySalary must be greater than zero") BigDecimal yearlySalary,
        @NotNull @DecimalMin(value = "0.00", message = "stateTaxRatePercent cannot be negative") BigDecimal stateTaxRatePercent,
        @NotNull PayFrequency payFrequency,
        LocalDate anchorPayDate) {
}
