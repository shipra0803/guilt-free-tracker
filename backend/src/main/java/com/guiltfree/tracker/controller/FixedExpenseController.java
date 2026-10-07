package com.guiltfree.tracker.controller;

import com.guiltfree.tracker.dto.FixedExpenseRequest;
import com.guiltfree.tracker.model.FixedExpense;
import com.guiltfree.tracker.repository.FixedExpenseRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// CRUD for fixed monthly expenses (rent, subscriptions, etc.).
@RestController
@RequestMapping("/api/fixed-expenses")
public class FixedExpenseController {

    private final FixedExpenseRepository fixedExpenseRepository;

    public FixedExpenseController(FixedExpenseRepository fixedExpenseRepository) {
        this.fixedExpenseRepository = fixedExpenseRepository;
    }

    @GetMapping
    public List<FixedExpense> list() {
        return fixedExpenseRepository.findAll();
    }

    @PostMapping
    public FixedExpense add(@Valid @RequestBody FixedExpenseRequest request) {
        return fixedExpenseRepository.save(new FixedExpense(request.name(), request.monthlyAmount()));
    }

    @PutMapping("/{id}")
    public FixedExpense update(@PathVariable Long id, @Valid @RequestBody FixedExpenseRequest request) {
        FixedExpense expense = fixedExpenseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No fixed expense with id " + id));
        expense.setName(request.name());
        expense.setMonthlyAmount(request.monthlyAmount());
        return fixedExpenseRepository.save(expense);
    }

    // Returns 204 so the frontend's fetch wrapper handles it correctly.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fixedExpenseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
