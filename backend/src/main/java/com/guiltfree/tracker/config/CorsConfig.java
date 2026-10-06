package com.guiltfree.tracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Allows the frontend (running on a different origin) to call the /api/** endpoints.
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // Comma-separated list of allowed origins, read from application config.
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    // Registers CORS rules for every /api endpoint.
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
