package com.guiltfree.tracker.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

// One logged transaction against a flexible category. Maps to the "expenses" table.
@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String description;

    // Column named "expense_date" (not "date") to avoid an H2 reserved-word conflict.
    @Column(name = "expense_date", nullable = false)
    private LocalDate date;

    // Required - every expense belongs to exactly one flexible category, never a fixed one.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flexible_category_id", nullable = false)
    private FlexibleCategory category;

    // No-arg constructor required by JPA.
    protected Expense() {
    }

    // Real constructor used when creating a new expense in code.
    public Expense(BigDecimal amount, String description, LocalDate date, FlexibleCategory category) {
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.category = category;
    }

    // Getters/setters used by JPA and the rest of the app to read/write fields.
    public Long getId() {
        return id;
    }

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

    public FlexibleCategory getCategory() {
        return category;
    }

    public void setCategory(FlexibleCategory category) {
        this.category = category;
    }
}
