package com.gympabon.controller;

import com.gympabon.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quick check endpoint: confirms that the API is running and that it can reach
 * MySQL. Open http://localhost:8080/api/health in the browser.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DatabaseConnection databaseConnection;

    public HealthController(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    @GetMapping
    public Map<String, String> health() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("api", "ok");
        try (Connection connection = databaseConnection.getConnection()) {
            result.put("database", connection.isValid(2) ? "connected" : "not connected");
        } catch (SQLException | RuntimeException e) {
            result.put("database", "error: " + e.getMessage());
        }
        return result;
    }
}
