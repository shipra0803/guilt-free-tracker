package com.guiltfree.tracker.controller;

import com.guiltfree.tracker.dto.SignupRequest;
import com.guiltfree.tracker.model.AppUser;
import com.guiltfree.tracker.repository.AppUserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

// First-run sign-up: creates the app's single account, then closes. Open to everyone (no login),
// which is safe because it only works while no account exists yet.
@RestController
@RequestMapping("/api/signup")
public class SignupController {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public SignupController(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Whether sign-up is still open, so the frontend knows which form to show.
    @GetMapping
    public Map<String, Boolean> status() {
        return Map.of("open", appUserRepository.count() == 0);
    }

    // synchronized so two first-run sign-ups racing each other can't both get through.
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204, like the deletes, so the frontend skips parsing a body
    public synchronized void signUp(@Valid @RequestBody SignupRequest request) {
        if (appUserRepository.count() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account already exists. Log in instead.");
        }
        appUserRepository.save(new AppUser(request.username().trim(), passwordEncoder.encode(request.password())));
    }
}
