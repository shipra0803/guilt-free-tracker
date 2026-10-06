package com.guiltfree.tracker.repository;

import com.guiltfree.tracker.model.FixedExpense;
import org.springframework.data.jpa.repository.JpaRepository;

// Data access for fixed expenses - no custom queries needed beyond standard CRUD.
public interface FixedExpenseRepository extends JpaRepository<FixedExpense, Long> {
}
