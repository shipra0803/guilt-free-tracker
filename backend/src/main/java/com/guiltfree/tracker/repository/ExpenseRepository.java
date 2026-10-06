package com.guiltfree.tracker.repository;

import com.guiltfree.tracker.model.Expense;
import com.guiltfree.tracker.model.FlexibleCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

// Data access for expenses - adds custom finders on top of the standard JpaRepository CRUD.
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // All expenses in a date range, oldest first - used for history totals.
    List<Expense> findByDateBetweenOrderByDateAsc(LocalDate start, LocalDate end);

    // Expenses in one category within a date range - powers "spent this month" per category.
    List<Expense> findByCategoryAndDateBetween(FlexibleCategory category, LocalDate start, LocalDate end);

    // Whether any expense still references this category - used to block deletion.
    boolean existsByCategory(FlexibleCategory category);

    // Most recent expenses first - powers the dashboard's recent-expenses list.
    List<Expense> findAllByOrderByDateDescIdDesc();
}
