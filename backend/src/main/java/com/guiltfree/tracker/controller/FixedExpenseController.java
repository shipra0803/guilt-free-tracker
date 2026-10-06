package com.guiltfree.tracker.controller;

import com.guiltfree.tracker.dto.FixedExpenseRequest;
import com.guiltfree.tracker.model.FixedExpense;
import com.guiltfree.tracker.service.FixedExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// CRUD for fixed monthly expenses (rent, subscriptions, etc.) - delegates to FixedExpenseService.
@RestController
@RequestMapping("/api/fixed-expenses")
public class FixedExpenseController {

    private final FixedExpenseService fixedExpenseService;

    // Injects the service that holds the actual persistence logic.
    public FixedExpenseController(FixedExpenseService fixedExpenseService) {
        this.fixedExpenseService = fixedExpenseService;
    }

    // Returns all fixed expenses.
    @GetMapping
    public List<FixedExpense> list() {
        return fixedExpenseService.list();
    }

    // Adds a new fixed expense.
    @PostMapping
    public FixedExpense add(@Valid @RequestBody FixedExpenseRequest request) {
        return fixedExpenseService.add(request.getName(), request.getMonthlyAmount());
    }

    // Updates an existing fixed expense.
    @PutMapping("/{id}")
    public FixedExpense update(@PathVariable Long id, @Valid @RequestBody FixedExpenseRequest request) {
        return fixedExpenseService.update(id, request.getName(), request.getMonthlyAmount());
    }

    // Deletes a fixed expense; returns 204 so the frontend's fetch wrapper handles it correctly.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fixedExpenseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
