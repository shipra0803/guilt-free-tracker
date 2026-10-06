package com.guiltfree.tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Spring Boot entry point - boots the whole backend application.
@SpringBootApplication
public class GuiltFreeTrackerApplication {

    // Starts the embedded server and the Spring application context.
    public static void main(String[] args) {
        SpringApplication.run(GuiltFreeTrackerApplication.class, args);
    }
}
