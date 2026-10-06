package com.guiltfree.tracker.service;

import com.guiltfree.tracker.model.FixedExpense;
import com.guiltfree.tracker.repository.FixedExpenseRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

// Persistence layer for fixed monthly expenses - plain CRUD, no computation.
@Service
public class FixedExpenseService {

    private final FixedExpenseRepository fixedExpenseRepository;

    // Injects the repository.
    public FixedExpenseService(FixedExpenseRepository fixedExpenseRepository) {
        this.fixedExpenseRepository = fixedExpenseRepository;
    }

    // Returns all fixed expenses.
    public List<FixedExpense> list() {
        return fixedExpenseRepository.findAll();
    }

    // Saves a new fixed expense.
    public FixedExpense add(String name, BigDecimal monthlyAmount) {
        return fixedExpenseRepository.save(new FixedExpense(name, monthlyAmount));
    }

    // Updates an existing fixed expense's fields.
    public FixedExpense update(Long id, String name, BigDecimal monthlyAmount) {
        FixedExpense expense = fixedExpenseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No fixed expense with id " + id));
        expense.setName(name);
        expense.setMonthlyAmount(monthlyAmount);
        return fixedExpenseRepository.save(expense);
    }

    // Deletes a fixed expense.
    public void delete(Long id) {
        fixedExpenseRepository.deleteById(id);
    }
}
