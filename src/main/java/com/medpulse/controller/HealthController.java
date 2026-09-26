package com.medpulse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;

    @GetMapping({"/", "/api/health"})
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "MedPulse AI Backend");
        response.put("version", "2.5.0-CLINICAL");
        response.put("timestamp", System.currentTimeMillis());

        try (Connection conn = dataSource.getConnection()) {
            response.put("database", "CONNECTED");
            response.put("dbUser", conn.getMetaData().getUserName());
            response.put("dbUrl", conn.getMetaData().getURL());
        } catch (Exception e) {
            response.put("database", "DISCONNECTED");
            response.put("dbError", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}
