package com.guiltfree.tracker.controller;

import com.guiltfree.tracker.dto.SummaryResponse;
import com.guiltfree.tracker.service.CategoryBudgetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Serves the consolidated summary payload (net income, fixed total, flexible breakdown, extra
// savings) that the Home and Settings pages need in one call.
@RestController
@RequestMapping("/api/summary")
public class SummaryController {

    private final CategoryBudgetService categoryBudgetService;

    // Injects the service that computes the summary numbers.
    public SummaryController(CategoryBudgetService categoryBudgetService) {
        this.categoryBudgetService = categoryBudgetService;
    }

    // Returns the full summary payload, computed fresh on every call.
    @GetMapping
    public SummaryResponse getSummary() {
        return categoryBudgetService.getSummary();
    }
}
