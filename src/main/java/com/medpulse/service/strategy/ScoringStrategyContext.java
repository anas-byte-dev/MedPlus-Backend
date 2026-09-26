package com.medpulse.service.strategy;

import com.medpulse.model.MedicalDepartment;
import com.medpulse.model.PatientTriageCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Context class for Strategy Pattern.
 * Orchestrates clinical scoring strategies and maps numerical scores
 * into standardized Emergency Severity Index (ESI) Acuity Levels.
 */
@Component
@Slf4j
public class ScoringStrategyContext {

    private final Map<String, TriageRiskScoringStrategy> strategyMap;
    private final TriageRiskScoringStrategy defaultStrategy;

    public ScoringStrategyContext(
            Map<String, TriageRiskScoringStrategy> strategyMap,
            @Qualifier("vitalSignsAcuityStrategy") TriageRiskScoringStrategy defaultStrategy) {
        this.strategyMap = strategyMap;
        this.defaultStrategy = defaultStrategy;
    }

    public int executeScoring(String strategyKey, PatientTriageCase patientCase, MedicalDepartment department) {
        TriageRiskScoringStrategy strategy = strategyMap.getOrDefault(strategyKey, defaultStrategy);
        int score = strategy.calculateRiskScore(patientCase, department);
        log.info("Calculated triage risk score: {} using strategy: {}", score, strategy.getStrategyName());
        return score;
    }

    /**
     * Comprehensive blended scoring: 55% Vitals Acuity + 45% Symptom Severity
     */
    public int calculateBlendedRiskScore(PatientTriageCase patientCase, MedicalDepartment department) {
        TriageRiskScoringStrategy vitalsStrategy = strategyMap.get("vitalSignsAcuityStrategy");
        TriageRiskScoringStrategy symptomsStrategy = strategyMap.get("symptomSeverityStrategy");

        int vitalsScore = (vitalsStrategy != null)
                ? vitalsStrategy.calculateRiskScore(patientCase, department)
                : 20;

        int symptomsScore = (symptomsStrategy != null)
                ? symptomsStrategy.calculateRiskScore(patientCase, department)
                : 20;

        int blended = (int) Math.round((vitalsScore * 0.55) + (symptomsScore * 0.45));
        return Math.min(100, Math.max(5, blended));
    }

    /**
     * Determines Emergency Severity Index (ESI) Acuity Level based on score
     */
    public static String determineAcuityLevel(int score) {
        if (score >= 85) {
            return "Level 1: Resuscitation (Critical)";
        } else if (score >= 70) {
            return "Level 2: Emergent (Immediate)";
        } else if (score >= 50) {
            return "Level 3: Urgent";
        } else if (score >= 30) {
            return "Level 4: Less Urgent";
        } else {
            return "Level 5: Non-Urgent";
        }
    }
}
