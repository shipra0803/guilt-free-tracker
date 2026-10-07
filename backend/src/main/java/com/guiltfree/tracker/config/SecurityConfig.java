package com.guiltfree.tracker.config;

import com.guiltfree.tracker.repository.AppUserRepository;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

// Login: every request needs the username/password of the account created at sign-up,
// sent as HTTP Basic on each call. Only /api/signup is open, and it closes once an account exists.
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Let CORS preflight requests through, using corsConfigurationSource() below.
                .cors(Customizer.withDefaults())
                // No cookies or sessions: credentials travel with every request, so there's nothing to forge.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/signup").permitAll()
                        // Error responses (e.g. sign-up's 409) are forwarded internally to /error; let them through.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .anyRequest().authenticated())
                // Plain 401 without a WWW-Authenticate header, so the browser doesn't pop up its own
                // login dialog - the app shows its login page instead.
                .httpBasic(basic -> basic.authenticationEntryPoint((request, response, e) -> response.sendError(401)))
                .build();
    }

    // Lets the frontend (on a different origin) call /api/**. Origins come from app.cors.allowed-origins
    // (FRONTEND_ORIGIN in production), comma-separated.
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    // Passwords are stored as BCrypt hashes.
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Spring Security looks up the account by username and checks the password against its hash.
    @Bean
    UserDetailsService userDetailsService(AppUserRepository appUserRepository) {
        return username -> appUserRepository.findByUsername(username)
                .map(user -> User.withUsername(user.getUsername()).password(user.getPasswordHash()).build())
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}
