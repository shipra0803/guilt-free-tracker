package com.guiltfree.tracker.repository;

import com.guiltfree.tracker.model.FlexibleCategory;
import org.springframework.data.jpa.repository.JpaRepository;

// Data access for flexible categories - no custom queries needed beyond standard CRUD.
public interface FlexibleCategoryRepository extends JpaRepository<FlexibleCategory, Long> {
}
