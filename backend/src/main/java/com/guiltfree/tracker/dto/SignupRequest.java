package com.guiltfree.tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Request body for POST /api/signup. BCrypt only reads the first 72 bytes, hence the max.
public record SignupRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(min = 8, max = 72, message = "password must be 8-72 characters") String password) {
}
