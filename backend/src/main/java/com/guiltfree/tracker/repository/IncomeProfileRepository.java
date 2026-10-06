package com.guiltfree.tracker.repository;

import com.guiltfree.tracker.model.IncomeProfile;
import org.springframework.data.jpa.repository.JpaRepository;

// Data access for the single income profile row - standard CRUD is all that's needed.
public interface IncomeProfileRepository extends JpaRepository<IncomeProfile, Long> {
}
