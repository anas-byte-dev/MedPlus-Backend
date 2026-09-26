package com.medpulse.service.strategy;

import com.medpulse.model.MedicalDepartment;
import com.medpulse.model.PatientTriageCase;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Concrete Strategy: Evaluates patient risk based on chief complaint keyword indicators
 * and clinical red flags (STEMI, Acute Stroke, Sepsis, Severe Dyspnea, Anaphylaxis).
 */
@Component("symptomSeverityStrategy")
public class SymptomSeverityStrategy implements TriageRiskScoringStrategy {

    private static final List<String> RESUSCITATION_KEYWORDS = Arrays.asList(
            "cardiac arrest", "unresponsive", "apnea", "respiratory arrest", "severe anaphylaxis",
            "massive hemorrhage", "intubated", "cyanosis", "pulseless"
    );

    private static final List<String> EMERGENT_KEYWORDS = Arrays.asList(
            "chest pain", "crushing", "stemi", "facial droop", "slurred speech", "hemiparesis",
            "stroke", "altered mental status", "severe dyspnea", "stridor", "seizure", "syncope"
    );

    private static final List<String> URGENT_KEYWORDS = Arrays.asList(
            "shortness of breath", "wheezing", "asthma exacerbation", "severe pain", "high fever",
            "abdominal pain", "rebound tenderness", "hematuria", "diaphoresis", "vomiting blood"
    );

    @Override
    public int calculateRiskScore(PatientTriageCase patientCase, MedicalDepartment department) {
        if (patientCase == null || patientCase.getChiefComplaint() == null) {
            return 20;
        }

        String complaint = patientCase.getChiefComplaint().toLowerCase();
        int score = 20;

        // Level 1: Immediate Resuscitation Flags
        for (String kw : RESUSCITATION_KEYWORDS) {
            if (complaint.contains(kw)) {
                return 98; // Immediate critical resuscitation priority
            }
        }

        // Level 2: Emergent Flags
        for (String kw : EMERGENT_KEYWORDS) {
            if (complaint.contains(kw)) {
                score += 45;
                break;
            }
        }

        // Level 3: Urgent Flags
        for (String kw : URGENT_KEYWORDS) {
            if (complaint.contains(kw)) {
                score += 25;
                break;
            }
        }

        // Check if patient's chief complaint matches department protocols
        if (department != null && department.getClinicalProtocols() != null) {
            for (String protocol : department.getClinicalProtocols()) {
                if (complaint.contains(protocol.toLowerCase())) {
                    score += 10;
                    break;
                }
            }
        }

        return Math.min(100, Math.max(10, score));
    }

    @Override
    public String getStrategyName() {
        return "SymptomSeverityStrategy (Chief Complaint Clinical Red Flags)";
    }
}
