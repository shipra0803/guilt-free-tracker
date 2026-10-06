package com.guiltfree.tracker.controller;

import com.guiltfree.tracker.dto.IncomeRequest;
import com.guiltfree.tracker.dto.TaxBreakdown;
import com.guiltfree.tracker.model.IncomeProfile;
import com.guiltfree.tracker.service.IncomeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

// Reads and saves the income profile, and exposes a live (non-persisting) tax-breakdown preview.
@RestController
@RequestMapping("/api/income")
public class IncomeController {

    private final IncomeService incomeService;

    // Injects the service that holds the actual persistence and tax logic.
    public IncomeController(IncomeService incomeService) {
        this.incomeService = incomeService;
    }

    // Returns the saved income profile.
    @GetMapping
    public IncomeProfile getIncome() {
        return incomeService.getCurrentProfile();
    }

    // Live tax preview - computes a breakdown without saving anything.
    @PostMapping("/estimate")
    public TaxBreakdown estimate(@Valid @RequestBody IncomeRequest request) {
        return incomeService.estimate(request.getYearlySalary(), request.getStateTaxRatePercent(),
                request.getPayFrequency(), request.getAnchorPayDate());
    }

    // Saves the income profile and returns the resulting tax breakdown.
    @PutMapping
    public TaxBreakdown saveIncome(@Valid @RequestBody IncomeRequest request) {
        return incomeService.saveProfile(request.getYearlySalary(), request.getStateTaxRatePercent(),
                request.getPayFrequency(), request.getAnchorPayDate());
    }
}
