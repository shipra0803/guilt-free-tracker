package com.guiltfree.tracker.controller;

import com.guiltfree.tracker.dto.ExpenseRequest;
import com.guiltfree.tracker.dto.ExpenseResponse;
import com.guiltfree.tracker.model.Expense;
import com.guiltfree.tracker.model.FlexibleCategory;
import com.guiltfree.tracker.repository.ExpenseRepository;
import com.guiltfree.tracker.repository.FlexibleCategoryRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.stream.Collectors;

// Logs, lists, and deletes individual expenses recorded against a flexible category.
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseRepository expenseRepository;
    private final FlexibleCategoryRepository flexibleCategoryRepository;

    // Injects the repositories needed to read/write expenses and validate categories.
    public ExpenseController(ExpenseRepository expenseRepository, FlexibleCategoryRepository flexibleCategoryRepository) {
        this.expenseRepository = expenseRepository;
        this.flexibleCategoryRepository = flexibleCategoryRepository;
    }

    // Returns all expenses, newest first.
    @GetMapping
    public List<ExpenseResponse> getExpenses() {
        return expenseRepository.findAllByOrderByDateDescIdDesc().stream()
                .map(ExpenseResponse::new)
                .collect(Collectors.toList());
    }

    // Logs a new expense; defaults the date to today if none was sent.
    @PostMapping
    public ExpenseResponse addExpense(@Valid @RequestBody ExpenseRequest request) {
        FlexibleCategory category = flexibleCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NoSuchElementException("No flexible category with id " + request.getCategoryId()));
        LocalDate date = request.getDate() != null ? request.getDate() : LocalDate.now();
        Expense expense = new Expense(request.getAmount(), request.getDescription(), date, category);
        return new ExpenseResponse(expenseRepository.save(expense));
    }

    // Deletes an expense; returns 204 so the frontend's fetch wrapper handles it correctly.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        expenseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
