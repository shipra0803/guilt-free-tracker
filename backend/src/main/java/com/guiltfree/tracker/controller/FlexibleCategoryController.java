package com.guiltfree.tracker.controller;

import com.guiltfree.tracker.dto.FlexibleCategoryRequest;
import com.guiltfree.tracker.model.FlexibleCategory;
import com.guiltfree.tracker.service.FlexibleCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// CRUD for flexible spending categories and their monthly budgets - delegates to FlexibleCategoryService.
@RestController
@RequestMapping("/api/flexible-categories")
public class FlexibleCategoryController {

    private final FlexibleCategoryService flexibleCategoryService;

    // Injects the service that holds the actual persistence logic.
    public FlexibleCategoryController(FlexibleCategoryService flexibleCategoryService) {
        this.flexibleCategoryService = flexibleCategoryService;
    }

    // Returns all flexible categories.
    @GetMapping
    public List<FlexibleCategory> list() {
        return flexibleCategoryService.list();
    }

    // Adds a new flexible category.
    @PostMapping
    public FlexibleCategory add(@Valid @RequestBody FlexibleCategoryRequest request) {
        return flexibleCategoryService.add(request.getName(), request.getMonthlyBudget());
    }

    // Updates an existing flexible category.
    @PutMapping("/{id}")
    public FlexibleCategory update(@PathVariable Long id, @Valid @RequestBody FlexibleCategoryRequest request) {
        return flexibleCategoryService.update(id, request.getName(), request.getMonthlyBudget());
    }

    // Deletes a category (blocked if it still has expenses); returns 204 on success.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        flexibleCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
