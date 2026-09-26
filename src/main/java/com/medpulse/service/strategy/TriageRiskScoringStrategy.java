package com.medpulse.service.strategy;

import com.medpulse.model.MedicalDepartment;
import com.medpulse.model.PatientTriageCase;

/**
 * Strategy Pattern Interface for Clinical Triage Risk & Acuity Scoring.
 * Computes an Emergency Severity Index (ESI) compatible triage risk score (0-100).
 */
public interface TriageRiskScoringStrategy {
    int calculateRiskScore(PatientTriageCase patientCase, MedicalDepartment department);
    String getStrategyName();
}
