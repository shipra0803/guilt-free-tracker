package com.guiltfree.tracker.model;

import jakarta.persistence.*;

// The app's one login account, created on first run. Maps to "app_users" ("user" is reserved in H2).
@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    // BCrypt hash - the plain password is never stored.
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // No-arg constructor required by JPA.
    protected AppUser() {
    }

    public AppUser(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
