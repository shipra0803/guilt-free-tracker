package com.guiltfree.tracker.controller;

import com.guiltfree.tracker.dto.FlexibleCategoryRequest;
import com.guiltfree.tracker.model.FlexibleCategory;
import com.guiltfree.tracker.repository.ExpenseRepository;
import com.guiltfree.tracker.repository.FlexibleCategoryRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// Create/update/delete for flexible spending categories. Listing goes through /api/summary.
@RestController
@RequestMapping("/api/flexible-categories")
public class FlexibleCategoryController {

    private final FlexibleCategoryRepository flexibleCategoryRepository;
    private final ExpenseRepository expenseRepository;

    public FlexibleCategoryController(FlexibleCategoryRepository flexibleCategoryRepository,
                                      ExpenseRepository expenseRepository) {
        this.flexibleCategoryRepository = flexibleCategoryRepository;
        this.expenseRepository = expenseRepository;
    }

    @PostMapping
    public FlexibleCategory add(@Valid @RequestBody FlexibleCategoryRequest request) {
        return flexibleCategoryRepository.save(new FlexibleCategory(request.name(), request.monthlyBudget()));
    }

    @PutMapping("/{id}")
    public FlexibleCategory update(@PathVariable Long id, @Valid @RequestBody FlexibleCategoryRequest request) {
        FlexibleCategory category = find(id);
        category.setName(request.name());
        category.setMonthlyBudget(request.monthlyBudget());
        return flexibleCategoryRepository.save(category);
    }

    // Deletes a category, refusing with 409 if any expense still references it.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        FlexibleCategory category = find(id);
        if (expenseRepository.existsByCategory(category)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Can't delete \"" + category.getName() + "\" - it still has expenses logged against it. "
                            + "Move or delete those first.");
        }
        flexibleCategoryRepository.delete(category);
        return ResponseEntity.noContent().build();
    }

    private FlexibleCategory find(Long id) {
        return flexibleCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No flexible category with id " + id));
    }
}
