package com.guiltfree.tracker.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

// A recurring monthly fixed cost (rent, utilities, etc). Maps to the "fixed_expenses" table.
@Entity
@Table(name = "fixed_expenses")
public class FixedExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "monthly_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyAmount;

    // No-arg constructor required by JPA.
    protected FixedExpense() {
    }

    // Real constructor used when creating a new fixed expense in code.
    public FixedExpense(String name, BigDecimal monthlyAmount) {
        this.name = name;
        this.monthlyAmount = monthlyAmount;
    }

    // Getters/setters used by JPA and the rest of the app to read/write fields.
    public Long getId() {
        return id;
    }

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
