package com.guiltfree.tracker.service;

import com.guiltfree.tracker.model.FlexibleCategory;
import com.guiltfree.tracker.repository.FlexibleCategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

// Persistence layer for flexible categories - CRUD plus a guard against deleting categories in use.
@Service
public class FlexibleCategoryService {

    private final FlexibleCategoryRepository flexibleCategoryRepository;
    private final CategoryBudgetService categoryBudgetService;

    // Injects the repository and the service used to check for existing expenses.
    public FlexibleCategoryService(FlexibleCategoryRepository flexibleCategoryRepository,
                                    CategoryBudgetService categoryBudgetService) {
        this.flexibleCategoryRepository = flexibleCategoryRepository;
        this.categoryBudgetService = categoryBudgetService;
    }

    // Returns all flexible categories.
    public List<FlexibleCategory> list() {
        return flexibleCategoryRepository.findAll();
    }

    // Saves a new flexible category.
    public FlexibleCategory add(String name, BigDecimal monthlyBudget) {
        return flexibleCategoryRepository.save(new FlexibleCategory(name, monthlyBudget));
    }

    // Updates an existing category's fields.
    public FlexibleCategory update(Long id, String name, BigDecimal monthlyBudget) {
        FlexibleCategory category = flexibleCategoryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No flexible category with id " + id));
        category.setName(name);
        category.setMonthlyBudget(monthlyBudget);
        return flexibleCategoryRepository.save(category);
    }

    // Deletes a category, refusing if any expense still references it.
    public void delete(Long id) {
        FlexibleCategory category = flexibleCategoryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No flexible category with id " + id));
        if (categoryBudgetService.categoryHasExpenses(category)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Can't delete \"" + category.getName() + "\" - it still has expenses logged against it. "
                            + "Move or delete those first.");
        }
        flexibleCategoryRepository.deleteById(id);
    }
}
