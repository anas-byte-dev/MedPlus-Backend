package com.medpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class MedpulseBackendApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(MedpulseBackendApplication.class, args);
    }

    /**
     * Reads .env file from working directory or project root and sets system properties
     * if the corresponding environment variable is not already defined.
     */
    private static void loadDotEnv() {
        File[] candidates = new File[] {
                new File(".env"),
                new File("backend/.env"),
                new File("../backend/.env")
        };

        for (File envFile : candidates) {
            if (envFile.exists() && envFile.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(envFile, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                            continue;
                        }
                        int eqIdx = line.indexOf('=');
                        String key = line.substring(0, eqIdx).trim();
                        String value = line.substring(eqIdx + 1).trim();
                        // Strip quotes if any
                        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                            value = value.substring(1, value.length() - 1);
                        } else if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
                            value = value.substring(1, value.length() - 1);
                        }
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, value);
                        }
                    }
                    System.out.println("[MedPulse] Successfully loaded environment from " + envFile.getAbsolutePath());
                    break;
                } catch (Exception e) {
                    System.err.println("[MedPulse] Warning: Failed to load .env: " + e.getMessage());
                }
            }
        }
    }
}

