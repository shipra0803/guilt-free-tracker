package com.guiltfree.tracker.repository;

import com.guiltfree.tracker.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Data access for the login account - looked up by username when checking a login.
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);
}
