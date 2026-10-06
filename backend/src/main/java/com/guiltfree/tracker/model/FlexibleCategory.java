package com.guiltfree.tracker.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

// A discretionary spending category with its own monthly budget. Maps to "flexible_categories".
@Entity
@Table(name = "flexible_categories")
public class FlexibleCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "monthly_budget", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyBudget;

    // No-arg constructor required by JPA.
    protected FlexibleCategory() {
    }

    // Real constructor used when creating a new category in code.
    public FlexibleCategory(String name, BigDecimal monthlyBudget) {
        this.name = name;
        this.monthlyBudget = monthlyBudget;
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

    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }

    public void setMonthlyBudget(BigDecimal monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }
}
