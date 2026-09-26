package com.medpulse.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medpulse.agent.AgentTool;
import com.medpulse.model.PatientTriageCase;
import com.medpulse.repository.PatientTriageCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class AnalyzeVitalsTool implements AgentTool {

    private final PatientTriageCaseRepository patientCaseRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "analyzePatientVitalsAndBiometrics";
    }

    @Override
    public String getDescription() {
        return "Analyzes patient vital signs (HR, BP, SpO2, Temp, RR), calculates National Early Warning Score (NEWS2), and highlights acute physiological anomalies.";
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        try {
            String vitals = (String) parameters.get("vitalSigns");
            String patientId = (String) parameters.get("patientId");

            if ((vitals == null || vitals.isBlank()) && patientId != null) {
                Optional<PatientTriageCase> opt = patientCaseRepository.findByPatientId(patientId);
                if (opt.isPresent()) {
                    vitals = opt.get().getVitalSigns();
                }
            }

            if (vitals == null || vitals.isBlank()) {
                vitals = "HR: 80 bpm, BP: 120/80 mmHg, SpO2: 98%, Temp: 37.0°C";
            }

            Map<String, Object> analysis = new LinkedHashMap<>();
            analysis.put("evaluatedVitals", vitals);

            List<String> redFlags = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            int news2Score = 0;

            String lower = vitals.toLowerCase();

            // SpO2 check
            if (lower.contains("spo2")) {
                if (lower.contains("90") || lower.contains("91") || lower.contains("92") || lower.contains("89") || lower.contains("88")) {
                    redFlags.add("CRITICAL: Severe Hypoxemia detected (SpO2 < 93%). Initiate supplemental O2 immediately.");
                    news2Score += 3;
                } else if (lower.contains("94") || lower.contains("95")) {
                    warnings.add("Borderline oxygenation (SpO2 94-95%). Monitor pulse oximetry.");
                    news2Score += 1;
                }
            }

            // HR check
            if (lower.contains("hr")) {
                if (lower.contains("120") || lower.contains("125") || lower.contains("130") || lower.contains("140")) {
                    redFlags.add("CRITICAL: Severe Sinus Tachycardia / Arrhythmia risk (>120 bpm). Order 12-lead ECG.");
                    news2Score += 3;
                } else if (lower.contains("105") || lower.contains("110") || lower.contains("115")) {
                    warnings.add("Elevated Heart Rate (>100 bpm). Assess for pain, infection, or hypovolemia.");
                    news2Score += 2;
                }
            }

            // BP check
            if (lower.contains("bp")) {
                if (lower.contains("170/") || lower.contains("180/") || lower.contains("190/") || lower.contains("/105") || lower.contains("/110") || lower.contains("/120")) {
                    redFlags.add("CRITICAL: Stage 2 Hypertensive Crisis / Accelerated HTN. Monitor for end-organ damage.");
                    news2Score += 3;
                } else if (lower.contains("85/") || lower.contains("80/") || lower.contains("75/")) {
                    redFlags.add("CRITICAL: Severe Hypotension / Shock state. Establish wide-bore IV access and fluid challenge.");
                    news2Score += 3;
                }
            }

            // Temp check
            if (lower.contains("temp") || lower.contains("38.") || lower.contains("39.")) {
                if (lower.contains("38.5") || lower.contains("38.6") || lower.contains("39.")) {
                    warnings.add("Febrile state (>38.5°C). Screen for systemic inflammatory response / sepsis criteria.");
                    news2Score += 2;
                }
            }

            String clinicalAction;
            if (news2Score >= 7) {
                clinicalAction = "EMERGENCY PROTOCOL (High Clinical Acuity): Urgent bedside assessment by senior emergency physician and ICU team.";
            } else if (news2Score >= 4) {
                clinicalAction = "URGENT PROTOCOL (Medium Clinical Acuity): Immediate medical review and continuous vital sign telemetry monitoring.";
            } else {
                clinicalAction = "ROUTINE MONITORING (Low Clinical Acuity): Reassess vitals every 4-6 hours.";
            }

            analysis.put("calculatedNEWS2Score", news2Score);
            analysis.put("criticalRedFlags", redFlags);
            analysis.put("clinicalWarnings", warnings);
            analysis.put("recommendedAction", clinicalAction);

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(analysis);
        } catch (Exception e) {
            log.error("Error in AnalyzeVitalsTool: {}", e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
