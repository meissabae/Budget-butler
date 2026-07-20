package com.budgetbutler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * This is the "main" file. Running this class starts the whole backend server.
 * Spring Boot scans this package (and sub-packages) automatically to find
 * our controllers, services, and repositories - we don't need to register them by hand.
 */
@SpringBootApplication
public class BudgetButlerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BudgetButlerApplication.class, args);
        System.out.println("Budget Butler backend is running on http://localhost:8080");
    }
}
