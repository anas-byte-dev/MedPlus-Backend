package com.medpulse.service.strategy;

import com.medpulse.model.PatientTriageCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VitalSignsAcuityStrategyTest {

    private VitalSignsAcuityStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new VitalSignsAcuityStrategy();
    }

    @Test
    void shouldScoreHighRiskForSevereHypoxemiaAndTachycardia() {
        PatientTriageCase patientCase = PatientTriageCase.builder()
                .patientId("TEST-01")
                .patientName("Emergency Patient")
                .age(68)
                .vitalSigns("HR: 135 bpm, BP: 185/115 mmHg, SpO2: 89%, Temp: 38.6°C")
                .build();

        int score = strategy.calculateRiskScore(patientCase, null);

        // Expect very high risk score >= 80 (Hypoxemia + Tachycardia + Hypertensive Crisis + Fever)
        assertTrue(score >= 80, "Expected high risk score for severe vitals anomaly, got: " + score);
    }

    @Test
    void shouldScoreLowRiskForStableVitals() {
        PatientTriageCase patientCase = PatientTriageCase.builder()
                .patientId("TEST-02")
                .patientName("Stable Patient")
                .age(28)
                .vitalSigns("HR: 72 bpm, BP: 120/80 mmHg, SpO2: 99%, Temp: 36.8°C")
                .build();

        int score = strategy.calculateRiskScore(patientCase, null);

        // Expect low baseline score <= 30
        assertTrue(score <= 30, "Expected low baseline score for normal vitals, got: " + score);
    }
}
