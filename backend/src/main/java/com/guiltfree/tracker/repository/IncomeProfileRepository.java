package com.guiltfree.tracker.repository;

import com.guiltfree.tracker.model.IncomeProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Data access for the single income profile row.
public interface IncomeProfileRepository extends JpaRepository<IncomeProfile, Long> {

    // The one saved profile, if any.
    default Optional<IncomeProfile> current() {
        return findAll().stream().findFirst();
    }
}
