package com.guiltfree.tracker.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

// Single-row settings entity holding salary/tax/pay-schedule info. Maps to "income_profiles".
@Entity
@Table(name = "income_profiles")
public class IncomeProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "yearly_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal yearlySalary;

    // User-supplied flat rate; zero is valid for no-income-tax states.
    @Column(name = "state_tax_rate_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal stateTaxRatePercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "pay_frequency", nullable = false)
    private PayFrequency payFrequency;

    // Any known real payday; only used for BIWEEKLY/WEEKLY paycheck counting.
    @Column(name = "anchor_pay_date")
    private LocalDate anchorPayDate;

    // No-arg constructor required by JPA.
    protected IncomeProfile() {
    }

    // Convenience constructor defaulting to monthly pay with no anchor date.
    public IncomeProfile(BigDecimal yearlySalary, BigDecimal stateTaxRatePercent) {
        this(yearlySalary, stateTaxRatePercent, PayFrequency.MONTHLY, null);
    }

    // Full constructor used when creating a new profile in code.
    public IncomeProfile(BigDecimal yearlySalary, BigDecimal stateTaxRatePercent,
                          PayFrequency payFrequency, LocalDate anchorPayDate) {
        this.yearlySalary = yearlySalary;
        this.stateTaxRatePercent = stateTaxRatePercent;
        this.payFrequency = payFrequency;
        this.anchorPayDate = anchorPayDate;
    }

    // Getters/setters used by JPA and the rest of the app to read/write fields.
    public Long getId() {
        return id;
    }

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
