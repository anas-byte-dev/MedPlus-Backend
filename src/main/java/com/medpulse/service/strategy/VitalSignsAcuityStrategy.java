package com.medpulse.service.strategy;

import com.medpulse.model.MedicalDepartment;
import com.medpulse.model.PatientTriageCase;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Concrete Strategy: Evaluates patient risk based on physiological vitals & biometrics.
 * Implements clinical rules mirroring NEWS2 (National Early Warning Score).
 */
@Component("vitalSignsAcuityStrategy")
public class VitalSignsAcuityStrategy implements TriageRiskScoringStrategy {

    private static final Pattern HR_PATTERN = Pattern.compile("(?i)HR\\s*:\\s*(\\d+)");
    private static final Pattern SPO2_PATTERN = Pattern.compile("(?i)SpO2\\s*:\\s*(\\d+)");
    private static final Pattern BP_PATTERN = Pattern.compile("(?i)BP\\s*:\\s*(\\d+)/(\\d+)");
    private static final Pattern TEMP_PATTERN = Pattern.compile("(?i)Temp\\s*:\\s*([0-9.]+)");

    @Override
    public int calculateRiskScore(PatientTriageCase patientCase, MedicalDepartment department) {
        if (patientCase == null || patientCase.getVitalSigns() == null) {
            return 20; // Default baseline
        }

        int score = 15; // Baseline physiological risk
        String vitals = patientCase.getVitalSigns();

        // 1. Oxygen Saturation (SpO2) Check
        Matcher spo2Matcher = SPO2_PATTERN.matcher(vitals);
        if (spo2Matcher.find()) {
            try {
                int spo2 = Integer.parseInt(spo2Matcher.group(1));
                if (spo2 < 90) {
                    score += 35; // Severe hypoxemia / respiratory failure
                } else if (spo2 <= 93) {
                    score += 25; // Moderate hypoxia
                } else if (spo2 <= 95) {
                    score += 10;
                }
            } catch (NumberFormatException ignored) {}
        }

        // 2. Heart Rate (HR) Check
        Matcher hrMatcher = HR_PATTERN.matcher(vitals);
        if (hrMatcher.find()) {
            try {
                int hr = Integer.parseInt(hrMatcher.group(1));
                if (hr > 130 || hr < 40) {
                    score += 30; // Severe tachycardia or severe bradycardia
                } else if (hr > 110 || hr < 50) {
                    score += 18;
                } else if (hr > 100) {
                    score += 8;
                }
            } catch (NumberFormatException ignored) {}
        }

        // 3. Blood Pressure (BP) Check
        Matcher bpMatcher = BP_PATTERN.matcher(vitals);
        if (bpMatcher.find()) {
            try {
                int systolic = Integer.parseInt(bpMatcher.group(1));
                int diastolic = Integer.parseInt(bpMatcher.group(2));
                if (systolic >= 180 || diastolic >= 120) {
                    score += 30; // Hypertensive crisis
                } else if (systolic < 90) {
                    score += 35; // Cardiogenic or septic shock
                } else if (systolic >= 160 || diastolic >= 100) {
                    score += 15;
                }
            } catch (NumberFormatException ignored) {}
        }

        // 4. Body Temperature Check
        Matcher tempMatcher = TEMP_PATTERN.matcher(vitals);
        if (tempMatcher.find()) {
            try {
                double temp = Double.parseDouble(tempMatcher.group(1));
                if (temp >= 39.5 || temp <= 35.0) {
                    score += 20; // Hyperpyrexia / hypothermia
                } else if (temp >= 38.5) {
                    score += 10;
                }
            } catch (NumberFormatException ignored) {}
        }

        // 5. Age Vulnerability
        if (patientCase.getAge() != null) {
            if (patientCase.getAge() >= 75 || patientCase.getAge() <= 2) {
                score += 10;
            } else if (patientCase.getAge() >= 65) {
                score += 5;
            }
        }

        return Math.min(100, Math.max(5, score));
    }

    @Override
    public String getStrategyName() {
        return "VitalSignsAcuityStrategy (NEWS2 Physiological Biometrics)";
    }
}
