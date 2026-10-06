package com.guiltfree.tracker.service;

import com.guiltfree.tracker.dto.TaxBreakdown;
import com.guiltfree.tracker.model.IncomeProfile;
import com.guiltfree.tracker.model.PayFrequency;
import com.guiltfree.tracker.repository.IncomeProfileRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

// Manages the single ongoing income profile settings record.
@Service
public class IncomeService {

    private final IncomeProfileRepository incomeProfileRepository;
    private final TaxCalculationService taxCalculationService;

    // Injects the repository and the tax calculation service.
    public IncomeService(IncomeProfileRepository incomeProfileRepository,
                          TaxCalculationService taxCalculationService) {
        this.incomeProfileRepository = incomeProfileRepository;
        this.taxCalculationService = taxCalculationService;
    }

    // Returns the saved profile, creating a zeroed placeholder if none exists yet.
    public IncomeProfile getCurrentProfile() {
        return incomeProfileRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> incomeProfileRepository.save(
                        new IncomeProfile(BigDecimal.ZERO, BigDecimal.ZERO, PayFrequency.MONTHLY, null)));
    }

    // Live preview - computes a breakdown without persisting anything.
    public TaxBreakdown estimate(BigDecimal yearlySalary, BigDecimal stateTaxRatePercent,
                                  PayFrequency payFrequency, LocalDate anchorPayDate) {
        return taxCalculationService.estimate(yearlySalary, stateTaxRatePercent, payFrequency, anchorPayDate);
    }

    // Persists the income profile and returns the resulting tax breakdown.
    public TaxBreakdown saveProfile(BigDecimal yearlySalary, BigDecimal stateTaxRatePercent,
                                     PayFrequency payFrequency, LocalDate anchorPayDate) {
        IncomeProfile profile = getCurrentProfile();
        profile.setYearlySalary(yearlySalary);
        profile.setStateTaxRatePercent(stateTaxRatePercent);
        profile.setPayFrequency(payFrequency);
        profile.setAnchorPayDate(anchorPayDate);
        incomeProfileRepository.save(profile);

        return taxCalculationService.estimate(yearlySalary, stateTaxRatePercent, payFrequency, anchorPayDate);
    }
}
