package com.gympabon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the GymPabon backend. Starts the embedded web server that
 * exposes the REST API used by the frontend.
 */
@SpringBootApplication
public class GymPabonApplication {

    public static void main(String[] args) {
        SpringApplication.run(GymPabonApplication.class, args);
    }
}
