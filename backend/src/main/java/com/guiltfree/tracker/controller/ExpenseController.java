package com.guiltfree.tracker.controller;

import com.guiltfree.tracker.dto.ExpenseRequest;
import com.guiltfree.tracker.dto.ExpenseResponse;
import com.guiltfree.tracker.model.Expense;
import com.guiltfree.tracker.model.FlexibleCategory;
import com.guiltfree.tracker.repository.ExpenseRepository;
import com.guiltfree.tracker.repository.FlexibleCategoryRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

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
                .map(ExpenseResponse::from)
                .toList();
    }

    // Logs a new expense; defaults the date to today if none was sent.
    @PostMapping
    public ExpenseResponse addExpense(@Valid @RequestBody ExpenseRequest request) {
        FlexibleCategory category = flexibleCategoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No flexible category with id " + request.categoryId()));
        LocalDate date = request.date() != null ? request.date() : LocalDate.now();
        Expense expense = new Expense(request.amount(), request.description(), date, category);
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    // Deletes an expense; returns 204 so the frontend's fetch wrapper handles it correctly.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        expenseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
