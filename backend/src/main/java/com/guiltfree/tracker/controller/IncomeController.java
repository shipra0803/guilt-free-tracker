package com.guiltfree.tracker.controller;

import com.guiltfree.tracker.dto.IncomeRequest;
import com.guiltfree.tracker.dto.TaxBreakdown;
import com.guiltfree.tracker.model.IncomeProfile;
import com.guiltfree.tracker.model.PayFrequency;
import com.guiltfree.tracker.repository.IncomeProfileRepository;
import com.guiltfree.tracker.service.TaxCalculationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

// Reads and saves the single income profile, and exposes a live (non-persisting) tax-breakdown preview.
@RestController
@RequestMapping("/api/income")
public class IncomeController {

    private final IncomeProfileRepository incomeProfileRepository;
    private final TaxCalculationService taxCalculationService;

    public IncomeController(IncomeProfileRepository incomeProfileRepository,
                            TaxCalculationService taxCalculationService) {
        this.incomeProfileRepository = incomeProfileRepository;
        this.taxCalculationService = taxCalculationService;
    }

    // Returns the saved profile, creating a zeroed placeholder if none exists yet.
    @GetMapping
    public IncomeProfile getIncome() {
        return incomeProfileRepository.current().orElseGet(() -> incomeProfileRepository.save(
                new IncomeProfile(BigDecimal.ZERO, BigDecimal.ZERO, PayFrequency.MONTHLY, null)));
    }

    // Live tax preview - computes a breakdown without saving anything.
    @PostMapping("/estimate")
    public TaxBreakdown estimate(@Valid @RequestBody IncomeRequest request) {
        return taxCalculationService.estimate(request.yearlySalary(), request.stateTaxRatePercent(),
                request.payFrequency(), request.anchorPayDate());
    }

    // Saves the income profile and returns the resulting tax breakdown.
    @PutMapping
    public TaxBreakdown saveIncome(@Valid @RequestBody IncomeRequest request) {
        IncomeProfile profile = getIncome();
        profile.setYearlySalary(request.yearlySalary());
        profile.setStateTaxRatePercent(request.stateTaxRatePercent());
        profile.setPayFrequency(request.payFrequency());
        profile.setAnchorPayDate(request.anchorPayDate());
        incomeProfileRepository.save(profile);
        return estimate(request);
    }
}
